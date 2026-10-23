package com.libreta.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class Doodle { NOTES, TASKS, SEARCH, TRASH, TAGS, CALENDAR, BELL, STAR, LOCK, TEMPLATE, NOTEBOOK, HISTORY }

/** Trazo "a mano": puntos con un leve temblor determinista. */
private fun sketch(points: List<Offset>, closed: Boolean, seed: Int, amp: Float): Path {
    val r = Random(seed)
    val pts = mutableListOf<Offset>()
    val src = if (closed) points + points.first() else points
    for (i in 0 until src.size - 1) {
        val a = src[i]
        val b = src[i + 1]
        val steps = 3
        for (s in 0 until steps) {
            val t = s / steps.toFloat()
            pts += Offset(a.x + (b.x - a.x) * t + (r.nextFloat() - 0.5f) * amp, a.y + (b.y - a.y) * t + (r.nextFloat() - 0.5f) * amp)
        }
    }
    pts += src.last()
    val path = Path()
    path.moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size - 1) {
        val mid = Offset((pts[i].x + pts[i + 1].x) / 2, (pts[i].y + pts[i + 1].y) / 2)
        path.quadraticTo(pts[i].x, pts[i].y, mid.x, mid.y)
    }
    path.lineTo(pts.last().x, pts.last().y)
    if (closed) path.close()
    return path
}

private fun circlePts(cx: Float, cy: Float, r: Float, n: Int = 14): List<Offset> =
    (0 until n).map { val a = it / n.toFloat() * 2 * Math.PI; Offset(cx + r * cos(a).toFloat(), cy + r * sin(a).toFloat()) }

private fun rectPts(x: Float, y: Float, w: Float, h: Float) = listOf(Offset(x, y), Offset(x + w, y), Offset(x + w, y + h), Offset(x, y + h))

private class Pen(val scope: DrawScope, val s: Float, val ink: Color, val stroke: Float) {
    fun p(x: Float, y: Float) = Offset(x * s, y * s)
    fun shape(pts: List<Offset>, fill: Color?, seed: Int, closed: Boolean = true) {
        val scaled = pts.map { p(it.x, it.y) }
        if (fill != null) {
            // relleno "mal registrado" estilo risografía
            scope.translate(left = -3.5f * s / 1.4f, top = 3f * s / 1.4f) { scope.drawPath(sketch(scaled, closed, seed + 7, 1.2f * s), fill) }
        }
        scope.drawPath(sketch(scaled, closed, seed, 1.4f * s), ink, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    fun line(vararg xy: Float, seed: Int = 1) {
        val pts = xy.toList().chunked(2).map { Offset(it[0], it[1]) }
        val scaled = pts.map { p(it.x, it.y) }
        scope.drawPath(sketch(scaled, false, seed, 1.0f * s), ink, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun DoodleArt(kind: Doodle, modifier: Modifier = Modifier, size: Dp = 150.dp) {
    val pal = LocalPalette.current
    val inf = rememberInfiniteTransition(label = "doodle")
    val bob by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse), label = "bob")
    val sky = Color(0xFF54B4DE)
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 100f
        val pen = Pen(this, s, pal.ink, 2.4.dp.toPx())
        val skyPen = Pen(this, s, sky, 1.8.dp.toPx())
        // nubes y destellos decorativos
        translate(top = -bob * 3 * s) {
            skyPen.shape(circlePts(14f, 19f, 6f, 10), null, 11)
            skyPen.shape(circlePts(23f, 16f, 8f, 10), null, 12)
            skyPen.line(80f, 10f, 80f, 20f, seed = 3)
            skyPen.line(75f, 15f, 85f, 15f, seed = 4)
        }
        when (kind) {
            Doodle.NOTES, Doodle.NOTEBOOK -> {
                pen.shape(rectPts(28f, 26f, 48f, 60f), pal.primary, 1)
                pen.shape(rectPts(28f, 26f, 10f, 60f), pal.orange, 2)
                for (i in 0..3) pen.line(45f, 40f + i * 10, 68f - (if (i == 3) 10 else 0), 40f + i * 10, seed = 10 + i)
                pen.shape(listOf(Offset(70f, 70f), Offset(88f, 52f), Offset(93f, 57f), Offset(75f, 75f), Offset(68f, 77f)), pal.canary, 5)
            }
            Doodle.TASKS -> {
                for (i in 0..2) {
                    pen.shape(rectPts(22f, 26f + i * 22, 14f, 14f), if (i < 2) pal.success else null, 20 + i)
                    pen.line(44f, 33f + i * 22, 80f - i * 8, 33f + i * 22, seed = 30 + i)
                    if (i < 2) pen.line(24f, 33f + i * 22, 29f, 38f + i * 22, 40f, 22f + i * 22, seed = 40 + i)
                }
            }
            Doodle.SEARCH -> {
                pen.shape(circlePts(44f, 44f, 22f), pal.iceWash, 50)
                pen.line(60f, 60f, 82f, 82f, seed = 51)
                pen.line(62f, 64f, 80f, 86f, seed = 52)
                pen.line(34f, 36f, 40f, 30f, seed = 53)
            }
            Doodle.TRASH -> {
                pen.shape(listOf(Offset(30f, 34f), Offset(70f, 34f), Offset(65f, 86f), Offset(35f, 86f)), pal.danger, 60)
                pen.shape(rectPts(25f, 26f, 50f, 8f), null, 61)
                pen.shape(rectPts(42f, 18f, 16f, 8f), null, 62)
                for (i in 0..2) pen.line(42f + i * 8, 44f, 42f + i * 8, 76f, seed = 63 + i)
            }
            Doodle.TAGS -> {
                pen.shape(listOf(Offset(22f, 30f), Offset(58f, 30f), Offset(80f, 52f), Offset(58f, 74f), Offset(22f, 74f)), pal.canary, 70)
                pen.shape(circlePts(34f, 52f, 5f, 8), pal.surface, 71)
                pen.line(40f, 52f, 64f, 52f, seed = 72)
            }
            Doodle.CALENDAR -> {
                pen.shape(rectPts(22f, 28f, 58f, 54f), pal.surface, 80)
                pen.shape(rectPts(22f, 28f, 58f, 13f), pal.orange, 81)
                pen.line(34f, 22f, 34f, 32f, seed = 82)
                pen.line(68f, 22f, 68f, 32f, seed = 83)
                for (r in 0..2) for (c in 0..3) {
                    if (r == 1 && c == 2) pen.shape(rectPts(30f + c * 12, 48f + r * 11, 8f, 7f), pal.primary, 84)
                    else pen.line(31f + c * 12, 52f + r * 11, 36f + c * 12, 52f + r * 11, seed = 85 + r * 4 + c)
                }
            }
            Doodle.BELL -> {
                pen.shape(listOf(Offset(50f, 22f), Offset(68f, 36f), Offset(70f, 62f), Offset(80f, 72f), Offset(20f, 72f), Offset(30f, 62f), Offset(32f, 36f)), pal.canary, 90)
                pen.shape(circlePts(50f, 80f, 6f, 8), pal.orange, 91)
                pen.line(14f, 40f, 20f, 44f, seed = 92)
                pen.line(86f, 40f, 80f, 44f, seed = 93)
            }
            Doodle.STAR -> {
                val pts = (0 until 10).map { i ->
                    val r = if (i % 2 == 0) 34f else 15f
                    val a = -Math.PI / 2 + i * Math.PI / 5
                    Offset(50f + r * cos(a).toFloat(), 54f + r * sin(a).toFloat())
                }
                pen.shape(pts, pal.canary, 100)
            }
            Doodle.LOCK -> {
                pen.shape(rectPts(28f, 46f, 44f, 38f), pal.primary, 110)
                pen.line(36f, 46f, 36f, 34f, 42f, 24f, 58f, 24f, 64f, 34f, 64f, 46f, seed = 111)
                pen.shape(circlePts(50f, 62f, 5f, 8), pal.ink, 112)
            }
            Doodle.TEMPLATE -> {
                pen.shape(rectPts(34f, 20f, 44f, 56f), pal.surface, 120)
                pen.shape(rectPts(24f, 30f, 44f, 56f), pal.success, 121)
                for (i in 0..2) pen.line(32f, 44f + i * 10, 58f, 44f + i * 10, seed = 122 + i)
            }
            Doodle.HISTORY -> {
                pen.shape(circlePts(50f, 52f, 30f), pal.iceWash, 130)
                pen.line(50f, 34f, 50f, 52f, 64f, 60f, seed = 131)
                pen.line(16f, 40f, 22f, 50f, 30f, 42f, seed = 132)
            }
        }
    }
}

@Composable
fun EmptyState(doodle: Doodle, title: String, subtitle: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val p = LocalPalette.current
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        DoodleArt(doodle)
        Spacer(Modifier.height(16.dp))
        Text(title.uppercase(), style = T.headingSm, color = p.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = T.bodySm, color = p.inkSoft, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}
