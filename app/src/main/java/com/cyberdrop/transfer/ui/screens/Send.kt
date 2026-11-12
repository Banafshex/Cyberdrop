package com.cyberdrop.transfer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberdrop.transfer.core.Fmt
import com.cyberdrop.transfer.core.Identity
import com.cyberdrop.transfer.transfer.EngineState
import com.cyberdrop.transfer.transfer.FileMeta
import com.cyberdrop.transfer.ui.components.CyberField
import com.cyberdrop.transfer.ui.components.Label
import com.cyberdrop.transfer.ui.components.NeonButton
import com.cyberdrop.transfer.ui.components.NeonPanel
import com.cyberdrop.transfer.ui.components.NeonProgress
import com.cyberdrop.transfer.ui.components.SectionTitle
import com.cyberdrop.transfer.ui.components.TinyButton
import com.cyberdrop.transfer.ui.theme.Cyber
import com.cyberdrop.transfer.ui.theme.Mono
import com.cyberdrop.transfer.ui.theme.Type

@Composable
fun SendScreen(
    state: EngineState,
    myId: String,
    maxBytes: Long,
    describe: (Uri) -> FileMeta?,
    onSend: (Uri, String) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    var picked by remember { mutableStateOf<FileMeta?>(null) }
    var receiverRaw by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            picked = describe(uri)
        }
    }

    val formatted = Identity.normalize(receiverRaw)
    val tooBig = picked != null && picked!!.size > maxBytes
    val canSend = picked != null && formatted.isNotEmpty() && formatted != myId && !tooBig && state.outgoing?.active != true
    val busy = state.outgoing?.active == true

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        ScreenHeader("ارسال فایل", onBack)
        Spacer(Modifier.height(18.dp))

        state.outgoing?.let { t ->
            TransferCard(t, onCancel = onCancel, onDismiss = onDismiss)
            Spacer(Modifier.height(18.dp))
        }

        if (!busy) {
            SectionTitle("۰۱ // انتخاب فایل")
            Spacer(Modifier.height(10.dp))
            NeonPanel(Modifier.fillMaxWidth()) {
                if (picked == null) {
                    Text(
                        "هیچ فایلی انتخاب نشده است.",
                        color = Cyber.TextDim,
                        fontFamily = Mono,
                        fontSize = Type.small,
                    )
                } else {
                    Text(
                        picked!!.name,
                        color = Cyber.Text,
                        fontFamily = Mono,
                        fontSize = Type.body,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            Fmt.bytes(picked!!.size),
                            color = if (tooBig) Cyber.Magenta else Cyber.Cyan,
                            fontFamily = Mono,
                            fontSize = Type.small,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(picked!!.mime, color = Cyber.TextFaint, fontFamily = Mono, fontSize = 9.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    NeonProgress(
                        progress = if (maxBytes > 0) picked!!.size.toFloat() / maxBytes.toFloat() else 0f,
                        accent = if (tooBig) Cyber.Magenta else Cyber.CyanSoft,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "سقف مجاز ارسال: ${Fmt.bytes(maxBytes)}",
                        color = Cyber.TextFaint,
                        fontFamily = Mono,
                        fontSize = 9.sp,
                    )
                }
                Spacer(Modifier.height(14.dp))
                NeonButton(
                    if (picked == null) "انتخاب فایل" else "انتخاب فایل دیگر",
                    Modifier.fillMaxWidth(),
                    accent = Cyber.Cyan,
                    onClick = { picker.launch(arrayOf("*/*")) },
                )
            }

            if (tooBig) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "حجم فایل از سقف مجاز بیشتر است. سقف را از تنظیمات بالا ببرید.",
                    color = Cyber.Magenta,
                    fontFamily = Mono,
                    fontSize = Type.tiny,
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("۰۲ // شناسهٔ گیرنده")
            Spacer(Modifier.height(10.dp))
            NeonPanel(Modifier.fillMaxWidth(), accent = Cyber.CyanSoft) {
                CyberField(
                    value = receiverRaw,
                    onValueChange = { v ->
                        receiverRaw = v.uppercase().filter { it.isLetterOrDigit() }.take(10)
                    },
                    placeholder = "مثلاً 7F3A92K1",
                    keyboardType = KeyboardType.Ascii,
                    textSize = 18.sp,
                    accent = if (formatted.isNotEmpty()) Cyber.Green else Cyber.Cyan,
                )
                Spacer(Modifier.height(8.dp))
                Label(
                    when {
                        receiverRaw.isEmpty() -> "هشت نویسهٔ شناسهٔ گیرنده"
                        formatted.isEmpty() -> "شناسه ناقص است (${receiverRaw.length}/8)"
                        formatted == myId -> "این شناسهٔ خودتان است"
                        else -> "مقصد: $formatted"
                    },
                    color = when {
                        formatted.isEmpty() -> Cyber.Amber
                        formatted == myId -> Cyber.Magenta
                        else -> Cyber.Green
                    },
                )
            }

            Spacer(Modifier.height(20.dp))
            NeonButton(
                "ارسال",
                Modifier.fillMaxWidth(),
                accent = Cyber.Cyan,
                filled = true,
                enabled = canSend,
                onClick = { picked?.let { onSend(Uri.parse(it.uri), receiverRaw) } },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "فایل مستقیماً روی کانال رمزنگاری‌شده به گوشی گیرنده می‌رود؛ اگر ارتباط قطع شود، از همان‌جا ادامه پیدا می‌کند.",
                color = Cyber.TextFaint,
                fontFamily = Mono,
                fontSize = 9.sp,
                lineHeight = 15.sp,
            )
        } else {
            Spacer(Modifier.height(8.dp))
            TinyButton("لغو و بازگشت", accent = Cyber.TextDim, onClick = onCancel)
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TinyButton("‹  بازگشت", accent = Cyber.TextDim, onClick = onBack)
        Spacer(Modifier.weight(1f))
        Text(title, color = Cyber.Cyan, fontFamily = Mono, fontSize = Type.h1, letterSpacing = 2.sp)
    }
}
