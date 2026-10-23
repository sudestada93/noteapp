package com.libreta.app.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import com.libreta.app.model.Inline
import com.libreta.app.ui.theme.Mono
import com.libreta.app.ui.theme.Palette

/**
 * Renderiza el markdown en línea. En el bloque con foco los marcadores se ven tenues;
 * en el resto se ocultan para una lectura limpia (estilo "vista previa en vivo").
 */
class InlineTransformation(
    private val showMarkers: Boolean,
    private val palette: Palette,
    private val find: String,
    private val plain: Boolean = false,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val spans = if (plain) emptyList() else Inline.parse(raw)
        val n = raw.length
        val hidden = BooleanArray(n)
        val styles = mutableListOf<Triple<SpanStyle, Int, Int>>()
        val markerStyle = SpanStyle(color = palette.inkFaint, fontWeight = FontWeight.Normal, fontStyle = FontStyle.Normal, background = Color.Transparent)
        val link = if (palette.dark) palette.primary else Color(0xFF1F7FCB)

        for (s in spans) {
            val style = when (s.kind) {
                Inline.Kind.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                Inline.Kind.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                Inline.Kind.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
                Inline.Kind.STRIKE -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                Inline.Kind.HIGHLIGHT -> SpanStyle(background = palette.highlight)
                Inline.Kind.CODE -> SpanStyle(fontFamily = Mono, fontSize = 0.88.em, background = palette.iceWash, color = palette.ink)
                Inline.Kind.LINK, Inline.Kind.URL -> SpanStyle(color = link, textDecoration = TextDecoration.Underline)
            }
            styles += Triple(style, s.contentStart, s.contentEnd)
            if (s.kind != Inline.Kind.URL) {
                if (showMarkers) {
                    styles += Triple(markerStyle, s.start, s.contentStart)
                    styles += Triple(markerStyle, s.contentEnd, s.end)
                } else {
                    for (i in s.start until s.contentStart) hidden[i] = true
                    for (i in s.contentEnd until s.end) hidden[i] = true
                }
            }
        }
        if (find.isNotBlank()) {
            var idx = raw.indexOf(find, ignoreCase = true)
            while (idx >= 0) {
                styles += Triple(SpanStyle(background = palette.canary, color = Color(0xFF383838)), idx, idx + find.length)
                idx = raw.indexOf(find, idx + find.length, ignoreCase = true)
            }
        }

        if (showMarkers || hidden.none { it }) {
            val out = buildAnnotatedString {
                append(raw)
                styles.forEach { (st, a, b) -> if (a < b) addStyle(st, a, b) }
            }
            return TransformedText(out, OffsetMapping.Identity)
        }

        val o2t = IntArray(n + 1)
        var t = 0
        for (o in 0..n) {
            o2t[o] = t
            if (o < n && !hidden[o]) t++
        }
        val m = t
        val t2o = IntArray(m + 1)
        for (o in 0..n) t2o[o2t[o]] = o
        val sb = StringBuilder(m)
        for (o in 0 until n) if (!hidden[o]) sb.append(raw[o])
        val out = buildAnnotatedString {
            append(sb.toString())
            styles.forEach { (st, a, b) ->
                val ta = o2t[a.coerceIn(0, n)]
                val tb = o2t[b.coerceIn(0, n)]
                if (ta < tb) addStyle(st, ta, tb)
            }
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = o2t[offset.coerceIn(0, n)]
            override fun transformedToOriginal(offset: Int): Int = t2o[offset.coerceIn(0, m)]
        }
        return TransformedText(out, mapping)
    }

    override fun equals(other: Any?): Boolean =
        other is InlineTransformation && other.showMarkers == showMarkers && other.palette == palette && other.find == find && other.plain == plain

    override fun hashCode(): Int = (showMarkers.hashCode() * 31 + palette.hashCode()) * 31 + find.hashCode() + plain.hashCode()
}
