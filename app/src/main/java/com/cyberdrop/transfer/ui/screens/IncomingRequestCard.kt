package com.cyberdrop.transfer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberdrop.transfer.core.Fmt
import com.cyberdrop.transfer.transfer.IncomingRequest
import com.cyberdrop.transfer.ui.components.Label
import com.cyberdrop.transfer.ui.components.NeonButton
import com.cyberdrop.transfer.ui.components.NeonPanel
import com.cyberdrop.transfer.ui.theme.Cyber
import com.cyberdrop.transfer.ui.theme.Mono
import com.cyberdrop.transfer.ui.theme.Type

@Composable
fun IncomingRequestCard(
    req: IncomingRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NeonPanel(modifier.fillMaxWidth(), accent = Cyber.Amber) {
        Label("درخواست دریافت فایل", color = Cyber.Amber)
        Spacer(Modifier.height(12.dp))
        Text(
            req.fileName,
            color = Cyber.Text,
            fontFamily = Mono,
            fontSize = Type.body,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${Fmt.bytes(req.size)}   ·   از ${req.fromId}",
            color = Cyber.TextDim,
            fontFamily = Mono,
            fontSize = Type.small,
        )
        if (req.resumeOffset > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                "ادامهٔ فایل نیمه‌کاره از ${Fmt.bytes(req.resumeOffset)}",
                color = Cyber.Cyan,
                fontFamily = Mono,
                fontSize = Type.tiny,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonButton("تأیید و دریافت", Modifier.weight(1f), accent = Cyber.Green, filled = true, onClick = onAccept)
            NeonButton("رد", Modifier.weight(0.6f), accent = Cyber.Magenta, onClick = onReject)
        }
    }
}
