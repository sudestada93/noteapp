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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.libreta.app.R

/** Paleta "crayones": se usa para libretas, etiquetas y notas. */
val Crayons = listOf(
    Color(0xFF6FC2FF), // cielo
    Color(0xFFFFDE00), // canario
    Color(0xFFFF9538), // naranja
    Color(0xFFF38E84), // coral
    Color(0xFF38C1B0), // menta
    Color(0xFFB291DE), // lila
    Color(0xFFB3C419), // lima
    Color(0xFF7597EE), // vincapervinca
)
val CrayonNames = listOf("Cielo", "Canario", "Naranja", "Coral", "Menta", "Lila", "Lima", "Pervinca")

fun crayon(i: Int): Color = Crayons[((i % Crayons.size) + Crayons.size) % Crayons.size]

/** Versión pastel (para fondos) de un crayón. */
fun crayonWash(i: Int, dark: Boolean): Color {
    val c = crayon(i)
    return if (dark) androidx.compose.ui.graphics.lerp(Color(0xFF282725), c, 0.22f) else Color(
        red = c.red * 0.28f + 0.72f, green = c.green * 0.28f + 0.72f, blue = c.blue * 0.28f + 0.72f,
    )
}

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
)

val PaperPalette = Palette(
    dark = false,
    canvas = Color(0xFFF4EFEA),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF8F8F7),
    ink = Color(0xFF383838),
    inkSoft = Color(0xFF818181),
    inkFaint = Color(0xFFA1A1A1),
    line = Color(0xFF383838),
    shadow = Color(0xFF383838),
    primary = Color(0xFF6FC2FF),
    onPrimary = Color(0xFF383838),
    iceWash = Color(0xFFEBF9FF),
    canary = Color(0xFFFFDE00),
    orange = Color(0xFFFF9538),
    danger = Color(0xFFF38E84),
    success = Color(0xFF38C1B0),
    highlight = Color(0xFFFFEF7A),
    paperLine = Color(0x1F54B4DE),
)

val SlatePalette = Palette(
    dark = true,
    canvas = Color(0xFF1C1B1A),
    surface = Color(0xFF282725),
    surfaceAlt = Color(0xFF232220),
    ink = Color(0xFFF4EFEA),
    inkSoft = Color(0xFFB5AFA8),
    inkFaint = Color(0xFF8A847E),
    line = Color(0xFFE9E2D9),
    shadow = Color(0xFF6FC2FF),
    primary = Color(0xFF6FC2FF),
    onPrimary = Color(0xFF1C1B1A),
    iceWash = Color(0xFF1F3342),
    canary = Color(0xFFFFDE00),
    orange = Color(0xFFFF9538),
    danger = Color(0xFFF38E84),
    success = Color(0xFF38C1B0),
    highlight = Color(0x66FFDE00),
    paperLine = Color(0x1AF4EFEA),
)

val LocalPalette = staticCompositionLocalOf { PaperPalette }

val Mono = FontFamily(
    Font(R.font.jetbrains_mono_light, FontWeight.Light),
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
    Font(R.font.jetbrains_mono_extrabold, FontWeight.ExtraBold),
)
val Sans = FontFamily(
    Font(R.font.plex_sans_regular, FontWeight.Normal),
    Font(R.font.plex_sans_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.plex_sans_medium, FontWeight.Medium),
    Font(R.font.plex_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plex_sans_bold, FontWeight.Bold),
)
val Serif = FontFamily(
    Font(R.font.plex_serif_regular, FontWeight.Normal),
    Font(R.font.plex_serif_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.plex_serif_semibold, FontWeight.SemiBold),
    Font(R.font.plex_serif_semibold, FontWeight.Bold),
)

fun editorFamily(i: Int): FontFamily = when (i) { 1 -> Mono; 2 -> Serif; else -> Sans }

/** Escala tipográfica: mono para la interfaz, con tracking +0.02em. */
object T {
    private val base = TextStyle(fontFamily = Mono, letterSpacing = 0.02.em)
    val display = base.copy(fontSize = 34.sp, lineHeight = 36.sp, fontWeight = FontWeight.Light)
    val headingLg = base.copy(fontSize = 26.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold)
    val heading = base.copy(fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold)
    val headingSm = base.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
    val subheading = base.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
    val body = base.copy(fontSize = 14.sp, lineHeight = 21.sp)
    val bodySm = base.copy(fontSize = 13.sp, lineHeight = 19.sp)
    val label = base.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.06.em)
    val caption = base.copy(fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.04.em)
    val micro = base.copy(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.08.em)
}

object Dims {
    val radius = 2.dp
    val border = 2.dp
    val borderThin = 1.5.dp
    val shadow = 5.dp
    val gutter = 16.dp
}

@Composable
fun LibretaTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val p = if (dark) SlatePalette else PaperPalette
    val scheme = if (dark) darkColorScheme(
        primary = p.primary, onPrimary = p.onPrimary, background = p.canvas, onBackground = p.ink,
        surface = p.surface, onSurface = p.ink, surfaceVariant = p.surfaceAlt, onSurfaceVariant = p.inkSoft,
        secondary = p.canary, onSecondary = Color(0xFF383838), outline = p.line, error = p.danger,
        surfaceContainer = p.surface, surfaceContainerHigh = p.surface, surfaceContainerHighest = p.surfaceAlt,
        surfaceContainerLow = p.surface, primaryContainer = p.iceWash, onPrimaryContainer = p.ink,
        secondaryContainer = p.iceWash, onSecondaryContainer = p.ink, tertiary = p.orange,
    ) else lightColorScheme(
        primary = p.primary, onPrimary = p.onPrimary, background = p.canvas, onBackground = p.ink,
        surface = p.surface, onSurface = p.ink, surfaceVariant = p.surfaceAlt, onSurfaceVariant = p.inkSoft,
        secondary = p.canary, onSecondary = p.ink, outline = p.line, error = Color(0xFFD9534F),
        surfaceContainer = p.surface, surfaceContainerHigh = p.surface, surfaceContainerHighest = p.surfaceAlt,
        surfaceContainerLow = p.surface, primaryContainer = p.iceWash, onPrimaryContainer = p.ink,
        secondaryContainer = p.iceWash, onSecondaryContainer = p.ink, tertiary = p.orange,
    )
    val typography = MaterialTheme.typography.let { t ->
        t.copy(
            displayLarge = t.displayLarge.copy(fontFamily = Mono), displayMedium = t.displayMedium.copy(fontFamily = Mono),
            displaySmall = t.displaySmall.copy(fontFamily = Mono), headlineLarge = t.headlineLarge.copy(fontFamily = Mono),
            headlineMedium = t.headlineMedium.copy(fontFamily = Mono), headlineSmall = t.headlineSmall.copy(fontFamily = Mono),
            titleLarge = t.titleLarge.copy(fontFamily = Mono), titleMedium = t.titleMedium.copy(fontFamily = Mono),
            titleSmall = t.titleSmall.copy(fontFamily = Mono), bodyLarge = t.bodyLarge.copy(fontFamily = Mono),
            bodyMedium = t.bodyMedium.copy(fontFamily = Mono), bodySmall = t.bodySmall.copy(fontFamily = Mono),
            labelLarge = t.labelLarge.copy(fontFamily = Mono), labelMedium = t.labelMedium.copy(fontFamily = Mono),
            labelSmall = t.labelSmall.copy(fontFamily = Mono),
        )
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
