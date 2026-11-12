package com.cyberdrop.transfer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberdrop.transfer.core.Fmt
import com.cyberdrop.transfer.transfer.Direction
import com.cyberdrop.transfer.transfer.IncomingRequest
import com.cyberdrop.transfer.transfer.Phase
import com.cyberdrop.transfer.transfer.TransferUi
import com.cyberdrop.transfer.ui.components.Label
import com.cyberdrop.transfer.ui.components.NeonButton
import com.cyberdrop.transfer.ui.components.NeonPanel
import com.cyberdrop.transfer.ui.components.NeonProgress
import com.cyberdrop.transfer.ui.components.ProgressRing
import com.cyberdrop.transfer.ui.theme.Cyber
import com.cyberdrop.transfer.ui.theme.Mono
import com.cyberdrop.transfer.ui.theme.Type
import com.cyberdrop.transfer.ui.theme.Vazir

/**
 * صفحهٔ تمام‌صفحهٔ انتقال.
 * به‌محض شروع ارسال یا رسیدن پیشنهاد دریافت، این صفحه خودکار روی همه‌چیز می‌آید تا
 * کاربر همیشه بداند چه مرحله‌ای در جریان است، چقدر رفته، با چه سرعتی و چطور لغو کند.
 */
@Composable
fun TransferScreen(
    t: TransferUi,
    request: IncomingRequest?,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = phaseColor(t)
    val sending = t.direction == Direction.SEND
    val title = if (sending) "ارسال فایل" else "دریافت فایل"
    val speedLabel = when {
        t.phase == Phase.DONE -> "میانگین سرعت"
        sending -> "سرعت آپلود"
        else -> "سرعت دانلود"
    }
    val percent = (t.progress * 100).toInt()
    val stage = stageIndex(t)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (sending) "↑" else "↓",
                color = accent,
                fontFamily = Mono,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(10.dp))
            Text(title, color = accent, fontFamily = Vazir, fontSize = Type.h1, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                t.peerId,
                color = Cyber.TextDim,
                fontFamily = Mono,
                fontSize = Type.tiny,
            )
        }
        Spacer(Modifier.height(22.dp))

        // حلقهٔ درصد
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
                ProgressRing(progress = t.progress, accent = accent, modifier = Modifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$percent%",
                        color = accent,
                        fontFamily = Mono,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        phaseLabel(t),
                        color = Cyber.TextDim,
                        fontFamily = Vazir,
                        fontSize = Type.small,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            t.fileName,
            color = Cyber.Text,
            fontFamily = Vazir,
            fontSize = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        // نوار پُر شونده
        NeonProgress(t.progress, accent)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                Fmt.bytes(t.bytes),
                color = accent,
                fontFamily = Mono,
                fontSize = Type.small,
            )
            Spacer(Modifier.weight(1f))
            Text(
                Fmt.bytes(t.size),
                color = Cyber.TextDim,
                fontFamily = Mono,
                fontSize = Type.small,
            )
        }

        Spacer(Modifier.height(18.dp))
        NeonPanel(Modifier.fillMaxWidth(), accent = accent, ticks = false) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(speedLabel, color = Cyber.TextDim, fontFamily = Vazir, fontSize = Type.tiny)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (t.speed > 0) Fmt.speed(t.speed) else "—",
                        color = if (t.speed > 0) accent else Cyber.TextFaint,
                        fontFamily = Mono,
                        fontSize = 15.sp,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("زمان باقی‌مانده", color = Cyber.TextDim, fontFamily = Vazir, fontSize = Type.tiny)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        Fmt.eta(t.bytes, t.size, t.speed),
                        color = Cyber.Text,
                        fontFamily = Mono,
                        fontSize = 15.sp,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("مصرف‌شده", color = Cyber.TextDim, fontFamily = Vazir, fontSize = Type.tiny)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (t.durationMs > 0) "${t.durationMs / 1000}s" else "—",
                        color = Cyber.Text,
                        fontFamily = Mono,
                        fontSize = 15.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        StageTimeline(stage, accent)

        if (t.resumedFrom > 0L) {
            Spacer(Modifier.height(14.dp))
            Label("ادامه از ${Fmt.bytes(t.resumedFrom)}", color = Cyber.Amber)
        }
        if (t.message != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent.copy(alpha = 0.07f))
                    .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                    .padding(12.dp)
            ) {
                Text(
                    t.message,
                    color = Cyber.Text,
                    fontFamily = Vazir,
                    fontSize = Type.small,
                    lineHeight = 21.sp,
                )
            }
        }
        if (t.sha256 != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                "SHA256 " + t.sha256.take(32) + "…",
                color = Cyber.TextFaint,
                fontFamily = Mono,
                fontSize = 9.sp,
            )
        }

        Spacer(Modifier.height(24.dp))
        when {
            request != null -> {
                Text(
                    if (request.resumeOffset > 0)
                        "این فایل قبلاً نیمه‌کاره مانده؛ با تأیید، از همان‌جا ادامه می‌یابد."
                    else
                        "فایل «${request.fileName}» (${Fmt.bytes(request.size)}) از ${request.fromId} برای شما آماده است.",
                    color = Cyber.TextDim,
                    fontFamily = Vazir,
                    fontSize = Type.small,
                    lineHeight = 22.sp,
                )
                Spacer(Modifier.height(14.dp))
                NeonButton(
                    "تأیید و دریافت",
                    Modifier.fillMaxWidth(),
                    accent = Cyber.Green,
                    filled = true,
                    onClick = onAccept,
                )
                Spacer(Modifier.height(10.dp))
                NeonButton("رد کردن", Modifier.fillMaxWidth(), accent = Cyber.Magenta, onClick = onReject)
            }

            t.active -> {
                NeonButton(
                    if (t.phase == Phase.WAITING_ACCEPT) "لغو درخواست" else "لغو انتقال",
                    Modifier.fillMaxWidth(),
                    accent = Cyber.Magenta,
                    onClick = onCancel,
                )
            }

            else -> {
                NeonButton("بستن", Modifier.fillMaxWidth(), accent = Cyber.CyanSoft, onClick = onDismiss)
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun StageTimeline(current: Int, accent: Color) {
    val stages = listOf("اتصال", "تأیید", "انتقال", "پایان")
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            stages.forEachIndexed { i, name ->
                val done = i < current
                val active = i == current
                val color = when {
                    active -> accent
                    done -> accent.copy(alpha = 0.55f)
                    else -> Cyber.TextFaint
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(name, color = color, fontFamily = Vazir, fontSize = 10.sp)
                }
                if (i < stages.lastIndex) {
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(bottom = 16.dp)
                            .height(1.dp)
                            .background(if (i < current) accent.copy(alpha = 0.55f) else Cyber.Line)
                    )
                }
            }
        }
    }
}

fun stageIndex(t: TransferUi): Int = when (t.phase) {
    Phase.CONNECTING -> 0
    Phase.WAITING_PEER, Phase.WAITING_ACCEPT -> 1
    Phase.TRANSFERRING, Phase.INTERRUPTED -> 2
    Phase.VERIFYING, Phase.DONE -> 3
    else -> 0
}
