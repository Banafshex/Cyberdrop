package com.cyberdrop.transfer.ui.screens

import androidx.compose.ui.graphics.Color
import com.cyberdrop.transfer.transfer.Phase
import com.cyberdrop.transfer.transfer.TransferUi
import com.cyberdrop.transfer.ui.theme.Cyber

fun phaseLabel(t: TransferUi): String = when (t.phase) {
    Phase.CONNECTING -> "برقراری ارتباط"
    Phase.WAITING_PEER -> "در انتظار پاسخ گیرنده"
    Phase.WAITING_ACCEPT -> "در انتظار تأیید"
    Phase.TRANSFERRING -> if (t.direction == com.cyberdrop.transfer.transfer.Direction.SEND) "در حال آپلود" else "در حال دانلود"
    Phase.VERIFYING -> "بررسی یکپارچگی"
    Phase.INTERRUPTED -> "قطع شد — تلاش دوباره"
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
