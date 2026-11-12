package com.cyberdrop.transfer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberdrop.transfer.core.Fmt
import com.cyberdrop.transfer.transfer.Direction
import com.cyberdrop.transfer.transfer.Phase
import com.cyberdrop.transfer.transfer.TransferUi
import com.cyberdrop.transfer.ui.components.Label
import com.cyberdrop.transfer.ui.components.NeonButton
import com.cyberdrop.transfer.ui.components.NeonPanel
import com.cyberdrop.transfer.ui.components.NeonProgress
import com.cyberdrop.transfer.ui.components.ProgressRing
import com.cyberdrop.transfer.ui.components.StatusDot
import com.cyberdrop.transfer.ui.theme.Cyber
import com.cyberdrop.transfer.ui.theme.Mono
import com.cyberdrop.transfer.ui.theme.Type

fun phaseLabel(t: TransferUi): String = when (t.phase) {
    Phase.CONNECTING -> "برقراری ارتباط"
    Phase.WAITING_PEER -> "در انتظار گیرنده"
    Phase.WAITING_ACCEPT -> "در انتظار تأیید"
    Phase.TRANSFERRING -> "در حال انتقال"
    Phase.VERIFYING -> "بررسی یکپارچگی"
    Phase.INTERRUPTED -> "قطعی — تلاش دوباره"
    Phase.DONE -> "پایان"
    Phase.FAILED -> "خطا"
    Phase.CANCELLED -> "لغو شد"
}

fun phaseColor(t: TransferUi): Color = when (t.phase) {
    Phase.DONE -> Cyber.Green
    Phase.FAILED -> Cyber.Magenta
    Phase.INTERRUPTED -> Cyber.Amber
    Phase.CANCELLED -> Cyber.TextDim
    else -> Cyber.Cyan
}

@Composable
fun TransferCard(
    t: TransferUi,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = phaseColor(t)
    NeonPanel(modifier.fillMaxWidth(), accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(online = t.active, modifier = Modifier.width(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (t.direction == Direction.SEND) "ارسال" else "دریافت",
                color = accent,
                fontFamily = Mono,
                fontSize = Type.small,
                letterSpacing = 3.sp,
            )
            Spacer(Modifier.weight(1f))
            Text("↔ " + t.peerId, color = Cyber.TextDim, fontFamily = Mono, fontSize = Type.tiny, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
                ProgressRing(progress = t.progress, accent = accent, modifier = Modifier.size(78.dp))
                Text(
                    "${(t.progress * 100).toInt()}%",
                    color = accent,
                    fontFamily = Mono,
                    fontSize = Type.small,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    t.fileName,
                    color = Cyber.Text,
                    fontFamily = Mono,
                    fontSize = Type.body,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${Fmt.bytes(t.bytes)} / ${Fmt.bytes(t.size)}",
                    color = Cyber.TextDim,
                    fontFamily = Mono,
                    fontSize = Type.small,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        NeonProgress(t.progress, accent)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(phaseLabel(t), color = accent, fontFamily = Mono, fontSize = Type.tiny, letterSpacing = 2.sp)
            Spacer(Modifier.weight(1f))
            if (t.phase == Phase.TRANSFERRING) {
                Text(
                    "${Fmt.speed(t.speed)}   ·   ${Fmt.eta(t.bytes, t.size, t.speed)}",
                    color = Cyber.TextDim,
                    fontFamily = Mono,
                    fontSize = Type.tiny,
                )
            }
        }
        if (t.resumedFrom > 0L) {
            Spacer(Modifier.height(6.dp))
            Label("ادامه از ${Fmt.bytes(t.resumedFrom)}", color = Cyber.Amber)
        }
        if (t.message != null) {
            Spacer(Modifier.height(8.dp))
            Text(t.message, color = Cyber.TextDim, fontFamily = Mono, fontSize = Type.small)
        }
        if (t.sha256 != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "SHA256 " + t.sha256.take(24) + "…",
                color = Cyber.TextFaint,
                fontFamily = Mono,
                fontSize = Type.tiny,
            )
        }
        if (t.attempt > 1 && t.active) {
            Spacer(Modifier.height(6.dp))
            Text("تلاش ${t.attempt}", color = Cyber.TextFaint, fontFamily = Mono, fontSize = Type.tiny)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (t.active) {
                NeonButton("لغو", Modifier.weight(1f), accent = Cyber.Magenta, onClick = onCancel)
            } else {
                NeonButton("بستن", Modifier.weight(1f), accent = Cyber.TextDim, enabled = true, onClick = onDismiss)
            }
        }
        if (t.phase == Phase.INTERRUPTED) {
            Spacer(Modifier.height(8.dp))
            Text(
                "به‌محض برگشت اینترنت، انتقال از همان نقطه ادامه می‌یابد.",
                color = Cyber.TextFaint,
                fontFamily = Mono,
                fontSize = Type.tiny,
            )
        }
    }
}
