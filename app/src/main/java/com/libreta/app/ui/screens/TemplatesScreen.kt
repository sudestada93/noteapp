package com.libreta.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.libreta.app.data.TemplateEntity
import com.libreta.app.model.Block
import com.libreta.app.model.BlockCodec
import com.libreta.app.model.BlockType
import com.libreta.app.ui.components.BareIcon
import com.libreta.app.ui.components.ConfirmDialog
import com.libreta.app.ui.components.LibretaTopBar
import com.libreta.app.ui.components.Pressable
import com.libreta.app.ui.components.rememberApp
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T
import com.libreta.app.ui.theme.crayon
import com.libreta.app.util.Fmt
import kotlinx.coroutines.launch

data class Template(val emoji: String, val name: String, val title: () -> String, val blocks: () -> List<Block>, val custom: TemplateEntity? = null)

private fun t(text: String = "", type: BlockType = BlockType.PARAGRAPH) = Block(type = type, text = text)

val BuiltInTemplates = listOf(
    Template("🗓", "Reunión", { "Reunión — " + Fmt.short(System.currentTimeMillis()) }, {
        listOf(
            t("**Fecha:** " + Fmt.full(System.currentTimeMillis())), t("**Asistentes:** "),
            t("Objetivo", BlockType.H2), t("", BlockType.PARAGRAPH),
            t("Temas", BlockType.H2), t("", BlockType.NUMBERED),
            t("Decisiones", BlockType.H2), t("", BlockType.BULLET),
            t("Próximos pasos", BlockType.H2), t("", BlockType.TASK),
        )
    }),
    Template("📔", "Diario", { Fmt.dayHeader(System.currentTimeMillis()) }, {
        listOf(
            t("¿Cómo me siento hoy?", BlockType.H3), t(),
            t("Tres cosas por las que estoy agradecido", BlockType.H3), t("", BlockType.NUMBERED), t("", BlockType.NUMBERED), t("", BlockType.NUMBERED),
            t("Lo mejor del día", BlockType.H3), t(),
            t("Mañana quiero…", BlockType.H3), t("", BlockType.CHECK),
        )
    }),
    Template("✅", "Lista de tareas", { "Pendientes" }, {
        listOf(t("Hoy", BlockType.H2), t("", BlockType.TASK), t("Esta semana", BlockType.H2), t("", BlockType.TASK), t("Algún día", BlockType.H2), t("", BlockType.CHECK))
    }),
    Template("🚀", "Plan de proyecto", { "Proyecto: " }, {
        listOf(
            t("Resumen del proyecto en una frase.", BlockType.CALLOUT),
            t("Objetivos", BlockType.H2), t("", BlockType.BULLET),
            Block(type = BlockType.TABLE, rows = listOf(listOf("Hito", "Responsable", "Fecha"), listOf("", "", ""), listOf("", "", ""))),
            t("Riesgos", BlockType.H2), t("", BlockType.BULLET),
            t("Tareas", BlockType.H2), t("", BlockType.TASK),
        )
    }),
    Template("🍳", "Receta", { "Receta: " }, {
        listOf(t("**Porciones:** · **Tiempo:** "), t("Ingredientes", BlockType.H2), t("", BlockType.CHECK), t("Preparación", BlockType.H2), t("", BlockType.NUMBERED), t("Notas", BlockType.H3), t())
    }),
    Template("✈️", "Viaje", { "Viaje a " }, {
        listOf(
            t("Datos", BlockType.H2), t("**Fechas:** "), t("**Alojamiento:** "),
            t("Equipaje", BlockType.H2), t("Documentos", BlockType.CHECK), t("Cargadores", BlockType.CHECK), t("", BlockType.CHECK),
            t("Itinerario", BlockType.H2), Block(type = BlockType.TABLE, rows = listOf(listOf("Día", "Plan"), listOf("1", ""), listOf("2", ""))),
        )
    }),
    Template("📚", "Notas de lectura", { "Libro: " }, {
        listOf(t("**Autor:** · **Puntaje:** ★★★★☆"), t("Ideas clave", BlockType.H2), t("", BlockType.BULLET), t("Citas", BlockType.H2), t("", BlockType.QUOTE), t("Reflexión", BlockType.H2), t())
    }),
    Template("📆", "Planificador semanal", { "Semana del " + Fmt.short(System.currentTimeMillis()) }, {
        listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Fin de semana").flatMap { listOf(t(it, BlockType.H3), t("", BlockType.CHECK)) } +
            listOf(t("Foco de la semana", BlockType.CALLOUT))
    }),
    Template("💡", "Lluvia de ideas", { "Ideas: " }, {
        listOf(t("Problema", BlockType.H2), t(), t("Ideas", BlockType.H2), t("", BlockType.BULLET), t("Mejores 3", BlockType.H2), t("", BlockType.NUMBERED), t("Siguiente experimento", BlockType.TASK))
    }),
)

@Composable
fun TemplatesScreen(nav: Nav, notebookId: String?, onCreated: (String) -> Unit) {
    val repo = rememberApp().repository
    val p = LocalPalette.current
    val custom by repo.templates.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var deleting by remember { mutableStateOf<TemplateEntity?>(null) }
    val all = custom.map { c -> Template(c.emoji, c.name, { c.title }, { BlockCodec.decode(c.content) }, c) } + BuiltInTemplates

    fun use(tpl: Template) {
        scope.launch {
            val id = repo.createNote(notebookId, tpl.title(), tpl.blocks().ifEmpty { listOf(Block()) })
            onCreated(id)
        }
    }

    Column(Modifier.fillMaxSize().background(p.canvas)) {
        LibretaTopBar("Plantillas", onBack = nav.back, subtitle = "Empezá con estructura")
        LazyVerticalGrid(
            GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(2) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    com.libreta.app.ui.components.Engraving(com.libreta.app.ui.components.Art.BOOKS, Modifier.height(120.dp))
                }
            }
            if (custom.isNotEmpty()) item(span = { GridItemSpan(2) }) { Text("MIS PLANTILLAS", style = T.micro, color = p.inkFaint) }
            items(all, key = { it.custom?.id ?: it.name }) { tpl ->
                val i = all.indexOf(tpl)
                Pressable(Modifier.fillMaxWidth(), onClick = { use(tpl) }, onLongClick = tpl.custom?.let { c -> { deleting = c } }) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("%02d".format(i + 1), style = T.micro, color = p.inkFaint, modifier = Modifier.weight(1f))
                            if (tpl.custom != null) BareIcon(Icons.Filled.Delete, "Eliminar", { deleting = tpl.custom }, tint = p.inkFaint)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(tpl.name, style = T.heading, color = p.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            BlockCodec.preview(tpl.blocks(), 80).ifBlank { tpl.blocks().filter { it.type.isText }.joinToString(" · ") { it.type.name.lowercase() } }.take(80),
                            style = T.caption, color = p.inkSoft, maxLines = 3, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (custom.isEmpty()) item(span = { GridItemSpan(2) }) {
                Text("Tip: guardá cualquier nota como plantilla desde su menú ⋮.", style = T.caption, color = p.inkFaint, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
    deleting?.let { c ->
        ConfirmDialog("Eliminar plantilla", "¿Eliminar \"${c.name}\"?", "Eliminar", { deleting = null }) { scope.launch { repo.deleteTemplate(c.id) }; deleting = null }
    }
}
