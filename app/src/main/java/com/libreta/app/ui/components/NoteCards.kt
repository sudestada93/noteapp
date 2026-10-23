package com.libreta.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.libreta.app.data.NoteItem
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T
import com.libreta.app.ui.theme.crayon
import com.libreta.app.ui.theme.crayonWash
import com.libreta.app.util.Fmt

@Composable
private fun NoteIcons(item: NoteItem) {
    val p = LocalPalette.current
    val n = item.note
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        if (n.isPinned) Icon(Icons.Filled.PushPin, null, tint = p.ink, modifier = Modifier.size(13.dp))
        if (n.isShortcut) Icon(Icons.Filled.Star, null, tint = p.orange, modifier = Modifier.size(13.dp))
        if (n.isLocked) Icon(Icons.Filled.Lock, null, tint = p.inkSoft, modifier = Modifier.size(13.dp))
        if (n.hasImage) Icon(Icons.Filled.Image, null, tint = p.inkSoft, modifier = Modifier.size(13.dp))
        if (n.hasAudio) Icon(Icons.Filled.Mic, null, tint = p.inkSoft, modifier = Modifier.size(13.dp))
        if (n.hasFile) Icon(Icons.Filled.AttachFile, null, tint = p.inkSoft, modifier = Modifier.size(13.dp))
        if (n.hasTask || n.hasChecklist) Icon(Icons.Filled.CheckCircle, null, tint = p.inkSoft, modifier = Modifier.size(13.dp))
    }
}

@Composable
private fun ReminderChip(item: NoteItem) {
    val p = LocalPalette.current
    val at = item.note.reminderAt ?: return
    val overdue = !item.note.reminderDone && at < System.currentTimeMillis()
    Row(
        Modifier.clip(BrutalShape).background(if (item.note.reminderDone) p.surfaceAlt else if (overdue) p.danger else p.canary)
            .border(1.dp, p.line, BrutalShape).padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Alarm, null, tint = Color(0xFF383838), modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(3.dp))
        Text(Fmt.due(at), style = T.micro, color = Color(0xFF383838))
    }
}

/** viewMode: 0 lista, 1 tarjetas, 2 grilla, 3 compacta */
@Composable
fun NoteCard(
    item: NoteItem,
    viewMode: Int,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val p = LocalPalette.current
    val n = item.note
    val accent = if (n.color >= 0) crayon(n.color) else crayon(item.notebook?.color ?: 0)
    val bg = if (selected) p.iceWash else if (n.color >= 0) crayonWash(n.color, p.dark) else p.surface
    val title = n.title.ifBlank { "Sin título" }
    val preview = if (n.isLocked) "🔒 Contenido bloqueado" else n.preview
    when (viewMode) {
        3 -> Pressable(modifier.fillMaxWidth(), color = bg, shadow = 2.dp, border = 1.5.dp, onClick = onClick, onLongClick = onLongClick) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(accent).border(1.dp, p.line))
                Spacer(Modifier.width(10.dp))
                Text(title, style = T.body.copy(fontWeight = FontWeight.SemiBold), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(Fmt.relative(n.updatedAt), style = T.caption, color = p.inkSoft)
            }
        }
        2 -> Pressable(modifier.fillMaxWidth(), color = bg, shadow = 4.dp, onClick = onClick, onLongClick = onLongClick) {
            Column {
                if (n.coverPath != null && !n.isLocked) {
                    AsyncImage(
                        model = com.libreta.app.ui.components.coverFile(n.coverPath), contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(96.dp),
                    )
                    Box(Modifier.fillMaxWidth().height(2.dp).background(p.line))
                } else Box(Modifier.fillMaxWidth().height(8.dp).background(accent))
                Column(Modifier.padding(12.dp)) {
                    Text(title, style = T.bodySm.copy(fontWeight = FontWeight.Bold), color = p.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text(preview, style = T.caption, color = p.inkSoft, maxLines = if (n.coverPath != null) 3 else 6, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    ReminderChip(item)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Text(Fmt.relative(n.updatedAt), style = T.micro, color = p.inkFaint, modifier = Modifier.weight(1f))
                        NoteIcons(item)
                    }
                }
            }
        }
        0 -> Pressable(modifier.fillMaxWidth(), color = bg, shadow = 3.dp, onClick = onClick, onLongClick = onLongClick) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                Box(Modifier.width(8.dp).fillMaxHeight().background(accent))
                Box(Modifier.width(2.dp).fillMaxHeight().background(p.line))
                Column(Modifier.weight(1f).padding(12.dp)) {
                    Text(title, style = T.body.copy(fontWeight = FontWeight.SemiBold), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (preview.isNotBlank()) Text(preview, style = T.caption, color = p.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Text(Fmt.relative(n.updatedAt) + " · " + (item.notebook?.name ?: ""), style = T.micro, color = p.inkFaint, modifier = Modifier.weight(1f), maxLines = 1)
                        NoteIcons(item)
                    }
                }
                if (n.coverPath != null && !n.isLocked) {
                    AsyncImage(
                        model = coverFile(n.coverPath), contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.width(78.dp).fillMaxHeight().border(1.dp, p.line),
                    )
                }
            }
        }
        else -> Pressable(modifier.fillMaxWidth(), color = bg, shadow = 5.dp, onClick = onClick, onLongClick = onLongClick) {
            Column {
                Row(Modifier.fillMaxWidth().background(accent).padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text((item.notebook?.emoji?.plus(" ") ?: "") + (item.notebook?.name ?: "").uppercase(), style = T.micro, color = Color(0xFF383838), modifier = Modifier.weight(1f), maxLines = 1)
                    Text(Fmt.relative(n.updatedAt).uppercase(), style = T.micro, color = Color(0xFF383838))
                }
                Box(Modifier.fillMaxWidth().height(2.dp).background(p.line))
                Row(Modifier.padding(14.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = T.subheading.copy(fontWeight = FontWeight.Bold), color = p.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (preview.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(preview, style = T.bodySm, color = p.inkSoft, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (n.coverPath != null && !n.isLocked) {
                        Spacer(Modifier.width(12.dp))
                        AsyncImage(
                            model = coverFile(n.coverPath), contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(72.dp).border(1.5.dp, p.line),
                        )
                    }
                }
                if (item.tags.isNotEmpty() || n.reminderAt != null || n.hasImage || n.hasAudio || n.hasFile || n.hasTask || n.isShortcut) {
                    Row(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item { ReminderChip(item) }
                            items(item.tags, key = { it.id }) { t -> Sticker(t.name, crayon(t.color)) }
                        }
                        NoteIcons(item)
                    }
                }
            }
        }
    }
}

private var attachmentsRoot: java.io.File? = null
fun setAttachmentsRoot(dir: java.io.File) { attachmentsRoot = dir }
fun coverFile(path: String): java.io.File = java.io.File(attachmentsRoot, path)
