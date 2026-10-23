package com.libreta.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.libreta.app.R

/** Tintas: colores sobrios para libretas, etiquetas y notas (como plumas de distintas tintas). */
val Crayons = listOf(
    Color(0xFF6B3E1F), // sepia
    Color(0xFF2E3A7A), // violeta tinta
    Color(0xFF2E5A3A), // bosque
    Color(0xFF7A1F3D), // borravino
    Color(0xFFC99A2E), // mostaza
    Color(0xFF3E6AA8), // azul grabado
    Color(0xFFA9552F), // terracota
    Color(0xFF6E6C34), // oliva
)
val CrayonNames = listOf("Sepia", "Violeta", "Bosque", "Borravino", "Mostaza", "Azul", "Terracota", "Oliva")

fun crayon(i: Int): Color = Crayons[((i % Crayons.size) + Crayons.size) % Crayons.size]

/** Tinte muy suave de una tinta sobre el papel. */
fun crayonWash(i: Int, dark: Boolean): Color =
    if (dark) lerp(Color(0xFF1E1B16), crayon(i), 0.22f) else lerp(Color(0xFFFBF8F1), crayon(i), 0.10f)

@Immutable
data class Palette(
    val dark: Boolean,
    val canvas: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val line: Color,
    val shadow: Color,
    val primary: Color,
    val onPrimary: Color,
    val iceWash: Color,
    val canary: Color,
    val orange: Color,
    val danger: Color,
    val success: Color,
    val highlight: Color,
    val paperLine: Color,
    val art: Color,
    val violet: Color,
    val onViolet: Color,
    val rule: Color,
)

/** Papel marfil, tinta cálida, sepia para la acción, mostaza como acento. */
val PaperPalette = Palette(
    dark = false,
    canvas = Color(0xFFF5F0E6),
    surface = Color(0xFFFBF8F1),
    surfaceAlt = Color(0xFFEFE8DB),
    ink = Color(0xFF1F1B16),
    inkSoft = Color(0xFF5E574D),
    inkFaint = Color(0xFF9A9286),
    line = Color(0xFFDCD2C1),
    shadow = Color(0x00000000),
    primary = Color(0xFF4A2C17),
    onPrimary = Color(0xFFFBF8F1),
    iceWash = Color(0xFFEDE5D5),
    canary = Color(0xFFE2B33F),
    orange = Color(0xFFA9552F),
    danger = Color(0xFF8E2A3B),
    success = Color(0xFF2E5A3A),
    highlight = Color(0xFFF1D78E),
    paperLine = Color(0xFFE6DCCB),
    art = Color(0xFF2F55A4),
    violet = Color(0xFF242D64),
    onViolet = Color(0xFFF5F0E6),
    rule = Color(0xFF3A332B),
)

/** Noche: papel oscuro como tapa de libro, tinta marfil. */
val SlatePalette = Palette(
    dark = true,
    canvas = Color(0xFF15130F),
    surface = Color(0xFF1E1B16),
    surfaceAlt = Color(0xFF26221C),
    ink = Color(0xFFEFE8DA),
    inkSoft = Color(0xFFB8AF9F),
    inkFaint = Color(0xFF7F776A),
    line = Color(0xFF39332A),
    shadow = Color(0x00000000),
    primary = Color(0xFFE9DFC9),
    onPrimary = Color(0xFF1F1B16),
    iceWash = Color(0xFF2A251D),
    canary = Color(0xFFE2B33F),
    orange = Color(0xFFD08A5E),
    danger = Color(0xFFD9798A),
    success = Color(0xFF8DBB95),
    highlight = Color(0x66E2B33F),
    paperLine = Color(0xFF2A251E),
    art = Color(0xFF9DB4E6),
    violet = Color(0xFF2B3470),
    onViolet = Color(0xFFF5F0E6),
    rule = Color(0xFFCFC5B2),
)

val LocalPalette = staticCompositionLocalOf { PaperPalette }

/** Serif de exhibición: títulos, citas, números grandes. */
val Display = FontFamily(
    Font(R.font.cormorant_light, FontWeight.Light),
    Font(R.font.cormorant_regular, FontWeight.Normal),
    Font(R.font.cormorant_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.cormorant_medium, FontWeight.Medium),
    Font(R.font.cormorant_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_semibold, FontWeight.Bold),
)

/** Sans de interfaz, tranquila. */
val Ui = FontFamily(
    Font(R.font.raleway, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.raleway, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.raleway, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.raleway, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

val Mono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_medium, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_medium, FontWeight.Bold),
)
val Sans = FontFamily(
    Font(R.font.plex_sans_regular, FontWeight.Normal),
    Font(R.font.plex_sans_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.plex_sans_medium, FontWeight.Medium),
    Font(R.font.plex_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plex_sans_semibold, FontWeight.Bold),
)
val Serif = FontFamily(
    Font(R.font.plex_serif_regular, FontWeight.Normal),
    Font(R.font.plex_serif_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.plex_serif_semibold, FontWeight.SemiBold),
    Font(R.font.plex_serif_semibold, FontWeight.Bold),
)

/** Fuente del cuerpo de las notas: 0 serif (predeterminada), 1 sans, 2 mono. */
fun editorFamily(i: Int): FontFamily = when (i) { 1 -> Sans; 2 -> Mono; else -> Serif }

object T {
    private val serif = TextStyle(fontFamily = Display)
    private val ui = TextStyle(fontFamily = Ui, letterSpacing = 0.01.em)
    val display = serif.copy(fontSize = 42.sp, lineHeight = 44.sp, fontWeight = FontWeight.Light)
    val headingLg = serif.copy(fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Normal)
    val heading = serif.copy(fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.Medium)
    val headingSm = serif.copy(fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium)
    val quote = serif.copy(fontSize = 30.sp, lineHeight = 36.sp, fontStyle = FontStyle.Normal, fontWeight = FontWeight.Normal)
    val subheading = ui.copy(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
    val body = ui.copy(fontSize = 14.sp, lineHeight = 21.sp)
    val bodySm = ui.copy(fontSize = 13.sp, lineHeight = 19.sp)
    val label = ui.copy(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.16.em)
    val caption = ui.copy(fontSize = 12.sp, lineHeight = 16.sp)
    val micro = ui.copy(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.14.em)
}

object Dims {
    val radius = 12.dp
    val border = 1.dp
    val borderThin = 1.dp
    val shadow = 0.dp
    val gutter = 22.dp
}

@Composable
fun LibretaTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val p = if (dark) SlatePalette else PaperPalette
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = p.primary, onPrimary = p.onPrimary, background = p.canvas, onBackground = p.ink,
        surface = p.surface, onSurface = p.ink, surfaceVariant = p.surfaceAlt, onSurfaceVariant = p.inkSoft,
        secondary = p.canary, onSecondary = Color(0xFF1F1B16), outline = p.line, outlineVariant = p.line, error = p.danger,
        surfaceContainer = p.surface, surfaceContainerHigh = p.surface, surfaceContainerHighest = p.surfaceAlt,
        surfaceContainerLow = p.surface, surfaceContainerLowest = p.surface, primaryContainer = p.iceWash, onPrimaryContainer = p.ink,
        secondaryContainer = p.iceWash, onSecondaryContainer = p.ink, tertiary = p.canary, surfaceTint = Color.Transparent,
    )
    val typography = MaterialTheme.typography.let { t ->
        t.copy(
            displayLarge = t.displayLarge.copy(fontFamily = Display), displayMedium = t.displayMedium.copy(fontFamily = Display),
            displaySmall = t.displaySmall.copy(fontFamily = Display), headlineLarge = t.headlineLarge.copy(fontFamily = Display),
            headlineMedium = t.headlineMedium.copy(fontFamily = Display), headlineSmall = t.headlineSmall.copy(fontFamily = Display),
            titleLarge = t.titleLarge.copy(fontFamily = Display), titleMedium = t.titleMedium.copy(fontFamily = Ui),
            titleSmall = t.titleSmall.copy(fontFamily = Ui), bodyLarge = t.bodyLarge.copy(fontFamily = Ui),
            bodyMedium = t.bodyMedium.copy(fontFamily = Ui), bodySmall = t.bodySmall.copy(fontFamily = Ui),
            labelLarge = t.labelLarge.copy(fontFamily = Ui), labelMedium = t.labelMedium.copy(fontFamily = Ui),
            labelSmall = t.labelSmall.copy(fontFamily = Ui),
        )
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
