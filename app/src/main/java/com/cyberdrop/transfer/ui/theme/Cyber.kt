package com.cyberdrop.transfer.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * پالت سایبرپانک: مشکی مطلق، مشکی-آبی، یک رنگ نئون اصلی (فیروزه‌ای) و
 * دو رنگ هشدار (سرخابی/کهربایی) که فقط در حالت‌های خاص استفاده می‌شوند.
 */
object Cyber {
    val Bg = Color(0xFF03060A)
    val Panel = Color(0xFF070D14)
    val PanelHi = Color(0xFF0B141D)
    val Line = Color(0xFF0F1F29)

    val Cyan = Color(0xFF00E5FF)
    val CyanSoft = Color(0xFF12B5CC)
    val CyanDim = Color(0xFF0B4E5C)

    val Magenta = Color(0xFFFF2E63)
    val Amber = Color(0xFFFFB020)
    val Green = Color(0xFF35E5A2)

    val Text = Color(0xFFC7EEF3)
    val TextDim = Color(0xFF608894)
    val TextFaint = Color(0xFF32505A)

    val Gap = 14.dp
    val Radius = 3.dp
}

val Mono = FontFamily.Monospace

object Type {
    val title = 26.sp
    val h1 = 20.sp
    val body = 14.sp
    val small = 12.sp
    val tiny = 10.sp
}
