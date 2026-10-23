package com.libreta.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsNone
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
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (n.isPinned) Icon(Icons.Filled.PushPin, null, tint = p.inkSoft, modifier = Modifier.size(12.dp))
        if (n.isShortcut) Icon(Icons.Filled.Star, null, tint = p.canary, modifier = Modifier.size(13.dp))
        if (n.reminderAt != null && !n.reminderDone) Icon(Icons.Filled.NotificationsNone, null, tint = p.inkFaint, modifier = Modifier.size(13.dp))
        if (n.isLocked) Icon(Icons.Filled.Lock, null, tint = p.inkFaint, modifier = Modifier.size(12.dp))
        if (n.hasImage) Icon(Icons.Filled.Image, null, tint = p.inkFaint, modifier = Modifier.size(12.dp))
        if (n.hasAudio) Icon(Icons.Filled.Mic, null, tint = p.inkFaint, modifier = Modifier.size(12.dp))
        if (n.hasFile) Icon(Icons.Filled.AttachFile, null, tint = p.inkFaint, modifier = Modifier.size(12.dp))
        if (n.hasTask || n.hasChecklist) Icon(Icons.Filled.CheckCircleOutline, null, tint = p.inkFaint, modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun Meta(item: NoteItem, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(crayon(item.notebook?.color ?: 0)))
        Spacer(Modifier.width(7.dp))
        Text(
            ((item.notebook?.name ?: "") + "  ·  " + Fmt.relative(item.note.updatedAt)).uppercase(),
            style = T.micro, color = p.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(8.dp))
        NoteIcons(item)
    }
}

/** viewMode: 0 lista, 1 fichas, 2 grilla, 3 compacta */
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
    val bg = if (selected) p.iceWash else if (n.color >= 0) crayonWash(n.color, p.dark) else p.surface
    val title = n.title.ifBlank { "Sin título" }
    val preview = if (n.isLocked) "Contenido protegido" else n.preview
    val cover = n.coverPath?.takeIf { !n.isLocked }
    when (viewMode) {
        3 -> Row(
            modifier.fillMaxWidth().background(if (selected) p.iceWash else Color.Transparent)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick).hairlineBottom(p.line).padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(if (n.color >= 0) crayon(n.color) else crayon(item.notebook?.color ?: 0)))
            Spacer(Modifier.width(12.dp))
            Text(title, style = T.subheading, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(Fmt.relative(n.updatedAt), style = T.caption, color = p.inkFaint)
        }
        0 -> Row(
            modifier.fillMaxWidth().background(if (selected) p.iceWash else Color.Transparent)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick).hairlineBottom(p.line).padding(vertical = 16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = T.headingSm, color = p.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (preview.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(preview, style = T.bodySm, color = p.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
                Meta(item)
            }
            if (cover != null) {
                Spacer(Modifier.width(14.dp))
                AsyncImage(cover.let(::coverFile), null, contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)))
            }
        }
        2 -> Pressable(modifier.fillMaxWidth(), color = bg, onClick = onClick, onLongClick = onLongClick) {
            Column {
                if (cover != null) {
                    AsyncImage(coverFile(cover), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(104.dp))
                }
                Column(Modifier.padding(14.dp)) {
                    Text(title, style = T.headingSm.copy(fontSize = T.headingSm.fontSize * 0.9f), color = p.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (preview.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(preview, style = T.caption, color = p.inkSoft, maxLines = if (cover != null) 3 else 7, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(Fmt.relative(n.updatedAt).uppercase(), style = T.micro, color = p.inkFaint)
                }
            }
        }
        else -> Pressable(modifier.fillMaxWidth(), color = bg, onClick = onClick, onLongClick = onLongClick) {
            Column(Modifier.padding(18.dp)) {
                Meta(item)
                Spacer(Modifier.height(10.dp))
                Row {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = T.heading.copy(fontSize = T.heading.fontSize * 0.9f), color = p.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (preview.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(preview, style = T.bodySm, color = p.inkSoft, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (cover != null) {
                        Spacer(Modifier.width(14.dp))
                        AsyncImage(coverFile(cover), null, contentScale = ContentScale.Crop, modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)))
                    }
                }
                if (item.tags.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.tags.take(4).forEach { t -> Sticker(t.name, crayon(t.color)) }
                    }
                }
            }
        }
    }
}


private var attachmentsRoot: java.io.File? = null
fun setAttachmentsRoot(dir: java.io.File) { attachmentsRoot = dir }
fun coverFile(path: String): java.io.File = java.io.File(attachmentsRoot, path)
