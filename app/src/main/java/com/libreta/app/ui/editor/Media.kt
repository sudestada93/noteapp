package com.libreta.app.ui.editor

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.libreta.app.data.Repository
import com.libreta.app.model.Block
import com.libreta.app.model.BlockType
import com.libreta.app.ui.components.BrutalButton
import com.libreta.app.ui.components.BrutalShape
import com.libreta.app.ui.components.BtnStyle
import com.libreta.app.ui.components.SquareIconButton
import com.libreta.app.ui.components.hardShadow
import com.libreta.app.ui.theme.Crayons
import com.libreta.app.ui.theme.Dims
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T
import com.libreta.app.util.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

fun fileUri(context: Context, file: File) = FileProvider.getUriForFile(context, context.packageName + ".files", file)

fun openFile(context: Context, file: File, mime: String?) {
    try {
        val uri = fileUri(context, file)
        val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(i, "Abrir con"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No hay una app para abrir este archivo", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo abrir el archivo", Toast.LENGTH_SHORT).show()
    }
}

fun shareFile(context: Context, file: File, mime: String?) {
    try {
        val uri = fileUri(context, file)
        val i = Intent(Intent.ACTION_SEND).setType(mime ?: "*/*").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(i, "Compartir"))
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo compartir", Toast.LENGTH_SHORT).show()
    }
}

// ---------------- Audio ----------------

@Composable
fun Waveform(samples: List<Int>, progress: Float, modifier: Modifier = Modifier, bars: Int = 48) {
    val p = LocalPalette.current
    Canvas(modifier) {
        val data = if (samples.isEmpty()) List(bars) { 20 + ((it * 37) % 60) } else samples
        val n = data.size
        val w = size.width / n
        data.forEachIndexed { i, v ->
            val h = (v.coerceIn(4, 100) / 100f) * size.height
            val played = i / n.toFloat() <= progress
            drawRoundRect(
                if (played) p.ink else p.inkFaint,
                topLeft = Offset(i * w + w * 0.2f, (size.height - h) / 2),
                size = Size(w * 0.6f, h),
                cornerRadius = CornerRadius(1f),
            )
        }
    }
}

@Composable
fun AudioPlayer(file: File, durationMs: Long, waveform: List<Int>, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    var playing by remember { mutableStateOf(false) }
    var pos by remember { mutableLongStateOf(0L) }
    var total by remember { mutableLongStateOf(durationMs) }
    val player = remember(file) { MediaPlayer() }
    var prepared by remember { mutableStateOf(false) }
    DisposableEffect(file) {
        onDispose { runCatching { player.release() } }
    }
    LaunchedEffect(playing) {
        while (playing && isActive) {
            pos = runCatching { player.currentPosition.toLong() }.getOrDefault(pos)
            delay(80)
        }
    }
    fun toggle() {
        try {
            if (!prepared) {
                player.setDataSource(file.absolutePath)
                player.prepare()
                total = player.duration.toLong()
                player.setOnCompletionListener { playing = false; pos = 0 }
                prepared = true
            }
            if (playing) { player.pause(); playing = false } else { player.start(); playing = true }
        } catch (e: Exception) {
            playing = false
        }
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).hardShadow(p.shadow, 3.dp).clip(BrutalShape).background(p.primary)
                .border(Dims.border, p.line, BrutalShape).clickable { toggle() },
            contentAlignment = Alignment.Center,
        ) { Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Reproducir", tint = p.onPrimary) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Waveform(
                waveform, if (total > 0) pos / total.toFloat() else 0f,
                Modifier.fillMaxWidth().height(32.dp).pointerInput(total) {
                    detectTapGestures { o ->
                        if (prepared && total > 0) {
                            val target = (o.x / size.width * total).toLong()
                            player.seekTo(target.toInt()); pos = target
                        }
                    }
                },
            )
            Text("${Fmt.duration(pos)} / ${Fmt.duration(total)}", style = T.caption, color = p.inkSoft)
        }
    }
}

/** Grabadora con forma de onda en vivo. */
@Composable
fun RecorderDialog(repo: Repository, onDismiss: () -> Unit, onDone: (Block) -> Unit) {
    val context = LocalContext.current
    val p = LocalPalette.current
    val (path, file) = remember { repo.newAttachmentFile("m4a") }
    val levels = remember { mutableStateListOf<Int>() }
    var elapsed by remember { mutableLongStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }
    val recorder = remember {
        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
        } catch (e: Exception) {
            error = "No se pudo acceder al micrófono"
        }
        r
    }
    var finished by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            if (!finished) {
                runCatching { recorder.stop() }
                runCatching { recorder.release() }
                file.delete()
            }
        }
    }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (isActive && error == null) {
            elapsed = System.currentTimeMillis() - start
            val amp = runCatching { recorder.maxAmplitude }.getOrDefault(0)
            levels.add((Math.sqrt(amp / 32767.0) * 100).toInt().coerceIn(4, 100))
            delay(100)
        }
    }
    fun stop() {
        finished = true
        runCatching { recorder.stop() }
        runCatching { recorder.release() }
        val all = levels.toList()
        val bars = 48
        val wave = if (all.isEmpty()) emptyList() else List(bars) { i ->
            val a = i * all.size / bars
            val b = ((i + 1) * all.size / bars).coerceAtLeast(a + 1).coerceAtMost(all.size)
            all.subList(a.coerceAtMost(all.size - 1), b).maxOrNull() ?: 4
        }
        onDone(Block(type = BlockType.AUDIO, path = path, name = "Grabación " + Fmt.time(System.currentTimeMillis()), mime = "audio/mp4", size = file.length(), durationMs = elapsed, waveform = wave))
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().hardShadow(p.shadow, 6.dp).clip(BrutalShape).background(p.surface).border(Dims.border, p.line, BrutalShape),
        ) {
            Row(Modifier.fillMaxWidth().background(p.danger).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF383838)))
                Spacer(Modifier.width(8.dp))
                Text("GRABANDO", style = T.label, color = Color(0xFF383838))
            }
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Fmt.duration(elapsed), style = T.display, color = p.ink)
                Spacer(Modifier.height(12.dp))
                Waveform(levels.takeLast(48), 1f, Modifier.fillMaxWidth().height(56.dp))
                if (error != null) Text(error!!, style = T.bodySm, color = p.danger)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrutalButton("Descartar", onDismiss, style = BtnStyle.Secondary, icon = Icons.Filled.Delete)
                    BrutalButton("Guardar", { stop() }, icon = Icons.Filled.Stop, enabled = error == null)
                }
            }
        }
    }
}

// ---------------- Boceto ----------------

private class Stroke2(val points: List<Offset>, val color: Color, val width: Float)

@Composable
fun SketchDialog(repo: Repository, onDismiss: () -> Unit, onDone: (Block) -> Unit) {
    val p = LocalPalette.current
    val strokes = remember { mutableStateListOf<Stroke2>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val inks = remember { listOf(Color(0xFF383838)) + Crayons }
    var color by remember { mutableStateOf(inks[0]) }
    var width by remember { mutableFloatStateOf(6f) }
    var eraser by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val paper = Color.White

    fun save() {
        if (canvasSize.width == 0) return
        val bmp = Bitmap.createBitmap(canvasSize.width, canvasSize.height, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        c.drawColor(paper.toArgb())
        strokes.forEach { s ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true; style = android.graphics.Paint.Style.STROKE; strokeWidth = s.width
                strokeCap = android.graphics.Paint.Cap.ROUND; strokeJoin = android.graphics.Paint.Join.ROUND; this.color = s.color.toArgb()
            }
            val path = android.graphics.Path()
            s.points.firstOrNull()?.let { path.moveTo(it.x, it.y) }
            s.points.drop(1).forEach { path.lineTo(it.x, it.y) }
            if (s.points.size == 1) path.lineTo(s.points[0].x + 0.1f, s.points[0].y)
            c.drawPath(path, paint)
        }
        val (path, file) = repo.newAttachmentFile("png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        onDone(Block(type = BlockType.IMAGE, path = path, name = "Boceto.png", mime = "image/png", size = file.length(), sketch = true))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(p.canvas).statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                SquareIconButton(Icons.Filled.Close, "Cerrar", onDismiss)
                Spacer(Modifier.width(12.dp))
                Text("BOCETO", style = T.label, color = p.ink, modifier = Modifier.weight(1f))
                SquareIconButton(Icons.AutoMirrored.Filled.Undo, "Deshacer", { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) })
                Spacer(Modifier.width(10.dp))
                BrutalButton("Insertar", { save() }, small = true)
            }
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)
                    .hardShadow(p.shadow, 5.dp).clip(BrutalShape).background(paper).border(Dims.border, p.line, BrutalShape),
            ) {
                Canvas(
                    Modifier.fillMaxSize()
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(color, width, eraser) {
                            detectDragGestures(
                                onDragStart = { current = listOf(it) },
                                onDrag = { change, _ -> current = current + change.position },
                                onDragEnd = {
                                    strokes.add(Stroke2(current, if (eraser) paper else color, if (eraser) width * 4 else width))
                                    current = emptyList()
                                },
                            )
                        }
                        .pointerInput(color, width, eraser) {
                            detectTapGestures { strokes.add(Stroke2(listOf(it), if (eraser) paper else color, if (eraser) width * 4 else width)) }
                        },
                ) {
                    // puntos de fondo
                    val step = 22.dp.toPx()
                    var y = step
                    while (y < size.height) {
                        var x = step
                        while (x < size.width) { drawCircle(Color(0x22383838), 1.4f, Offset(x, y)); x += step }
                        y += step
                    }
                    fun drawS(pts: List<Offset>, c: Color, w: Float) {
                        if (pts.isEmpty()) return
                        if (pts.size == 1) { drawCircle(c, w / 2, pts[0]); return }
                        val path = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
                        drawPath(path, c, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    strokes.forEach { drawS(it.points, it.color, it.width) }
                    drawS(current, if (eraser) paper else color, if (eraser) width * 4 else width)
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                inks.forEach { c ->
                    Box(
                        Modifier.size(34.dp).hardShadow(p.shadow, if (c == color && !eraser) 0.dp else 3.dp).clip(BrutalShape).background(c)
                            .border(if (c == color && !eraser) 3.dp else 1.5.dp, p.line, BrutalShape)
                            .clickable { color = c; eraser = false },
                    )
                }
                Spacer(Modifier.width(6.dp))
                listOf(3f, 6f, 12f).forEach { w ->
                    Box(
                        Modifier.size(34.dp).clip(BrutalShape).background(if (width == w) p.iceWash else p.surface)
                            .border(if (width == w) 3.dp else 1.5.dp, p.line, BrutalShape).clickable { width = w },
                        contentAlignment = Alignment.Center,
                    ) { Box(Modifier.size((w * 1.4f).dp.coerceAtMost(20.dp)).clip(CircleShape).background(p.ink)) }
                }
                Box(
                    Modifier.height(34.dp).clip(BrutalShape).background(if (eraser) p.canary else p.surface)
                        .border(if (eraser) 3.dp else 1.5.dp, p.line, BrutalShape).clickable { eraser = !eraser }.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("GOMA", style = T.micro, color = if (eraser) Color(0xFF383838) else p.ink) }
                Box(
                    Modifier.height(34.dp).clip(BrutalShape).background(p.surface)
                        .border(1.5.dp, p.line, BrutalShape).clickable { strokes.clear() }.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("LIMPIAR", style = T.micro, color = p.ink) }
            }
        }
    }
}

// ---------------- Visor de imágenes ----------------

@Composable
fun ImageViewerDialog(file: File, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xF0181716))) {
            AsyncImage(
                model = file, contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 6f)
                            offset = if (scale == 1f) Offset.Zero else offset + pan
                        }
                    }
                    .pointerInput(Unit) { detectTapGestures(onDoubleTap = { scale = if (scale > 1f) 1f else 2.5f; offset = Offset.Zero }) }
                    .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
            )
            Box(Modifier.statusBarsPadding().padding(16.dp)) {
                SquareIconButton(Icons.Filled.Close, "Cerrar", onDismiss)
            }
        }
    }
}

// ---------------- PDF ----------------

@Composable
fun PdfThumbnail(file: File, modifier: Modifier = Modifier) {
    var bmp by remember(file) { mutableStateOf<ImageBitmap?>(null) }
    var pages by remember(file) { mutableStateOf(0) }
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            runCatching {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { r ->
                        pages = r.pageCount
                        r.openPage(0).use { page ->
                            val w = 600
                            val h = (w * page.height / page.width.toFloat()).toInt()
                            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            b.eraseColor(android.graphics.Color.WHITE)
                            page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp = b.asImageBitmap()
                        }
                    }
                }
            }
        }
    }
    val b = bmp
    if (b != null) {
        Box(modifier) {
            Image(b, null, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
            if (pages > 1) {
                Text(
                    "$pages PÁGS", style = T.micro, color = Color(0xFF383838),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color(0xFFFFDE00)).border(1.dp, Color(0xFF383838)).padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }
    }
}
