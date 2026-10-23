package com.libreta.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T

/** Medidor en arco: el progreso recorre el contorno del arco desde abajo a la izquierda. */
@Composable
fun ArchGauge(value: String, label: String, progress: Float, modifier: Modifier = Modifier, color: Color = LocalPalette.current.canary, caption: String? = null) {
    val p = LocalPalette.current
    val anim by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900), label = "gauge")
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 7.dp.toPx()
            val inset = sw / 2
            val w = size.width - sw
            val r = w / 2
            val path = Path().apply {
                moveTo(inset, size.height)
                lineTo(inset, inset + r)
                arcTo(Rect(inset, inset, inset + w, inset + w), 180f, 180f, false)
                lineTo(inset + w, size.height)
            }
            drawPath(path, p.line, style = Stroke(1.dp.toPx()))
            val pm = PathMeasure().apply { setPath(path, false) }
            val seg = Path()
            pm.getSegment(0f, pm.length * anim, seg, true)
            drawPath(seg, color, style = Stroke(sw, cap = StrokeCap.Butt))
        }
        Column(Modifier.padding(bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = T.display.copy(fontSize = T.display.fontSize * 0.95f), color = p.ink)
            Text(label.uppercase(), style = T.micro, color = p.inkSoft)
            if (caption != null) {
                Spacer(Modifier.height(4.dp))
                Text(caption, style = T.caption, color = p.inkFaint)
            }
        }
    }
}
