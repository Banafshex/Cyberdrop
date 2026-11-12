package com.cyberdrop.transfer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberdrop.transfer.ui.theme.Cyber
import com.cyberdrop.transfer.ui.theme.Mono
import com.cyberdrop.transfer.ui.theme.Type

/** پس‌زمینهٔ شبکه‌ای + خط اسکن متحرک + وینیت. */
@Composable
fun CyberBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val transition = rememberInfiniteTransition(label = "bg")
    val scan by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6400, easing = LinearEasing), RepeatMode.Restart),
        label = "scan",
    )
    Box(modifier.fillMaxSize().background(Cyber.Bg)) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 46.dp.toPx()
            var x = 0f
            while (x <= size.width) {
                drawLine(Cyber.Line, Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(Cyber.Line, Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
            val sy = scan * size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Cyber.Cyan.copy(alpha = 0.05f), Color.Transparent),
                    startY = sy - 160f,
                    endY = sy + 160f,
                ),
                topLeft = Offset(0f, sy - 160f),
                size = Size(size.width, 320f),
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0xCC000000)),
                    center = Offset(size.width / 2f, size.height / 2.4f),
                    radius = size.maxDimension * 0.75f,
                ),
                size = size,
            )
        }
        content()
    }
}

fun Modifier.cornerTicks(color: Color, len: Dp = 10.dp, width: Dp = 1.5.dp): Modifier = drawBehind {
    val l = len.toPx()
    val w = width.toPx()
    drawLine(color, Offset(0f, 0f), Offset(l, 0f), w)
    drawLine(color, Offset(0f, 0f), Offset(0f, l), w)
    drawLine(color, Offset(size.width, 0f), Offset(size.width - l, 0f), w)
    drawLine(color, Offset(size.width, 0f), Offset(size.width, l), w)
    drawLine(color, Offset(0f, size.height), Offset(l, size.height), w)
    drawLine(color, Offset(0f, size.height), Offset(0f, size.height - l), w)
    drawLine(color, Offset(size.width, size.height), Offset(size.width - l, size.height), w)
    drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - l), w)
}

@Composable
fun NeonPanel(
    modifier: Modifier = Modifier,
    accent: Color = Cyber.Cyan,
    ticks: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .background(Cyber.Panel.copy(alpha = 0.94f), RoundedCornerShape(Cyber.Radius))
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(Cyber.Radius))
            .then(if (ticks) Modifier.cornerTicks(accent.copy(alpha = 0.85f)) else Modifier)
            .padding(16.dp),
        content = content,
    )
}

/** برچسب کوچک با خط تیره، سبک HUD. */
@Composable
fun Label(text: String, color: Color = Cyber.TextDim, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.height(1.dp).fillMaxWidth(0.06f).background(color.copy(alpha = 0.5f)))
        Text(
            " " + text,
            color = color,
            fontFamily = Mono,
            fontSize = Type.tiny,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
fun SectionTitle(text: String, color: Color = Cyber.TextDim, modifier: Modifier = Modifier) {
    Text(
        text,
        color = color,
        fontFamily = Mono,
        fontSize = Type.small,
        letterSpacing = 3.sp,
        modifier = modifier,
    )
}

@Composable
fun GlitchTitle(text: String, color: Color = Cyber.Cyan, size: TextUnit = Type.title) {
    val t = rememberInfiniteTransition(label = "glitch")
    val shift by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift",
    )
    Box {
        Text(
            text,
            color = Cyber.Magenta.copy(alpha = 0.30f),
            fontFamily = Mono,
            fontSize = size,
            letterSpacing = 4.sp,
            modifier = Modifier.offset(x = (1 + 2 * shift).dp),
        )
        Text(
            text,
            color = Cyber.Cyan.copy(alpha = 0.30f),
            fontFamily = Mono,
            fontSize = size,
            letterSpacing = 4.sp,
            modifier = Modifier.offset(x = (-1 - 2 * shift).dp),
        )
        Text(text, color = color, fontFamily = Mono, fontSize = size, letterSpacing = 4.sp)
    }
}

@Composable
fun NeonButton(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = Cyber.Cyan,
    enabled: Boolean = true,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .clip(RoundedCornerShape(Cyber.Radius))
            .background(if (filled) accent.copy(alpha = 0.12f) else Cyber.PanelHi)
            .border(
                1.dp,
                if (enabled) accent.copy(alpha = if (filled) 0.75f else 0.4f) else Cyber.Line,
                RoundedCornerShape(Cyber.Radius),
            )
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (enabled) accent else Cyber.TextFaint,
            fontFamily = Mono,
            fontSize = 15.sp,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun TinyButton(text: String, accent: Color = Cyber.CyanSoft, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = accent, fontFamily = Mono, fontSize = Type.tiny, letterSpacing = 1.sp)
    }
}

/** نوار پیشرفت با تیک‌های ظریف و سرِ نورانی. */
@Composable
fun NeonProgress(progress: Float, accent: Color = Cyber.Cyan, modifier: Modifier = Modifier) {
    val p = progress.coerceIn(0f, 1f)
    Canvas(modifier.fillMaxWidth().height(12.dp)) {
        drawRect(Cyber.Line)
        val w = size.width * p
        if (w > 0f) {
            drawRect(accent.copy(alpha = 0.16f), size = Size(w, size.height))
            drawRect(accent.copy(alpha = 0.85f), topLeft = Offset(0f, size.height * 0.55f), size = Size(w, size.height * 0.45f))
            drawRect(Color.White.copy(alpha = 0.85f), topLeft = Offset((w - 2f).coerceAtLeast(0f), 0f), size = Size(2f, size.height))
        }
        var x = 0f
        val tick = 9.dp.toPx()
        while (x < size.width) {
            drawLine(Cyber.Bg, Offset(x, 0f), Offset(x, size.height), 1f)
            x += tick
        }
    }
}

/** حلقهٔ پیشرفت ۲۷۰ درجه. */
@Composable
fun ProgressRing(
    progress: Float,
    accent: Color = Cyber.Cyan,
    modifier: Modifier = Modifier,
    trackColor: Color = Cyber.Line,
) {
    val p = progress.coerceIn(0f, 1f)
    Canvas(modifier) {
        val stroke = 8.dp.toPx()
        val inset = stroke * 1.6f
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        drawArc(trackColor, 135f, 270f, false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
        if (p > 0f) {
            drawArc(accent.copy(alpha = 0.20f), 135f, 270f * p, false, topLeft = topLeft, size = arcSize, style = Stroke(stroke * 3f, cap = StrokeCap.Butt))
            drawArc(accent, 135f, 270f * p, false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
        }
        var angle = 135f
        while (angle <= 405f) {
            val rad = Math.toRadians(angle.toDouble())
            val cx = size.width / 2 + Math.cos(rad).toFloat() * (size.width / 2 - 1.dp.toPx())
            val cy = size.height / 2 + Math.sin(rad).toFloat() * (size.height / 2 - 1.dp.toPx())
            drawCircle(accent.copy(alpha = 0.35f), 1.2f.dp.toPx(), Offset(cx, cy))
            angle += 13.5f
        }
    }
}

@Composable
fun KvRow(key: String, value: String, valueColor: Color = Cyber.Text, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(key, color = Cyber.TextDim, fontFamily = Mono, fontSize = Type.small, letterSpacing = 1.sp)
        Box(Modifier.weight(1f))
        Text(value, color = valueColor, fontFamily = Mono, fontSize = Type.small, letterSpacing = 1.sp)
    }
}

@Composable
fun StatusDot(online: Boolean, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "dot")
    val pulse by t.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    Canvas(modifier.height(8.dp).fillMaxWidth(0.06f)) {
        val color = if (online) Cyber.Green else Cyber.Magenta
        drawCircle(color.copy(alpha = 0.25f * pulse), size.minDimension / 2)
        drawCircle(color, size.minDimension / 3.2f)
    }
}
