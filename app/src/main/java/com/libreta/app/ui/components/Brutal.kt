package com.libreta.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.libreta.app.ui.theme.Dims
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T

val BrutalShape = RoundedCornerShape(Dims.radius)
val PillShape = RoundedCornerShape(50)

/** Arco: rectángulo con la parte superior en medio punto (como una ventana o un vitral). */
val ArchShape: Shape = GenericShape { size, _ ->
    val r = size.width / 2
    moveTo(0f, size.height)
    lineTo(0f, r)
    arcTo(Rect(0f, 0f, size.width, size.width), 180f, 180f, false)
    lineTo(size.width, size.height)
    close()
}

/** Sin sombras: el papel no tiene espesor. Se conserva por compatibilidad. */
@Suppress("UNUSED_PARAMETER")
fun Modifier.hardShadow(color: Color, offset: Dp = 0.dp, progress: Float = 0f, radius: Dp = Dims.radius): Modifier = this

/** Línea fina inferior. */
fun Modifier.hairlineBottom(color: Color, inset: Dp = 0.dp): Modifier = this.drawBehind {
    drawLine(color, Offset(inset.toPx(), size.height), Offset(size.width - inset.toPx(), size.height), 1.dp.toPx())
}

@Composable
fun rememberHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) { { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) } }
}

/** Hoja de papel tocable: borde fino, sin sombra; al presionar se oscurece apenas. */
@Composable
fun Pressable(
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.surface,
    borderColor: Color = LocalPalette.current.line,
    border: Dp = Dims.border,
    shadow: Dp = 0.dp,
    shadowColor: Color = LocalPalette.current.shadow,
    shape: Shape = BrutalShape,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val p = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val t by animateFloatAsState(if (pressed) 1f else 0f, tween(120), label = "press")
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
            .graphicsLayer { val s = 1f - 0.015f * t; scaleX = s; scaleY = s }
            .clip(shape)
            .background(color)
            .drawBehind { if (t > 0f) drawRect(p.ink.copy(alpha = 0.05f * t)) }
            .border(BorderStroke(border, if (border == 0.dp) Color.Transparent else borderColor), shape)
            .then(clickMod),
    ) { content() }
}

@Composable
fun BrutalCard(
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.surface,
    shadow: Dp = 0.dp,
    padding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Pressable(modifier, color = color, onClick = onClick, onLongClick = onLongClick) {
        Column(Modifier.padding(padding), content = content)
    }
}

enum class BtnStyle { Primary, Secondary, Danger, Ghost, Canary }

/** Botón píldora: sepia relleno (principal) o contorno fino. Texto en versalitas espaciadas. */
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
    val filled = style == BtnStyle.Primary
    val fg = when (style) {
        BtnStyle.Primary -> p.onPrimary
        BtnStyle.Danger -> p.danger
        else -> p.ink
    }
    if (style == BtnStyle.Ghost) {
        Row(
            modifier.clip(PillShape).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) { Icon(icon, null, tint = p.inkSoft, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
            Text(text.uppercase(), style = T.label, color = p.inkSoft)
        }
        return
    }
    Pressable(
        modifier.graphicsLayer { alpha = if (enabled) 1f else 0.4f },
        color = if (filled) p.primary else Color.Transparent,
        borderColor = if (filled) p.primary else if (style == BtnStyle.Danger) p.danger else p.rule.copy(alpha = 0.45f),
        shape = PillShape,
        onClick = if (enabled) onClick else null,
    ) {
        Row(
            Modifier.padding(horizontal = if (small) 16.dp else 22.dp, vertical = if (small) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(if (small) 15.dp else 17.dp))
                if (text.isNotEmpty()) Spacer(Modifier.width(8.dp))
            }
            if (text.isNotEmpty()) Text(text.uppercase(), style = if (small) T.micro else T.label, color = fg, maxLines = 1)
        }
    }
}

/** Botón circular con contorno fino. */
@Composable
fun SquareIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Transparent,
    tint: Color = LocalPalette.current.ink,
    size: Dp = 42.dp,
    shadow: Dp = 0.dp,
) {
    Pressable(modifier.size(size), color = color, shape = CircleShape, borderColor = LocalPalette.current.line, onClick = onClick) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

/** Ícono plano para barras. */
@Composable
fun BareIcon(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, tint: Color = LocalPalette.current.ink, active: Boolean = false) {
    val p = LocalPalette.current
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .then(if (active) Modifier.background(p.iceWash) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(21.dp)) }
}

/** Etiqueta: píldora con contorno fino y punto de tinta. */
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
            .clip(PillShape)
            .background(if (selected) p.ink else Color.Transparent)
            .border(1.dp, if (selected) p.ink else p.line, PillShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(if (selected) p.canvas else color))
            Spacer(Modifier.width(7.dp))
        }
        Text(text, style = T.caption, color = if (selected) p.canvas else p.inkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (onRemove != null) {
            Spacer(Modifier.width(6.dp))
            Text("×", style = T.body, color = p.inkFaint, modifier = Modifier.clickable(onClick = onRemove))
        }
    }
}

/** Píldora de filtro. */
@Composable
fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null) {
    val p = LocalPalette.current
    Row(
        Modifier
            .clip(PillShape)
            .background(if (selected) p.ink else Color.Transparent)
            .border(1.dp, if (selected) p.ink else p.line, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) p.canvas else p.inkSoft, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text.uppercase(), style = T.micro, color = if (selected) p.canvas else p.inkSoft)
    }
}

@Composable
fun Badge(text: String, modifier: Modifier = Modifier, color: Color = LocalPalette.current.highlight) {
    Text(
        text.uppercase(),
        style = T.micro,
        color = LocalPalette.current.ink,
        modifier = modifier.clip(PillShape).background(color).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Título de sección: serif grande y liviana, con acción en versalitas. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    val p = LocalPalette.current
    Row(modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, style = T.headingLg.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Light), color = p.ink, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action.uppercase(), style = T.micro, color = p.inkSoft,
                modifier = Modifier.clip(PillShape).clickable(onClick = onAction).padding(horizontal = 6.dp, vertical = 8.dp),
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
    Column(Modifier.fillMaxWidth().background(p.canvas).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) BareIcon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", onBack)
            else Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                Text(title, style = T.headingSm, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle.uppercase(), style = T.micro, color = p.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            actions()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
    }
}

/** Renglón tranquilo entre dos líneas finas (antes era una marquesina). */
@Composable
fun Marquee(text: String, modifier: Modifier = Modifier, color: Color = Color.Transparent) {
    val p = LocalPalette.current
    Box(
        modifier
            .fillMaxWidth()
            .background(color)
            .drawBehind {
                drawLine(p.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                drawLine(p.line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
            .padding(vertical = 11.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = T.micro, color = p.inkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

/** Campo de texto subrayado, sin caja. */
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
    textStyle: TextStyle = T.body.copy(fontSize = T.subheading.fontSize),
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
                    .drawBehind {
                        val w = if (focused) 1.5.dp.toPx() else 1.dp.toPx()
                        drawLine(if (focused) p.ink else p.line, Offset(0f, size.height - w / 2), Offset(size.width, size.height - w / 2), w)
                    }
                    .padding(horizontal = 2.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) {
                    Icon(leading, null, tint = p.inkFaint, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = textStyle, color = p.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    inner()
                }
                trailing?.invoke()
            }
        },
    )
}

/** Casilla: círculo o cuadrado de trazo fino; marcada se rellena de tinta. */
@Composable
fun CrayonCheckbox(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, color: Color = LocalPalette.current.ink, size: Dp = 20.dp, round: Boolean = false) {
    val p = LocalPalette.current
    val haptic = rememberHaptic()
    val shape = if (round) CircleShape else RoundedCornerShape(5.dp)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(if (checked) color else Color.Transparent)
            .border(1.3.dp, if (checked) color else p.inkSoft, shape)
            .clickable { haptic(); onToggle() },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Filled.Check, null, tint = p.canvas, modifier = Modifier.size(size * 0.7f))
    }
}

@Composable
fun ColorDots(selected: Int, onSelect: (Int) -> Unit, colors: List<Color>, allowNone: Boolean = false) {
    val p = LocalPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (allowNone) {
            Box(
                Modifier.size(30.dp).clip(CircleShape)
                    .border(if (selected == -1) 2.dp else 1.dp, if (selected == -1) p.ink else p.line, CircleShape).clickable { onSelect(-1) },
                contentAlignment = Alignment.Center,
            ) { Text("Ø", style = T.caption, color = p.inkSoft) }
        }
        colors.forEachIndexed { i, c ->
            Box(
                Modifier.size(30.dp)
                    .border(if (selected == i) 2.dp else 0.dp, if (selected == i) p.ink else Color.Transparent, CircleShape)
                    .padding(4.dp).clip(CircleShape).background(c)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { if (selected == i) Icon(Icons.Filled.Check, null, tint = Color(0xFFFBF8F1), modifier = Modifier.size(13.dp)) }
        }
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier, dashed: Boolean = false) {
    val p = LocalPalette.current
    Box(modifier.fillMaxWidth().height(1.dp).background(p.line))
}

/** Fila de "ficha" estilo índice: etiqueta en versalitas y valor, separadas por línea fina. */
@Composable
fun LedgerRow(label: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .hairlineBottom(p.line)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label.uppercase(), style = T.micro, color = p.inkFaint, modifier = Modifier.width(96.dp))
        content()
    }
}
