package com.cyberdrop.transfer.transfer

import android.content.Context
import android.net.Uri
import android.os.PowerManager
import android.provider.OpenableColumns
import android.util.Log
import com.cyberdrop.transfer.core.Fmt
import com.cyberdrop.transfer.core.Identity
import com.cyberdrop.transfer.core.Prefs
import com.cyberdrop.transfer.net.Signaling
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * هستهٔ انتقال: ترکیب WebRTC (کانال دادهٔ رمزنگاری‌شده، مستقیم بین دو گوشی)
 * با یک کانال signaling عمومی برای رد و بدل کردن SDP.
 *
 * ویژگی ادامه‌پذیری: گیرنده بایت‌های رسیده را در فایل *.part نگه می‌دارد؛
 * هنگام هر اتصال دوباره، آفست فعلی را اعلام می‌کند و ارسال از همان‌جا ادامه می‌یابد.
 */
class TransferEngine(private val app: Context) {

    companion object {
        private const val TAG = "Cyberdrop"
        private const val CHUNK = 64 * 1024
        private const val HIGH_WATER = 1024L * 1024L
        private const val MAX_ATTEMPTS = 8
    }

    val prefs = Prefs(app)
    val myId: String = Identity.get(app)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(EngineState())
    val state: StateFlow<EngineState> = _state.asStateFlow()

    private val dir: File by lazy {
        val base = app.getExternalFilesDir(null) ?: app.filesDir
        File(base, "Cyberdrop").apply { mkdirs() }
    }

    private var signaling: Signaling? = null
    private var listenJob: Job? = null
    private var sendJob: Job? = null
    private var recvJob: Job? = null
    private var factory: PeerConnectionFactory? = null
    private var wake: PowerManager.WakeLock? = null

    @Volatile
    private var cur: Session? = null

    @Volatile
    private var acceptSignal: CompletableDeferred<Boolean>? = null

    init {
        _state.update { it.copy(files = loadFiles()) }
    }

    // ------------------------------------------------------------------ عمومی

    /** اتصال به کانال signaling و آماده شدن برای دریافت. */
    fun start() {
        if (listenJob?.isActive == true) {
            scope.launch { announcePartials() }
            return
        }
        listenJob = scope.launch {
            ensureOnline(force = true)
            announcePartials()
        }
    }

    fun stop() {
        signaling?.close()
        signaling = null
        _state.update { it.copy(online = false, statusText = "آفلاین") }
    }

    fun retryOnline() {
        listenJob?.cancel()
        listenJob = scope.launch {
            ensureOnline(force = true)
            announcePartials()
        }
    }

    fun note(msg: String) {
        Log.i(TAG, msg)
        _state.update { it.copy(log = (listOf(msg) + it.log).take(40)) }
    }

    /** ارسال یک فایل. [tid] برای ادامهٔ انتقال قبلی استفاده می‌شود. */
    fun sendFile(uri: Uri, receiverId: String, tid: String? = null) {
        if (sendJob?.isActive == true) {
            note("یک ارسال دیگر در جریان است")
            return
        }
        val target = Identity.normalize(receiverId)
        if (target.isEmpty()) {
            note("شناسهٔ گیرنده نامعتبر است")
            return
        }
        if (target == myId) {
            note("نمی‌توانید برای خودتان بفرستید")
            return
        }
        val id = tid ?: newTid()
        sendJob = scope.launch { runSender(uri.toString(), target, id) }
    }

    fun cancel() {
        scope.launch {
            val s = cur
            try {
                if (s != null) sendText(s, JSONObject().put("t", "cancel"))
            } catch (_: Throwable) {
            }
            sendJob?.cancel()
            recvJob?.cancel()
            closeSession()
            acceptSignal?.complete(false)
            acceptSignal = null
            _state.update {
                it.copy(
                    outgoing = it.outgoing?.let { o -> o.copy(phase = Phase.CANCELLED) },
                    incoming = it.incoming?.let { o -> o.copy(phase = Phase.CANCELLED) },
                    request = null,
                )
            }
            prefs.outgoing = null
            releaseWake()
        }
    }

    fun acceptIncoming() {
        acceptSignal?.complete(true)
    }

    fun rejectIncoming() {
        acceptSignal?.complete(false)
    }

    fun dismissFinished() {
        _state.update {
            it.copy(
                outgoing = it.outgoing?.takeIf { o -> o.active },
                incoming = it.incoming?.takeIf { o -> o.active },
            )
        }
    }

    fun resendLast() {
        val raw = prefs.outgoing ?: return
        runCatching {
            val o = JSONObject(raw)
            sendFile(Uri.parse(o.getString("uri")), o.getString("peer"), o.getString("tid"))
        }
    }

    /** اطلاعات فایل انتخاب‌شده را برای نمایش در UI برمی‌گرداند. */
    fun describe(uri: Uri): FileMeta? = readMeta(uri.toString())

    fun refreshFiles() {
        _state.update { it.copy(files = loadFiles()) }
    }

    // --------------------------------------------------------------- سیگنالینگ

    private suspend fun ensureOnline(force: Boolean = false): Boolean {
        val existing = signaling
        if (!force && existing != null && existing.isConnected()) {
            _state.update { it.copy(online = true) }
            return true
        }
        _state.update { it.copy(connecting = true, statusText = "در حال اتصال…") }
        existing?.close()
        val s = Signaling(
            selfId = myId,
            onMessage = { msg -> onSignal(msg) },
            onStatus = { ok, text ->
                _state.update { st ->
                    st.copy(
                        online = ok,
                        broker = if (ok) text else st.broker,
                        statusText = if (ok) "متصل" else text,
                    )
                }
            },
        )
        signaling = s
        val ok = s.connect()
        _state.update { it.copy(connecting = false, online = ok) }
        return ok
    }

    private fun onSignal(msg: JSONObject) {
        val from = msg.optString("from")
        if (from.isEmpty() || from == myId) return
        when (msg.optString("t")) {
            "offer" -> scope.launch { handleOffer(from, msg.optString("tid"), msg.optString("sdp")) }
            "answer" -> {
                val sdp = msg.optString("sdp")
                val s = cur
                if (s != null && s.direction == Direction.SEND) {
                    if (sdp.isEmpty()) s.answer.completeExceptionally(IOException("busy")) else s.answer.complete(sdp)
                }
            }
            "busy" -> cur?.answer?.completeExceptionally(IOException("busy"))
            "resume" -> scope.launch { handleResume(from, msg.optString("tid")) }
            "cancel" -> {
                note("طرف مقابل انتقال را لغو کرد")
                sendJob?.cancel()
                recvJob?.cancel()
                closeSession()
            }
        }
    }

    private suspend fun handleResume(from: String, tid: String) {
        val raw = prefs.outgoing ?: return
        val o = runCatching { JSONObject(raw) }.getOrNull() ?: return
        if (o.optString("tid") != tid || o.optString("peer") != from) return
        if (sendJob?.isActive == true) return
        note("درخواست ادامهٔ انتقال از طرف گیرنده")
        sendFile(Uri.parse(o.optString("uri")), from, tid)
    }

    /** به گیرنده‌هایی که فایل نیمه‌کاره دارند اطلاع می‌دهیم که آماده ادامه‌ایم. */
    private suspend fun announcePartials() {
        val list = dir.listFiles { f -> f.name.endsWith(".meta") } ?: return
        if (list.isEmpty()) return
        withContext(Dispatchers.IO) {
            for (f in list) {
                val meta = runCatching { JSONObject(f.readText()) }.getOrNull() ?: continue
                val peer = meta.optString("peer")
                val tid = meta.optString("tid")
                if (peer.isNotEmpty() && tid.isNotEmpty()) {
                    signaling?.publish(peer, "resume", tid = tid)
                }
            }
        }
    }

    // -------------------------------------------------------------- ارسال فایل

    private suspend fun runSender(uri: String, target: String, tid: String) {
        val meta = readMeta(uri)
        if (meta == null) {
            setOut { base(tid, Direction.SEND, target, "فایل", 0).copy(phase = Phase.FAILED, message = "فایل قابل خواندن نیست") }
            return
        }
        if (meta.size > prefs.maxBytes) {
            setOut {
                base(tid, Direction.SEND, target, meta.name, meta.size)
                    .copy(phase = Phase.FAILED, message = "حجم فایل از سقف مجاز بیشتر است")
            }
            return
        }
        prefs.outgoing = JSONObject()
            .put("tid", tid).put("uri", uri).put("peer", target)
            .put("name", meta.name).put("size", meta.size).toString()
        setOut { base(tid, Direction.SEND, target, meta.name, meta.size).copy(phase = Phase.CONNECTING) }
        acquireWake()

        var attempt = 0
        var lastError: String? = null
        try {
            while (attempt < MAX_ATTEMPTS) {
                attempt++
                setOut { it.copy(phase = Phase.CONNECTING, attempt = attempt, message = null) }
                if (!ensureOnline(force = attempt > 1)) {
                    lastError = "اتصال به شبکه برقرار نشد"
                    setOut { it.copy(phase = Phase.INTERRUPTED, message = "اینترنت در دسترس نیست — تلاش دوباره") }
                    delay(2500L * attempt)
                    continue
                }
                try {
                    val r = senderAttempt(tid, target, meta, attempt)
                    when (r) {
                        Attempt.Done -> {
                            setOut { it.copy(phase = Phase.DONE, bytes = meta.size, speed = 0, message = "تحویل شد") }
                            prefs.outgoing = null
                            prefs.completedCount = prefs.completedCount + 1
                            prefs.sentBytes = prefs.sentBytes + meta.size
                            return
                        }
                        is Attempt.Interrupted -> {
                            setOut {
                                it.copy(
                                    phase = Phase.INTERRUPTED,
                                    bytes = r.offset,
                                    speed = 0,
                                    message = "قطع ارتباط — ادامه از ${Fmt.bytes(r.offset)}",
                                )
                            }
                            delay(1800L * attempt)
                        }
                    }
                } catch (e: RejectedException) {
                    setOut { it.copy(phase = Phase.FAILED, message = e.message ?: "رد شد") }
                    prefs.outgoing = null
                    return
                } catch (e: IOException) {
                    lastError = e.message
                    setOut {
                        it.copy(
                            phase = Phase.INTERRUPTED,
                            speed = 0,
                            message = "قطعی موقت — تلاش دوباره (${attempt}/${MAX_ATTEMPTS})",
                        )
                    }
                    delay(1500L * attempt)
                }
            }
            setOut { it.copy(phase = Phase.FAILED, message = lastError ?: "انتقال ناموفق بود") }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            setOut { it.copy(phase = Phase.FAILED, message = e.message ?: "خطای نامشخص") }
        } finally {
            closeSession()
            releaseWake()
        }
    }

    private suspend fun senderAttempt(tid: String, target: String, meta: FileMeta, attempt: Int): Attempt {
        val s = Session(tid, target, Direction.SEND)
        cur = s
        val pc = createPeer(s)
        val dc = pc.createDataChannel("cd", DataChannel.Init())
        attachChannel(s, dc)

        val offer = createSdp(pc, true)
        setLocal(pc, offer)
        waitIce(s, 9000)
        val payload = pc.localDescription?.description ?: offer.description
        if (signaling?.publish(target, "offer", sdp = payload, tid = tid) != true) {
            throw IOException("اتصال به شبکه برقرار نشد")
        }
        setOut { it.copy(phase = Phase.WAITING_PEER, message = "در انتظار پاسخ گیرنده…") }

        val answer = withTimeoutOrNull(75_000) { s.answer.await() } ?: throw IOException("پاسخی از گیرنده نرسید")
        setRemote(pc, SessionDescription(SessionDescription.Type.ANSWER, answer))

        val opened = withTimeoutOrNull(60_000) { s.open.await() }
        if (opened == null) throw IOException("کانال مستقیم بسته شد")

        sendText(
            s,
            JSONObject()
                .put("t", "meta")
                .put("tid", tid)
                .put("name", meta.name)
                .put("size", meta.size)
                .put("mime", meta.mime),
        )
        setOut { it.copy(phase = Phase.WAITING_ACCEPT, message = "در انتظار تأیید گیرنده…") }

        val reply = awaitType(s, 12 * 60_000, "accept", "reject")
            ?: throw IOException("گیرنده پاسخ نداد")
        if (reply.optString("t") == "reject") throw RejectedException("گیرنده دریافت را رد کرد")

        val offset = reply.optLong("offset", 0L).coerceIn(0L, meta.size)
        setOut {
            it.copy(
                phase = Phase.TRANSFERRING,
                bytes = offset,
                resumedFrom = offset,
                message = if (offset > 0) "ادامه از ${Fmt.bytes(offset)}" else null,
            )
        }
        val sha = pump(s, meta, offset)
        sendText(s, JSONObject().put("t", "end").put("size", meta.size).put("sha", sha ?: JSONObject.NULL))
        setOut { it.copy(phase = Phase.VERIFYING, message = "بررسی یکپارچگی…") }

        val done = awaitType(s, 180_000, "done", "error")
        if (done != null && done.optString("t") == "done") {
            return if (done.optBoolean("ok", true)) Attempt.Done else Attempt.Interrupted(s.sentBytes)
        }
        throw IOException("گیرنده پایان انتقال را تأیید نکرد")
    }

    /** حلقهٔ فرستادن بایت‌ها؛ در صورت موفقیت هش فایل (اگر از ابتدا فرستاده شده) را برمی‌گرداند. */
    private suspend fun pump(s: Session, meta: FileMeta, start: Long): String? {
        val dc = s.dc ?: throw IOException("کانال بسته است")
        val input = openInput(meta.uri) ?: throw IOException("فایل باز نشد")
        val digest = if (start == 0L) MessageDigest.getInstance("SHA-256") else null
        val buf = ByteArray(CHUNK)
        var sent = start
        var speedMark = System.currentTimeMillis()
        var speedBytes = 0L
        var speed = 0L
        var lastUi = 0L
        try {
            skipFully(input, start)
            while (true) {
                if (!s.alive) throw IOException("ارتباط قطع شد")
                val n = readChunk(input, buf)
                if (n <= 0) break
                awaitBuffer(s)
                if (!dc.send(DataChannel.Buffer(ByteBuffer.wrap(buf, 0, n), true))) {
                    throw IOException("ارسال ناموفق")
                }
                if (digest != null) digest.update(buf, 0, n)
                sent += n
                speedBytes += n
                val now = System.currentTimeMillis()
                if (now - speedMark >= 700) {
                    speed = speedBytes * 1000L / (now - speedMark).coerceAtLeast(1)
                    speedMark = now
                    speedBytes = 0
                }
                if (now - lastUi >= 180) {
                    lastUi = now
                    s.sentBytes = sent
                    val sp = speed
                    setOut { it.copy(bytes = sent, speed = sp, phase = Phase.TRANSFERRING) }
                }
            }
        } finally {
            runCatching { input.close() }
        }
        s.sentBytes = sent
        setOut { it.copy(bytes = sent, speed = 0) }
        return digest?.let { Fmt.hex(it.digest()) }
    }

    private suspend fun awaitBuffer(s: Session) {
        val dc = s.dc ?: throw IOException("کانال بسته است")
        var waited = 0
        while (dc.bufferedAmount() > HIGH_WATER) {
            if (!s.alive) throw IOException("ارتباط قطع شد")
            delay(4)
            waited += 4
            if (waited > 20_000) break
        }
    }

    // ------------------------------------------------------------ دریافت فایل

    private suspend fun handleOffer(from: String, tid: String, sdp: String) {
        if (tid.isEmpty() || sdp.isEmpty()) return
        val busy = (sendJob?.isActive == true) || (recvJob?.isActive == true) || cur?.alive == true
        if (busy) {
            signaling?.publish(from, "busy", tid = tid)
            return
        }
        val s = Session(tid, from, Direction.RECEIVE)
        cur = s
        recvJob = scope.launch {
            try {
                signaling?.publish(from, "ack", tid = tid)
                val pc = createPeer(s)
                setRemote(pc, SessionDescription(SessionDescription.Type.OFFER, sdp))
                val answer = createSdp(pc, false)
                setLocal(pc, answer)
                waitIce(s, 9000)
                val payload = pc.localDescription?.description ?: answer.description
                signaling?.publish(from, "answer", sdp = payload, tid = tid)

                val opened = withTimeoutOrNull(60_000) { s.open.await() }
                if (opened == null) {
                    note("کانال مستقیم برقرار نشد")
                    return@launch
                }
                val metaMsg = awaitType(s, 45_000, "meta") ?: return@launch
                val name = metaMsg.optString("name", "file")
                val size = metaMsg.optLong("size", 0L)
                if (size <= 0L) {
                    sendText(s, JSONObject().put("t", "reject"))
                    return@launch
                }
                if (size > prefs.maxBytes) {
                    sendText(s, JSONObject().put("t", "reject"))
                    note("گیرنده: حجم بیش از سقف مجاز")
                    return@launch
                }

                val part = partFile(tid)
                val already = if (part.exists()) part.length() else 0L
                val autoResume = already > 0L && already < size
                val accepted = if (autoResume || prefs.autoAccept) true else askUser(s, tid, from, name, size, already)
                if (!accepted) {
                    sendText(s, JSONObject().put("t", "reject"))
                    _state.update { it.copy(request = null) }
                    note("دریافت رد شد")
                    return@launch
                }
                _state.update { it.copy(request = null) }
                writePartialMeta(tid, name, size, from)
                sendText(s, JSONObject().put("t", "accept").put("offset", already))
                receiveLoop(s, tid, name, size)
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                note("خطای دریافت: ${e.message}")
                setIn { it.copy(phase = Phase.FAILED, message = e.message) }
            } finally {
                if (cur === s) closeSession()
            }
        }
    }

    private suspend fun askUser(
        s: Session,
        tid: String,
        from: String,
        name: String,
        size: Long,
        already: Long,
    ): Boolean {
        val signal = CompletableDeferred<Boolean>()
        acceptSignal = signal
        setIn {
            base(tid, Direction.RECEIVE, from, name, size).copy(
                phase = Phase.WAITING_ACCEPT,
                bytes = already,
                message = "در انتظار تأیید شما",
            )
        }
        _state.update {
            it.copy(request = IncomingRequest(tid, from, name, size, already))
        }
        val ok = withTimeoutOrNull(6 * 60_000) { signal.await() } ?: false
        acceptSignal = null
        return ok
    }

    private suspend fun receiveLoop(s: Session, tid: String, name: String, size: Long) {
        val part = partFile(tid)
        part.parentFile?.mkdirs()
        var offset = if (part.exists()) part.length() else 0L
        if (offset > size) {
            part.delete()
            offset = 0L
        }
        val raf = RandomAccessFile(part, "rw")
        raf.seek(offset)
        val digest = if (offset == 0L) MessageDigest.getInstance("SHA-256") else null
        var received = offset
        var speedMark = System.currentTimeMillis()
        var speedBytes = 0L
        var speed = 0L
        var lastUi = 0L
        var senderSha: String? = null
        var finished = false
        setIn {
            it.copy(
                phase = Phase.TRANSFERRING,
                bytes = offset,
                resumedFrom = offset,
                message = if (offset > 0) "ادامه از ${Fmt.bytes(offset)}" else null,
            )
        }
        acquireWake()
        try {
            while (true) {
                val item = withTimeoutOrNull(120_000) { s.inbox.receiveCatching().getOrNull() }
                if (item == null) break
                if (item is Msg.Bin) {
                    raf.write(item.data)
                    received += item.data.size
                    digest?.update(item.data)
                    speedBytes += item.data.size
                    val now = System.currentTimeMillis()
                    if (now - speedMark >= 700) {
                        speed = speedBytes * 1000L / (now - speedMark).coerceAtLeast(1)
                        speedMark = now
                        speedBytes = 0
                    }
                    if (now - lastUi >= 180) {
                        lastUi = now
                        val sp = speed
                        setIn { it.copy(bytes = received, speed = sp, phase = Phase.TRANSFERRING) }
                    }
                    if (received >= size) {
                        finished = true
                        break
                    }
                } else if (item is Msg.Text) {
                    when (item.json.optString("t")) {
                        "end" -> {
                            senderSha = item.json.optString("sha").takeIf { it.isNotEmpty() && it != "null" }
                            finished = true
                        }
                        "cancel", "link-down" -> {}
                        else -> {
                        }
                    }
                    if (finished) break
                }
            }
        } finally {
            runCatching { raf.close() }
            releaseWake()
        }

        if (!finished || received < size) {
            setIn {
                it.copy(
                    phase = Phase.INTERRUPTED,
                    bytes = received,
                    speed = 0,
                    message = "قطع شد — با وصل شدن اینترنت ادامه می‌یابد",
                )
            }
            return
        }

        setIn { it.copy(phase = Phase.VERIFYING, bytes = received, speed = 0, message = "بررسی فایل…") }
        val mySha = digest?.let { Fmt.hex(it.digest()) }
        val ok = senderSha == null || mySha == null || senderSha == mySha
        val target = finalFile(name)
        val moved = part.renameTo(target)
        if (!moved) {
            runCatching { part.copyTo(target, overwrite = true); part.delete() }
        }
        metaFile(tid).delete()
        val stored = StoredFile(
            tid = tid,
            name = name,
            path = target.absolutePath,
            size = received,
            from = s.peer,
            done = ok,
            timestamp = System.currentTimeMillis(),
        )
        addFile(stored)
        sendText(
            s,
            JSONObject().put("t", "done").put("ok", ok).put("size", received).put("sha", mySha ?: JSONObject.NULL),
        )
        setIn {
            it.copy(
                phase = Phase.DONE,
                bytes = received,
                size = received,
                speed = 0,
                sha256 = mySha,
                message = if (ok) "دریافت کامل شد" else "هشدار: چک‌سام مطابقت نداشت",
            )
        }
        note("فایل ذخیره شد: ${target.name}")
    }

    // ------------------------------------------------------------------ WebRTC

    private fun createPeer(s: Session): PeerConnection {
        val cfg = PeerConnection.RTCConfiguration(iceServers()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
            enableDscp = true
        }
        val pc = getFactory().createPeerConnection(cfg, Observer(s))
        if (pc == null) throw IOException("ساخت کانال ممکن نشد")
        s.pc = pc
        return pc
    }

    private fun iceServers(): List<PeerConnection.IceServer> {
        val out = mutableListOf<PeerConnection.IceServer>()
        val stun = listOf(
            "stun:stun.l.google.com:19302",
            "stun:stun1.l.google.com:19302",
            "stun:stun.cloudflare.com:3478",
        )
        for (url in stun) {
            runCatching { out.add(PeerConnection.IceServer.builder(url).createIceServer()) }
        }
        val turn = prefs.turnUrl.trim()
        if (turn.isNotEmpty()) {
            runCatching {
                val b = PeerConnection.IceServer.builder(turn)
                if (prefs.turnUser.isNotEmpty()) b.setUsername(prefs.turnUser)
                if (prefs.turnPass.isNotEmpty()) b.setPassword(prefs.turnPass)
                out.add(b.createIceServer())
            }
        }
        return out
    }

    @Synchronized
    private fun getFactory(): PeerConnectionFactory {
        factory?.let { return it }
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(app)
                .createInitializationOptions()
        )
        val f = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
        factory = f
        return f
    }

    private inner class Observer(private val s: Session) : PeerConnection.Observer {
        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}

        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
            when (p0) {
                PeerConnection.IceConnectionState.FAILED,
                PeerConnection.IceConnectionState.CLOSED,
                -> s.kill("ice")
                PeerConnection.IceConnectionState.DISCONNECTED -> {
                    if (s.openedOnce) {
                        s.kill("ice")
                    }
                }
                else -> {}
            }
        }

        override fun onIceConnectionReceivingChange(p0: Boolean) {}

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
            if (p0 == PeerConnection.IceGatheringState.COMPLETE) s.iceDone.complete(Unit)
        }

        override fun onIceCandidate(p0: IceCandidate?) {}

        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}

        override fun onAddStream(p0: MediaStream?) {}

        override fun onRemoveStream(p0: MediaStream?) {}

        override fun onDataChannel(p0: DataChannel?) {
            if (p0 != null) attachChannel(s, p0)
        }

        override fun onRenegotiationNeeded() {}

        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
    }

    private fun attachChannel(s: Session, dc: DataChannel) {
        s.dc = dc
        dc.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}

            override fun onStateChange() {
                when (dc.state()) {
                    DataChannel.State.OPEN -> {
                        s.openedOnce = true
                        s.open.complete(Unit)
                    }
                    DataChannel.State.CLOSING, DataChannel.State.CLOSED -> s.kill("dc")
                    else -> {}
                }
            }

            override fun onMessage(buffer: DataChannel.Buffer?) {
                val b = buffer ?: return
                val bytes = ByteArray(b.data.remaining())
                b.data.get(bytes)
                if (b.binary) {
                    s.inbox.trySend(Msg.Bin(bytes))
                } else {
                    val json = runCatching { JSONObject(String(bytes, Charsets.UTF_8)) }.getOrNull()
                    if (json != null) s.inbox.trySend(Msg.Text(json))
                }
            }
        })
        if (dc.state() == DataChannel.State.OPEN) {
            s.openedOnce = true
            s.open.complete(Unit)
        }
    }

    private suspend fun createSdp(pc: PeerConnection, offer: Boolean): SessionDescription =
        suspendCancellableCoroutine { cont ->
            val obs = object : SdpObserver {
                override fun onCreateSuccess(p0: SessionDescription?) {
                    if (p0 == null) {
                        if (cont.isActive) cont.resumeWithException(IOException("SDP خالی"))
                    } else if (cont.isActive) {
                        cont.resume(p0)
                    }
                }

                override fun onSetSuccess() {}

                override fun onCreateFailure(p0: String?) {
                    if (cont.isActive) cont.resumeWithException(IOException("SDP: $p0"))
                }

                override fun onSetFailure(p0: String?) {}
            }
            val mc = MediaConstraints()
            if (offer) pc.createOffer(obs, mc) else pc.createAnswer(obs, mc)
        }

    private suspend fun setLocal(pc: PeerConnection, sdp: SessionDescription) {
        val done = withTimeoutOrNull(12_000) {
            suspendCancellableCoroutine<Boolean> { cont ->
                pc.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {
                        if (cont.isActive) cont.resumeWithException(IOException("setLocal: $p0"))
                    }
                }, sdp)
            }
        }
        if (done == null) throw IOException("ثبت SDP محلی ناموفق")
    }

    private suspend fun setRemote(pc: PeerConnection, sdp: SessionDescription) {
        val done = withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine<Boolean> { cont ->
                pc.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {
                        if (cont.isActive) cont.resumeWithException(IOException("setRemote: $p0"))
                    }
                }, sdp)
            }
        }
        if (done == null) throw IOException("ثبت SDP طرف مقابل ناموفق")
    }

    private suspend fun waitIce(s: Session, ms: Long) {
        withTimeoutOrNull(ms) { s.iceDone.await() }
    }

    // ---------------------------------------------------------------- کمکی‌ها

    private class Session(val tid: String, val peer: String, val direction: Direction) {
        @Volatile
        var alive = true

        @Volatile
        var openedOnce = false

        @Volatile
        var sentBytes = 0L

        var pc: PeerConnection? = null
        var dc: DataChannel? = null
        val inbox = Channel<Msg>(Channel.UNLIMITED)
        val open = CompletableDeferred<Unit>()
        val iceDone = CompletableDeferred<Unit>()
        val answer = CompletableDeferred<String>()

        fun kill(reason: String) {
            if (!alive) return
            alive = false
            try {
                inbox.trySend(Msg.Text(JSONObject().put("t", "link-down").put("why", reason)))
            } catch (_: Throwable) {
            }
            answer.cancel()
        }
    }

    private sealed class Attempt {
        object Done : Attempt
        class Interrupted(val offset: Long) : Attempt
    }

    private class RejectedException(message: String) : Exception(message)

    private fun closeSession() {
        val s = cur ?: return
        cur = null
        s.alive = false
        runCatching { s.dc?.unregisterObserver() }
        runCatching { s.dc?.close() }
        runCatching { s.dc?.dispose() }
        runCatching { s.pc?.close() }
        runCatching { s.pc?.dispose() }
        s.dc = null
        s.pc = null
    }

    private fun sendText(s: Session, json: JSONObject) {
        val dc = s.dc ?: throw IOException("کانال بسته است")
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        if (!dc.send(DataChannel.Buffer(ByteBuffer.wrap(bytes), false))) {
            throw IOException("ارسال پیام ناموفق")
        }
    }

    private suspend fun awaitType(s: Session, timeoutMs: Long, vararg types: String): JSONObject? =
        withTimeoutOrNull(timeoutMs) {
            while (true) {
                val m = s.inbox.receiveCatching().getOrNull() ?: return@withTimeoutOrNull null
                if (m is Msg.Text) {
                    val t = m.json.optString("t")
                    if (t == "link-down") return@withTimeoutOrNull null
                    if (types.contains(t)) return@withTimeoutOrNull m.json
                }
            }
            @Suppress("UNREACHABLE_CODE")
            null
        }

    private fun setOut(block: (TransferUi) -> TransferUi) {
        _state.update { st -> st.outgoing?.let { o -> st.copy(outgoing = block(o)) } ?: st }
    }

    private fun setIn(block: (TransferUi) -> TransferUi) {
        _state.update { st -> st.incoming?.let { o -> st.copy(incoming = block(o)) } ?: st }
    }

    private fun base(tid: String, dir: Direction, peer: String, name: String, size: Long) = TransferUi(
        tid = tid,
        direction = dir,
        peerId = peer,
        fileName = name,
        size = size,
    )

    private fun newTid(): String {
        val rnd = SecureRandom()
        val b = ByteArray(8)
        rnd.nextBytes(b)
        return Fmt.hex(b)
    }

    private fun readMeta(uri: String): FileMeta? = runCatching {
        val u = Uri.parse(uri)
        var name = "file"
        var size = -1L
        app.contentResolver.query(u, null, null, null, null)?.use { c ->
            val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val si = c.getColumnIndex(OpenableColumns.SIZE)
            if (c.moveToFirst()) {
                if (ni >= 0) name = c.getString(ni) ?: name
                if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
            }
        }
        if (size < 0) {
            val fd = app.contentResolver.openFileDescriptor(u, "r")
            size = fd?.use { it.statSize } ?: -1L
        }
        val mime = app.contentResolver.getType(u) ?: "application/octet-stream"
        if (size < 0) null else FileMeta(uri, name, size, mime)
    }.getOrNull()

    private fun openInput(uri: String): InputStream? =
        runCatching { app.contentResolver.openInputStream(Uri.parse(uri)) }.getOrNull()

    private fun readChunk(input: InputStream, buf: ByteArray): Int {
        var off = 0
        while (off < buf.size) {
            val r = input.read(buf, off, buf.size - off)
            if (r < 0) break
            off += r
        }
        return off
    }

    private fun skipFully(input: InputStream, n: Long) {
        var left = n
        while (left > 0) {
            val skipped = input.skip(left)
            if (skipped <= 0) {
                if (input.read() < 0) return
                left--
            } else {
                left -= skipped
            }
        }
    }

    private fun sanitize(name: String): String {
        var s = name.replace(Regex("[\\\\/:*?\"<>|\\x00-\\x1f]"), "_").trim()
        if (s.isEmpty()) s = "file"
        if (s.length > 96) {
            val dot = s.lastIndexOf('.')
            s = if (dot > 0 && s.length - dot <= 8) s.substring(0, 60) + s.substring(dot) else s.substring(0, 96)
        }
        return s
    }

    private fun partFile(tid: String) = File(dir, "$tid.part")

    private fun metaFile(tid: String) = File(dir, "$tid.meta")

    private fun writePartialMeta(tid: String, name: String, size: Long, peer: String) {
        runCatching {
            metaFile(tid).writeText(
                JSONObject()
                    .put("tid", tid).put("name", name).put("size", size).put("peer", peer)
                    .put("ts", System.currentTimeMillis()).toString()
            )
        }
    }

    private fun finalFile(name: String): File {
        val safe = sanitize(name)
        val dot = safe.lastIndexOf('.')
        val base = if (dot > 0) safe.substring(0, dot) else safe
        val ext = if (dot > 0) safe.substring(dot) else ""
        var f = File(dir, safe)
        var i = 1
        while (f.exists()) {
            f = File(dir, "$base ($i)$ext")
            i++
        }
        return f
    }

    private fun addFile(file: StoredFile) {
        val current = loadFiles().toMutableList()
        current.add(0, file)
        saveFiles(current.take(200))
        _state.update { it.copy(files = loadFiles()) }
    }

    private fun loadFiles(): List<StoredFile> {
        val raw = prefs.filesJson ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val out = mutableListOf<StoredFile>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    StoredFile(
                        tid = o.optString("tid"),
                        name = o.optString("name"),
                        path = o.optString("path"),
                        size = o.optLong("size"),
                        from = o.optString("from"),
                        done = o.optBoolean("done", true),
                        timestamp = o.optLong("ts"),
                    )
                )
            }
            out
        }.getOrDefault(emptyList())
    }

    private fun saveFiles(list: List<StoredFile>) {
        val arr = JSONArray()
        for (f in list) {
            arr.put(
                JSONObject()
                    .put("tid", f.tid).put("name", f.name).put("path", f.path)
                    .put("size", f.size).put("from", f.from).put("done", f.done)
                    .put("ts", f.timestamp)
            )
        }
        prefs.filesJson = arr.toString()
    }

    private fun acquireWake() {
        if (wake?.isHeld == true) return
        runCatching {
            val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
            wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Cyberdrop:transfer").apply {
                setReferenceCounted(false)
                acquire(2 * 60 * 60 * 1000L)
            }
        }
    }

    private fun releaseWake() {
        runCatching { wake?.release() }
        wake = null
    }
}
