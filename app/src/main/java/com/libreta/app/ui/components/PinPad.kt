package com.libreta.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T
import kotlin.math.roundToInt

/** Teclado numérico para PIN. onSubmit devuelve true si el PIN es correcto. */
@Composable
fun PinPad(
    title: String,
    subtitle: String,
    length: Int = 4,
    onBiometric: (() -> Unit)? = null,
    onSubmit: (String) -> Boolean,
) {
    val p = LocalPalette.current
    var pin by remember { mutableStateOf("") }
    var errorTick by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(errorTick) {
        if (errorTick > 0) {
            for (x in listOf(18f, -14f, 10f, -6f, 0f)) shake.animateTo(x)
        }
    }
    fun press(d: String) {
        if (pin.length >= length) return
        pin += d
        if (pin.length == length) {
            if (!onSubmit(pin)) { errorTick++; pin = "" }
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title.uppercase(), style = T.headingSm, color = p.ink)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = T.bodySm, color = p.inkSoft)
        Spacer(Modifier.height(22.dp))
        Row(Modifier.offset { IntOffset(shake.value.roundToInt(), 0) }, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(length) { i ->
                Box(
                    Modifier.size(18.dp).clip(CircleShape)
                        .background(if (i < pin.length) p.primary else p.surface)
                        .border(2.dp, p.line, CircleShape),
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("bio", "0", "del"))
        rows.forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                r.forEach { k ->
                    when (k) {
                        "bio" -> if (onBiometric != null) {
                            Pressable(Modifier.size(68.dp), color = p.iceWash, onClick = onBiometric) {
                                Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Fingerprint, "Huella", tint = p.ink) }
                            }
                        } else Spacer(Modifier.size(68.dp))
                        "del" -> Pressable(Modifier.size(68.dp), color = p.surface, onClick = { pin = pin.dropLast(1) }) {
                            Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.Backspace, "Borrar", tint = p.ink) }
                        }
                        else -> Pressable(Modifier.size(68.dp), color = p.surface, onClick = { press(k) }) {
                            Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) { Text(k, style = T.heading, color = p.ink) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}
