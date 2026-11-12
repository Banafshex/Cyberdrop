package com.cyberdrop.transfer.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberdrop.transfer.core.Fmt
import com.cyberdrop.transfer.core.Prefs
import com.cyberdrop.transfer.transfer.TransferEngine
import com.cyberdrop.transfer.ui.components.ChoiceChip
import com.cyberdrop.transfer.ui.components.CyberField
import com.cyberdrop.transfer.ui.components.CyberToggle
import com.cyberdrop.transfer.ui.components.Label
import com.cyberdrop.transfer.ui.components.NeonPanel
import com.cyberdrop.transfer.ui.components.SectionTitle
import com.cyberdrop.transfer.ui.theme.Cyber
import com.cyberdrop.transfer.ui.theme.Mono
import com.cyberdrop.transfer.ui.theme.Type

@Composable
fun SettingsScreen(engine: TransferEngine, myId: String, onBack: () -> Unit) {
    val prefs = engine.prefs
    var name by remember { mutableStateOf(prefs.deviceName) }
    var maxBytes by remember { mutableStateOf(prefs.maxBytes) }
    var autoAccept by remember { mutableStateOf(prefs.autoAccept) }
    var turnUrl by remember { mutableStateOf(prefs.turnUrl) }
    var turnUser by remember { mutableStateOf(prefs.turnUser) }
    var turnPass by remember { mutableStateOf(prefs.turnPass) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        ScreenHeader("تنظیمات", onBack)
        Spacer(Modifier.height(18.dp))

        SectionTitle("محدودیت حجم")
        Spacer(Modifier.height(10.dp))
        NeonPanel(Modifier.fillMaxWidth()) {
            Text(
                "سقف مجاز برای هر فایل: ${Fmt.bytes(maxBytes)}",
                color = Cyber.Text,
                fontFamily = Mono,
                fontSize = Type.body,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1L, 2L, 4L, 8L).forEach { gb ->
                    ChoiceChip(
                        "$gb GB",
                        selected = maxBytes == gb * Prefs.GB,
                        onClick = {
                            maxBytes = gb * Prefs.GB
                            prefs.maxBytes = maxBytes
                        },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "فایل بزرگ‌تر از این مقدار قابل ارسال نیست (هم فرستنده و هم گیرنده همین سقف را دارند).",
                color = Cyber.TextFaint,
                fontFamily = Mono,
                fontSize = 9.sp,
                lineHeight = 15.sp,
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("نام دستگاه")
        Spacer(Modifier.height(10.dp))
        NeonPanel(Modifier.fillMaxWidth(), accent = Cyber.CyanSoft) {
            CyberField(
                value = name,
                onValueChange = {
                    name = it.take(24)
                    prefs.deviceName = name
                },
                placeholder = "نام دستگاه",
            )
            Spacer(Modifier.height(8.dp))
            Label("این نام برای شناسایی شما در گزارش‌ها استفاده می‌شود")
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("دریافت خودکار")
        Spacer(Modifier.height(10.dp))
        NeonPanel(Modifier.fillMaxWidth(), accent = Cyber.CyanSoft) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("پذیرش خودکار فایل", color = Cyber.Text, fontFamily = Mono, fontSize = Type.small)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "اگر روشن باشد، بدون پرسیدن تأیید فایل دریافت می‌شود.",
                        color = Cyber.TextFaint,
                        fontFamily = Mono,
                        fontSize = 9.sp,
                    )
                }
                CyberToggle(
                    checked = autoAccept,
                    onCheckedChange = {
                        autoAccept = it
                        prefs.autoAccept = it
                    },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("سرور رله (اختیاری)")
        Spacer(Modifier.height(10.dp))
        NeonPanel(Modifier.fillMaxWidth(), accent = Cyber.CyanSoft) {
            Text(
                "در بعضی شبکه‌های موبایل (NAT متقارن) ارتباط مستقیم برقرار نمی‌شود. اگر TURN رایگان دارید، نشانی آن را وارد کنید تا به عنوان مسیر پشتیبان استفاده شود.",
                color = Cyber.TextFaint,
                fontFamily = Mono,
                fontSize = 9.sp,
                lineHeight = 15.sp,
            )
            Spacer(Modifier.height(10.dp))
            CyberField(
                value = turnUrl,
                onValueChange = {
                    turnUrl = it
                    prefs.turnUrl = it
                },
                placeholder = "turn:host:3478",
                textSize = Type.small,
            )
            Spacer(Modifier.height(8.dp))
            CyberField(
                value = turnUser,
                onValueChange = {
                    turnUser = it
                    prefs.turnUser = it
                },
                placeholder = "username",
                textSize = Type.small,
            )
            Spacer(Modifier.height(8.dp))
            CyberField(
                value = turnPass,
                onValueChange = {
                    turnPass = it
                    prefs.turnPass = it
                },
                placeholder = "password",
                textSize = Type.small,
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("درباره")
        Spacer(Modifier.height(10.dp))
        NeonPanel(Modifier.fillMaxWidth(), accent = Cyber.CyanSoft) {
            Text("CYBERDROP 1.0", color = Cyber.Cyan, fontFamily = Mono, fontSize = Type.body, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text("شناسهٔ دستگاه: $myId", color = Cyber.TextDim, fontFamily = Mono, fontSize = Type.small)
            Spacer(Modifier.height(8.dp))
            Text(
                "انتقال با WebRTC DataChannel و رمزنگاری DTLS انجام می‌شود. برای رد و بدل کردن اطلاعات اتصال، از بروکرهای عمومی و رایگان MQTT استفاده می‌کنیم؛ هیچ سروری برای اجرا لازم نیست و محتوای فایل هرگز از آن‌ها عبور نمی‌کند.",
                color = Cyber.TextFaint,
                fontFamily = Mono,
                fontSize = 9.sp,
                lineHeight = 16.sp,
            )
        }
        Spacer(Modifier.height(30.dp))
    }
}
