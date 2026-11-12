package com.cyberdrop.transfer.ui.screens

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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberdrop.transfer.core.Fmt
import com.cyberdrop.transfer.transfer.EngineState
import com.cyberdrop.transfer.ui.components.GlitchTitle
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

@Composable
fun HomeScreen(
    state: EngineState,
    myId: String,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onSettings: () -> Unit,
) {
    val clip = LocalClipboardManager.current
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlitchTitle("CYBERDROP")
            Spacer(Modifier.weight(1f))
            StatusDot(online = state.online)
            Spacer(Modifier.width(7.dp))
            Text(
                if (state.online) "ONLINE" else "OFFLINE",
                color = if (state.online) Cyber.Green else Cyber.Magenta,
                fontFamily = Mono,
                fontSize = 9.sp,
            )
            Spacer(Modifier.width(10.dp))
            TinyButton("تنظیمات", accent = Cyber.TextDim, onClick = onSettings)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "انتقال فایل نقطه‌به‌نقطه · بدون سرور · رمزنگاری‌شده",
            color = Cyber.TextFaint,
            fontFamily = Vazir,
            fontSize = Type.tiny,
        )
        Spacer(Modifier.height(22.dp))

        NeonPanel(Modifier.fillMaxWidth()) {
            Label("شناسهٔ شما")
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    myId,
                    color = Cyber.Cyan,
                    fontFamily = Mono,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                TinyButton("کپی") { clip.setText(AnnotatedString(myId)) }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "این شناسه از اولین اجرا ساخته شده و ثابت می‌ماند. آن را به فرستنده بدهید.",
                color = Cyber.TextDim,
                fontFamily = Vazir,
                fontSize = Type.small,
                lineHeight = 21.sp,
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NeonButton(
                "↑  ارسال فایل",
                Modifier.weight(1f),
                accent = Cyber.Cyan,
                filled = true,
                onClick = onSend,
            )
            NeonButton(
                "↓  دریافت",
                Modifier.weight(1f),
                accent = Cyber.CyanSoft,
                onClick = onReceive,
            )
        }

        Spacer(Modifier.height(22.dp))
        SectionTitle("آمار")
        Spacer(Modifier.height(10.dp))
        NeonPanel(Modifier.fillMaxWidth(), accent = Cyber.CyanSoft) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text("فایل‌های دریافت‌شده", color = Cyber.TextDim, fontFamily = Vazir, fontSize = Type.tiny)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        state.files.count { it.done }.toString(),
                        color = Cyber.Cyan,
                        fontFamily = Mono,
                        fontSize = 22.sp,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("حجم دریافتی", color = Cyber.TextDim, fontFamily = Vazir, fontSize = Type.tiny)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        Fmt.bytes(state.files.filter { it.done }.sumOf { it.size }),
                        color = Cyber.Cyan,
                        fontFamily = Mono,
                        fontSize = 22.sp,
                    )
                }
            }
        }

        if (state.log.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            SectionTitle("گزارش زنده")
            Spacer(Modifier.height(8.dp))
            Column {
                state.log.take(4).forEachIndexed { i, line ->
                    Text(
                        "› $line",
                        color = if (i == 0) Cyber.TextDim else Cyber.TextFaint,
                        fontFamily = Vazir,
                        fontSize = Type.tiny,
                        maxLines = 2,
                        lineHeight = 17.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                }
            }
        }

        Spacer(Modifier.height(26.dp))
        Text(
            "CYBERDROP 1.0  ·  WebRTC DataChannel + public rendezvous",
            color = Cyber.TextFaint,
            fontFamily = Mono,
            fontSize = 8.sp,
        )
        Spacer(Modifier.height(24.dp))
    }
}
