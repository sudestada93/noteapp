package com.libreta.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.libreta.app.ui.theme.Dims
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T

val BrutalShape = RoundedCornerShape(Dims.radius)

/** Sombra dura desplazada (sin desenfoque) hacia abajo-izquierda, firma del sistema. */
fun Modifier.hardShadow(color: Color, offset: Dp = Dims.shadow, progress: Float = 0f, radius: Dp = Dims.radius): Modifier =
    this.drawBehind {
        val o = offset.toPx() * (1f - progress)
        if (o > 0.1f) drawRoundRect(color, topLeft = Offset(-o, o), size = size, cornerRadius = CornerRadius(radius.toPx()))
    }

@Composable
fun rememberHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) { { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) } }
}

/**
 * Superficie interactiva "presionable": al tocarla se hunde dentro de su sombra.
 */
@Composable
fun Pressable(
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.surface,
    borderColor: Color = LocalPalette.current.line,
    border: Dp = Dims.border,
    shadow: Dp = Dims.shadow,
    shadowColor: Color = LocalPalette.current.shadow,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val t by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessHigh), label = "press")
    val haptic = rememberHaptic()
    val clickMod = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interaction, indication = null,
            onClick = { haptic(); onClick?.invoke() },
            onLongClick = onLongClick?.let { lc -> { haptic(); lc() } },
        )
    } else Modifier
    Box(
        modifier
            .graphicsLayer {
                val s = shadow.toPx() * t
                translationX = -s
                translationY = s
            }
            .hardShadow(shadowColor, shadow, t)
            .clip(BrutalShape)
            .background(color)
            .border(BorderStroke(border, borderColor), BrutalShape)
            .then(clickMod),
    ) { content() }
}

@Composable
fun BrutalCard(
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.surface,
    shadow: Dp = Dims.shadow,
    padding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Pressable(modifier, color = color, shadow = shadow, onClick = onClick, onLongClick = onLongClick) {
        Column(Modifier.padding(padding), content = content)
    }
}

enum class BtnStyle { Primary, Secondary, Danger, Ghost, Canary }

@Composable
fun BrutalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: BtnStyle = BtnStyle.Primary,
    small: Boolean = false,
    enabled: Boolean = true,
) {
    val p = LocalPalette.current
    val bg = when (style) {
        BtnStyle.Primary -> p.primary
        BtnStyle.Secondary -> p.surface
        BtnStyle.Danger -> p.danger
        BtnStyle.Ghost -> Color.Transparent
        BtnStyle.Canary -> p.canary
    }
    val fg = when (style) {
        BtnStyle.Primary -> p.onPrimary
        BtnStyle.Danger, BtnStyle.Canary -> Color(0xFF383838)
        else -> p.ink
    }
    if (style == BtnStyle.Ghost) {
        Row(
            modifier.clip(BrutalShape).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) { Icon(icon, null, tint = p.ink, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
            Text(text.uppercase(), style = T.label, color = p.ink)
        }
        return
    }
    Pressable(
        modifier.graphicsLayer { alpha = if (enabled) 1f else 0.45f },
        color = bg,
        shadow = if (small) 3.dp else 4.dp,
        onClick = if (enabled) onClick else null,
    ) {
        Row(
            Modifier.padding(horizontal = if (small) 12.dp else 18.dp, vertical = if (small) 7.dp else 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(if (small) 16.dp else 18.dp))
                if (text.isNotEmpty()) Spacer(Modifier.width(8.dp))
            }
            if (text.isNotEmpty()) Text(text.uppercase(), style = if (small) T.micro else T.label, color = fg, maxLines = 1)
        }
    }
}

/** Botón de ícono cuadrado con borde. */
@Composable
fun SquareIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.surface,
    tint: Color = LocalPalette.current.ink,
    size: Dp = 40.dp,
    shadow: Dp = 3.dp,
) {
    Pressable(modifier.size(size), color = color, shadow = shadow, onClick = onClick) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

/** Ícono plano sin borde (para barras). */
@Composable
fun BareIcon(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, tint: Color = LocalPalette.current.ink, active: Boolean = false) {
    val p = LocalPalette.current
    Box(
        Modifier
            .size(40.dp)
            .clip(BrutalShape)
            .then(if (active) Modifier.background(p.primary).border(Dims.borderThin, p.line, BrutalShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription, tint = if (active) p.onPrimary else tint, modifier = Modifier.size(22.dp)) }
}

/** Etiqueta tipo "sticker", levemente rotada. */
@Composable
fun Sticker(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    selected: Boolean = false,
    leading: String? = "#",
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    val p = LocalPalette.current
    Row(
        modifier
            .rotate(rotation)
            .hardShadow(p.shadow, if (selected) 0.dp else 2.dp)
            .clip(BrutalShape)
            .background(if (selected) color else p.surface)
            .border(Dims.borderThin, p.line, BrutalShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(Modifier.size(8.dp).background(color).border(1.dp, p.line))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = T.caption, color = if (selected) Color(0xFF383838) else p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (onRemove != null) {
            Spacer(Modifier.width(4.dp))
            Text("×", style = T.label, color = p.inkSoft, modifier = Modifier.clickable(onClick = onRemove))
        }
    }
}

/** Píldora de filtro (segmento). */
@Composable
fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null) {
    val p = LocalPalette.current
    Row(
        Modifier
            .hardShadow(p.shadow, if (selected) 0.dp else 2.dp)
            .clip(BrutalShape)
            .background(if (selected) p.ink else p.surface)
            .border(Dims.borderThin, p.line, BrutalShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) p.canvas else p.ink, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text.uppercase(), style = T.micro, color = if (selected) p.canvas else p.ink)
    }
}

/** Insignia amarilla de estado. */
@Composable
fun Badge(text: String, modifier: Modifier = Modifier, color: Color = LocalPalette.current.canary) {
    Text(
        text.uppercase(),
        style = T.micro,
        color = Color(0xFF383838),
        modifier = modifier
            .clip(BrutalShape)
            .background(color)
            .border(1.dp, LocalPalette.current.line, BrutalShape)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    val p = LocalPalette.current
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(p.primary).border(1.5.dp, p.line))
        Spacer(Modifier.width(8.dp))
        Text(title.uppercase(), style = T.label, color = p.ink, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action.uppercase() + " →", style = T.micro, color = p.inkSoft,
                modifier = Modifier.clip(BrutalShape).clickable(onClick = onAction).padding(6.dp),
            )
        }
    }
}

@Composable
fun LibretaTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().background(p.surface).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) BareIcon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", onBack)
            else Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                Text(title.uppercase(), style = T.label.copy(letterSpacing = T.label.letterSpacing), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, style = T.caption, color = p.inkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            actions()
        }
        Box(Modifier.fillMaxWidth().height(Dims.border).background(p.line))
    }
}

/** Franja marquesina amarilla. */
@Composable
fun Marquee(text: String, modifier: Modifier = Modifier, color: Color = LocalPalette.current.canary) {
    val p = LocalPalette.current
    Box(
        modifier
            .fillMaxWidth()
            .background(color)
            .drawBehind {
                drawLine(p.line, Offset(0f, 0f), Offset(size.width, 0f), 2.dp.toPx())
                drawLine(p.line, Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx())
            }
            .padding(vertical = 9.dp),
    ) {
        Text(
            (text + "   ✦   ").repeat(6),
            style = T.label,
            color = Color(0xFF383838),
            maxLines = 1,
            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, velocity = 36.dp),
        )
    }
}

@Composable
fun BrutalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    leading: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textStyle: TextStyle = T.body,
    minLines: Int = 1,
) {
    val p = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = minLines,
        textStyle = textStyle.copy(color = p.ink),
        cursorBrush = SolidColor(p.ink),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .hardShadow(p.shadow, if (focused) 3.dp else 0.dp)
                    .clip(BrutalShape)
                    .background(p.surface)
                    .border(if (focused) 2.5.dp else Dims.borderThin, if (focused) p.primary else p.line, BrutalShape)
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) {
                    Icon(leading, null, tint = p.inkSoft, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = textStyle, color = p.inkFaint)
                    inner()
                }
                trailing?.invoke()
            }
        },
    )
}

/** Casillero de verificación con estilo de crayón. */
@Composable
fun CrayonCheckbox(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, color: Color = LocalPalette.current.success, size: Dp = 20.dp, round: Boolean = false) {
    val p = LocalPalette.current
    val haptic = rememberHaptic()
    val shape = if (round) RoundedCornerShape(50) else BrutalShape
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(if (checked) color else p.surface)
            .border(Dims.border, p.line, shape)
            .clickable { haptic(); onToggle() },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Filled.Check, null, tint = Color(0xFF383838), modifier = Modifier.size(size * 0.75f))
    }
}

@Composable
fun ColorDots(selected: Int, onSelect: (Int) -> Unit, colors: List<Color>, allowNone: Boolean = false) {
    val p = LocalPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (allowNone) {
            Box(
                Modifier.size(30.dp).clip(BrutalShape).background(p.surface)
                    .border(if (selected == -1) 3.dp else 1.5.dp, p.line, BrutalShape).clickable { onSelect(-1) },
                contentAlignment = Alignment.Center,
            ) { Text("Ø", style = T.label, color = p.inkSoft) }
        }
        colors.forEachIndexed { i, c ->
            Box(
                Modifier.size(30.dp)
                    .hardShadow(p.shadow, if (selected == i) 0.dp else 2.dp)
                    .clip(BrutalShape).background(c)
                    .border(if (selected == i) 3.dp else 1.5.dp, p.line, BrutalShape)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { if (selected == i) Icon(Icons.Filled.Check, null, tint = Color(0xFF383838), modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier, dashed: Boolean = false) {
    val p = LocalPalette.current
    Box(
        modifier.fillMaxWidth().height(2.dp).drawBehind {
            if (dashed) {
                var x = 0f
                val d = 6.dp.toPx()
                while (x < size.width) {
                    drawLine(p.inkFaint, Offset(x, size.height / 2), Offset(minOf(x + d, size.width), size.height / 2), 1.5.dp.toPx())
                    x += d * 2
                }
            } else drawLine(p.line, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.5.dp.toPx())
        },
    )
}
