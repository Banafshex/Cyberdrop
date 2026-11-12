package com.cyberdrop.transfer.transfer

import org.json.JSONObject

enum class Direction { SEND, RECEIVE }

enum class Phase {
    CONNECTING,
    WAITING_PEER,
    WAITING_ACCEPT,
    TRANSFERRING,
    VERIFYING,
    INTERRUPTED,
    DONE,
    FAILED,
    CANCELLED,
}

data class TransferUi(
    val tid: String,
    val direction: Direction,
    val peerId: String,
    val fileName: String,
    val size: Long,
    val bytes: Long = 0L,
    val phase: Phase = Phase.CONNECTING,
    val speed: Long = 0L,
    val resumedFrom: Long = 0L,
    val attempt: Int = 1,
    val message: String? = null,
    val sha256: String? = null,
    val durationMs: Long = 0L,
) {
    val progress: Float
        get() = if (size <= 0L) 0f else (bytes.toDouble() / size.toDouble()).toFloat().coerceIn(0f, 1f)

    /** انتقال «در جریان» است (شامل حالتی که موتور خودش در حال تلاش مجدد است). */
    val active: Boolean
        get() = phase == Phase.CONNECTING || phase == Phase.WAITING_PEER ||
            phase == Phase.WAITING_ACCEPT || phase == Phase.TRANSFERRING ||
            phase == Phase.VERIFYING || phase == Phase.INTERRUPTED
}

data class IncomingRequest(
    val tid: String,
    val fromId: String,
    val fileName: String,
    val size: Long,
    val resumeOffset: Long,
)

data class StoredFile(
    val tid: String,
    val name: String,
    val path: String,
    val size: Long,
    val from: String,
    val done: Boolean,
    val timestamp: Long,
)

data class EngineState(
    val online: Boolean = false,
    val connecting: Boolean = false,
    val broker: String = "",
    val statusText: String = "آماده",
    val outgoing: TransferUi? = null,
    val incoming: TransferUi? = null,
    val request: IncomingRequest? = null,
    val files: List<StoredFile> = emptyList(),
    val log: List<String> = emptyList(),
    val resumable: Boolean = false,
    val resumeName: String? = null,
    val resumeSize: Long = 0L,
    val resumePeer: String? = null,
) {
    val busy: Boolean get() = outgoing?.active == true || incoming?.active == true
}

sealed class Msg {
    class Bin(val data: ByteArray) : Msg()
    class Text(val json: JSONObject) : Msg()
}

data class FileMeta(val uri: String, val name: String, val size: Long, val mime: String)
