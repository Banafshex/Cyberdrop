package com.cyberdrop.transfer.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.cyberdrop.transfer.core.Fmt
import com.cyberdrop.transfer.transfer.EngineState
import com.cyberdrop.transfer.transfer.StoredFile
import com.cyberdrop.transfer.ui.components.Label
import com.cyberdrop.transfer.ui.components.NeonButton
import com.cyberdrop.transfer.ui.components.NeonPanel
import com.cyberdrop.transfer.ui.components.SectionTitle
import com.cyberdrop.transfer.ui.components.StatusDot
import com.cyberdrop.transfer.ui.components.TinyButton
import com.cyberdrop.transfer.ui.theme.Cyber
import com.cyberdrop.transfer.ui.theme.Mono
import com.cyberdrop.transfer.ui.theme.Type
import com.cyberdrop.transfer.ui.theme.Vazir
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiveScreen(
    state: EngineState,
    myId: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val clip = LocalClipboardManager.current
    val ctx = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        ScreenHeader("دریافت", onBack)
        Spacer(Modifier.height(18.dp))

        NeonPanel(Modifier.fillMaxWidth(), accent = if (state.online) Cyber.Green else Cyber.Magenta) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(online = state.online)
                Spacer(Modifier.weight(1f))
                Text(
                    if (state.online) "آمادهٔ دریافت" else "آفلاین",
                    color = if (state.online) Cyber.Green else Cyber.Magenta,
                    fontFamily = Vazir,
                    fontSize = Type.small,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    myId,
                    color = Cyber.Cyan,
                    fontFamily = Mono,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                TinyButton("کپی") { clip.setText(AnnotatedString(myId)) }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                state.statusText + if (state.broker.isNotEmpty() && state.online) " · ${state.broker}" else "",
                color = Cyber.TextDim,
                fontFamily = Vazir,
                fontSize = Type.tiny,
            )
            if (!state.online) {
                Spacer(Modifier.height(12.dp))
                NeonButton("تلاش دوباره برای اتصال", Modifier.fillMaxWidth(), accent = Cyber.Amber, onClick = onRetry)
            }
            Spacer(Modifier.height(12.dp))
            Label("برای دریافت، همین صفحه یا صفحهٔ اصلی کافی است")
            Spacer(Modifier.height(4.dp))
            Label("به‌محض رسیدن پیشنهاد، صفحهٔ انتقال خودکار باز می‌شود")
        }

        Spacer(Modifier.height(22.dp))
        SectionTitle("فایل‌های دریافتی")
        Spacer(Modifier.height(10.dp))
        if (state.files.isEmpty()) {
            NeonPanel(Modifier.fillMaxWidth(), accent = Cyber.CyanSoft) {
                Text(
                    "هنوز فایلی دریافت نشده است.",
                    color = Cyber.TextDim,
                    fontFamily = Vazir,
                    fontSize = Type.small,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.files.take(30).forEach { f ->
                    FileRow(f) { shareFile(ctx, f) }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun FileRow(f: StoredFile, onShare: () -> Unit) {
    val ctx = LocalContext.current
    NeonPanel(Modifier.fillMaxWidth(), accent = Cyber.Line, ticks = false) {
        Text(
            f.name,
            color = Cyber.Text,
            fontFamily = Vazir,
            fontSize = Type.small,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Fmt.bytes(f.size) + " · " + timeLabel(f.timestamp),
                color = Cyber.TextFaint,
                fontFamily = Mono,
                fontSize = 9.sp,
            )
            Spacer(Modifier.weight(1f))
            TinyButton("اشتراک") { onShare() }
            if (File(f.path).exists()) {
                Spacer(Modifier.width(6.dp))
                TinyButton("باز کردن") { openFile(ctx, f) }
            }
        }
    }
}

private fun timeLabel(ts: Long): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(ts))

private fun uriFor(ctx: android.content.Context, f: StoredFile) =
    FileProvider.getUriForFile(ctx, ctx.packageName + ".files", File(f.path))

private fun shareFile(ctx: android.content.Context, f: StoredFile) {
    runCatching {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uriFor(ctx, f))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(i, "اشتراک‌گذاری فایل"))
    }
}

private fun openFile(ctx: android.content.Context, f: StoredFile) {
    runCatching {
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriFor(ctx, f), "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(i)
    }
}
