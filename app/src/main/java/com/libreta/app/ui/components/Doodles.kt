package com.libreta.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.libreta.app.R
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T

/** Grabados en tinta azul (dibujados con sombreado de líneas, ver /art). */
enum class Art(val res: Int) {
    BOOKS(R.drawable.art_books),
    INKWELL(R.drawable.art_inkwell),
    WATCH(R.drawable.art_watch),
    KEY(R.drawable.art_key),
    MAGNIFIER(R.drawable.art_magnifier),
    OPEN_BOOK(R.drawable.art_open_book),
    LAUREL(R.drawable.art_laurel),
}

@Composable
fun Engraving(art: Art, modifier: Modifier = Modifier, tint: Color = LocalPalette.current.art) {
    Image(
        painterResource(art.res), contentDescription = null, modifier = modifier,
        contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(tint),
    )
}

enum class Doodle(val art: Art) {
    NOTES(Art.OPEN_BOOK), TASKS(Art.WATCH), SEARCH(Art.MAGNIFIER), TRASH(Art.LAUREL), TAGS(Art.KEY),
    CALENDAR(Art.WATCH), BELL(Art.WATCH), STAR(Art.LAUREL), LOCK(Art.KEY), TEMPLATE(Art.BOOKS),
    NOTEBOOK(Art.INKWELL), HISTORY(Art.WATCH)
}

@Composable
fun DoodleArt(kind: Doodle, modifier: Modifier = Modifier, size: Dp = 150.dp) {
    Engraving(kind.art, modifier.size(size))
}

@Composable
fun EmptyState(doodle: Doodle, title: String, subtitle: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val p = LocalPalette.current
    Column(
        modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Engraving(doodle.art, Modifier.widthIn(max = 180.dp).height(130.dp))
        Spacer(Modifier.height(22.dp))
        Text(title, style = T.heading, color = p.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = T.bodySm, color = p.inkSoft, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(22.dp))
            action()
        }
    }
}
