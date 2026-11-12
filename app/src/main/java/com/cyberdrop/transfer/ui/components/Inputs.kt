package com.cyberdrop.transfer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberdrop.transfer.ui.theme.Cyber
import com.cyberdrop.transfer.ui.theme.Mono
import com.cyberdrop.transfer.ui.theme.Type

@Composable
fun CyberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    accent: Color = Cyber.Cyan,
    textSize: TextUnit = 16.sp,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    center: Boolean = false,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        enabled = enabled,
        textStyle = TextStyle(
            color = if (enabled) Cyber.Text else Cyber.TextFaint,
            fontFamily = Mono,
            fontSize = textSize,
            letterSpacing = 2.sp,
        ),
        cursorBrush = SolidColor(accent),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Cyber.PanelHi, RoundedCornerShape(Cyber.Radius))
                    .border(1.dp, accent.copy(alpha = if (enabled) 0.35f else 0.12f), RoundedCornerShape(Cyber.Radius))
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                contentAlignment = if (center) Alignment.Center else Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = Cyber.TextFaint,
                        fontFamily = Mono,
                        fontSize = textSize,
                        letterSpacing = 1.sp,
                    )
                }
                inner()
            }
        },
    )
}

@Composable
fun CyberToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accent: Color = Cyber.Cyan,
) {
    Box(
        Modifier
            .width(56.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(if (checked) accent.copy(alpha = 0.16f) else Cyber.PanelHi)
            .border(1.dp, if (checked) accent.copy(alpha = 0.7f) else Cyber.Line, RoundedCornerShape(2.dp))
            .clickable { onCheckedChange(!checked) }
    ) {
        Box(
            Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .width(20.dp)
                .height(18.dp)
                .background(if (checked) accent else Cyber.TextFaint, RoundedCornerShape(1.dp))
        )
    }
}

@Composable
fun ChoiceChip(text: String, selected: Boolean, accent: Color = Cyber.Cyan, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(if (selected) accent.copy(alpha = 0.16f) else Cyber.PanelHi)
            .border(1.dp, if (selected) accent.copy(alpha = 0.8f) else Cyber.Line, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text,
            color = if (selected) accent else Cyber.TextDim,
            fontFamily = Mono,
            fontSize = Type.small,
            letterSpacing = 1.sp,
        )
    }
}
