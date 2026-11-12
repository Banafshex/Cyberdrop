package com.cyberdrop.transfer.net

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

/**
 * کانال «آشتی‌دهی» (signaling).
 *
 * برای برقراری ارتباط مستقیم بین دو گوشی، هر دستگاه باید نشانی عمومی خودش را
 * به طرف مقابل برساند. این کار با یک بروکر MQTT عمومی و رایگان انجام می‌شود؛
 * هیچ سروری برای شما لازم نیست (نه نصب، نه هزینه).
 * خودِ فایل هرگز از این کانال عبور نمی‌کند: انتقال روی DataChannel و
 * رمزنگاری‌شده با DTLS/SCTP انجام می‌شود.
 */
class Signaling(
    private val selfId: String,
    private val onMessage: (JSONObject) -> Unit,
    private val onStatus: (Boolean, String) -> Unit,
) {

    private var client: MqttAsyncClient? = null

    @Volatile
    private var connected = false

    fun isConnected(): Boolean = connected

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        if (connected) return@withContext true
        onStatus(false, "در حال اتصال به شبکه…")
        for ((name, url) in BROKERS) {
            var c: MqttAsyncClient? = null
            try {
                c = MqttAsyncClient(
                    url,
                    "cd_" + UUID.randomUUID().toString().replace("-", "").take(14),
                    MemoryPersistence()
                )
                val topic = topicFor(selfId)
                c.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        connected = true
                        onStatus(true, name)
                        try {
                            c.subscribe(topic, 1)
                        } catch (t: Throwable) {
                            Log.w(TAG, "resubscribe failed: ${t.message}")
                        }
                    }

                    override fun connectionLost(cause: Throwable?) {
                        connected = false
                        onStatus(false, "ارتباط برقرار نیست")
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        val payload = message?.payload ?: return
                        try {
                            onMessage(JSONObject(String(payload, Charsets.UTF_8)))
                        } catch (t: Throwable) {
                            Log.w(TAG, "bad signal: ${t.message}")
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })
                val opts = MqttConnectOptions().apply {
                    isAutomaticReconnect = true
                    isCleanSession = true
                    connectionTimeout = 12
                    keepAliveInterval = 30
                    maxInflight = 500
                    mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
                }
                c.connect(opts).waitForCompletion(15000)
                c.subscribe(topic, 1).waitForCompletion(10000)
                client = c
                connected = true
                onStatus(true, name)
                Log.i(TAG, "signaling up via $name")
                return@withContext true
            } catch (t: Throwable) {
                Log.w(TAG, "broker $name failed: ${t.message}")
                connected = false
                try {
                    c?.close()
                } catch (_: Throwable) {
                }
                try {
                    c?.disconnectForcibly(0, 500)
                } catch (_: Throwable) {
                }
                client = null
                onStatus(false, "تلاش با سرور بعدی…")
            }
        }
        onStatus(false, "اتصال برقرار نشد")
        false
    }

    fun publish(to: String, type: String, sdp: String? = null, tid: String? = null): Boolean {
        val c = client ?: return false
        return try {
            val obj = JSONObject()
            obj.put("v", 1)
            obj.put("t", type)
            obj.put("from", selfId)
            obj.put("to", to)
            obj.put("ts", System.currentTimeMillis())
            if (sdp != null) obj.put("sdp", sdp)
            if (tid != null) obj.put("tid", tid)
            val msg = MqttMessage(obj.toString().toByteArray(Charsets.UTF_8))
            msg.qos = 1
            c.publish(topicFor(to), msg)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "publish failed: ${t.message}")
            false
        }
    }

    fun close() {
        connected = false
        try {
            client?.disconnectForcibly(100, 500)
        } catch (_: Throwable) {
        }
        try {
            client?.close()
        } catch (_: Throwable) {
        }
        client = null
    }

    companion object {
        private const val TAG = "CyberdropSig"

        /** بروکرهای عمومی و رایگان؛ به ترتیب امتحان می‌شوند. */
        val BROKERS: List<Pair<String, String>> = listOf(
            "EMQX" to "ssl://broker.emqx.io:8883",
            "HiveMQ" to "ssl://broker.hivemq.com:8883",
            "Mosquitto" to "ssl://test.mosquitto.org:8883",
        )

        /** نام تاپیک از هشِ شناسه ساخته می‌شود تا شناسه روی بروکر عمومی لو نرود. */
        fun topicFor(id: String): String {
            val d = MessageDigest.getInstance("SHA-256").digest(id.toByteArray(Charsets.UTF_8))
            val sb = StringBuilder("cyberdrop/v2/sig/")
            for (i in 0 until 12) {
                val v = d[i].toInt() and 0xff
                sb.append("0123456789abcdef"[v shr 4])
                sb.append("0123456789abcdef"[v and 0x0f])
            }
            return sb.toString()
        }
    }
}
