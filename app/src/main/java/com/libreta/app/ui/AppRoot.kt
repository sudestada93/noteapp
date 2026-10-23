package com.libreta.app.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.foundation.shape.CircleShape
import com.libreta.app.ui.components.ArchShape
import com.libreta.app.ui.components.SheetItem
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.libreta.app.ui.components.BrutalShape
import com.libreta.app.ui.components.BrutalSheet
import com.libreta.app.ui.components.Pressable
import com.libreta.app.ui.components.rememberApp
import com.libreta.app.ui.editor.EditorScreen
import com.libreta.app.ui.screens.CalendarScreen
import com.libreta.app.ui.screens.HomeScreen
import com.libreta.app.ui.screens.MoreScreen
import com.libreta.app.ui.screens.Nav
import com.libreta.app.ui.screens.NotebooksScreen
import com.libreta.app.ui.screens.NotesScreen
import com.libreta.app.ui.screens.SearchScreen
import com.libreta.app.ui.screens.SettingsScreen
import com.libreta.app.ui.screens.TagsScreen
import com.libreta.app.ui.screens.TasksScreen
import com.libreta.app.ui.screens.TemplatesScreen
import com.libreta.app.ui.screens.TrashScreen
import com.libreta.app.ui.theme.Dims
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T
import com.libreta.app.ui.theme.crayon
import kotlinx.coroutines.launch

/** Acción externa (widget, atajo, notificación, compartir). */
sealed interface External {
    data class OpenNote(val id: String) : External
    data class Action(val name: String) : External
}

private fun enc(s: String) = Uri.encode(s)

@Composable
fun AppRoot(external: External?, onExternalHandled: () -> Unit) {
    val navController = rememberNavController()
    val app = rememberApp()
    val repo = app.repository
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    var createOpen by remember { mutableStateOf(false) }
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"

    val nav = remember(navController) {
        Nav(
            openNote = { id -> navController.navigate("note/$id") { launchSingleTop = true } },
            newNote = { start, nb, tag ->
                scope.launch {
                    val id = repo.createNote(nb, tagIds = listOfNotNull(tag))
                    navController.navigate("note/$id" + (start?.let { "?start=$it" } ?: ""))
                }
            },
            openNotes = { nb, tag, filter ->
                val q = listOfNotNull(nb?.let { "nb=$it" }, tag?.let { "tag=$it" }, filter?.let { "filter=$it" }).joinToString("&")
                navController.navigate("notes" + if (q.isNotEmpty()) "?$q" else "")
            },
            openSearch = { q -> navController.navigate("search" + (q?.let { "?q=${enc(it)}" } ?: "")) },
            go = { r -> navController.navigate(r) { launchSingleTop = true } },
            back = { navController.popBackStack() },
            openCreate = { createOpen = true },
            newFromTemplate = { navController.navigate("templates") },
        )
    }

    LaunchedEffect(external) {
        when (val e = external) {
            is External.OpenNote -> nav.openNote(e.id)
            is External.Action -> when (e.name) {
                "note" -> nav.newNote(null, null, null)
                "audio" -> nav.newNote("audio", null, null)
                "camera" -> nav.newNote("camera", null, null)
                "scan" -> nav.newNote("scan", null, null)
                "sketch" -> nav.newNote("sketch", null, null)
                "task" -> navController.navigate("tasks?new=1")
                "tasks" -> navController.navigate("tasks")
                "search" -> nav.openSearch(null)
            }
            null -> {}
        }
        if (external != null) onExternalHandled()
    }

    val topLevel = route == "home" || route.startsWith("notes") && backStack?.arguments?.getString("nb") == null && backStack?.arguments?.getString("tag") == null && backStack?.arguments?.getString("filter") == null ||
        route.startsWith("tasks") || route == "more"

    Box(Modifier.fillMaxSize().background(p.canvas)) {
        NavHost(
            navController, startDestination = "home",
            enterTransition = { fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 12 } },
            exitTransition = { fadeOut(tween(140)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition = { fadeOut(tween(140)) + slideOutHorizontally(tween(200)) { it / 12 } },
        ) {
            composable("home", enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }) { HomeScreen(nav) }
            composable(
                "notes?nb={nb}&tag={tag}&filter={filter}",
                arguments = listOf(
                    navArgument("nb") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("tag") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("filter") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { e ->
                val nb = e.arguments?.getString("nb")
                val tag = e.arguments?.getString("tag")
                val filter = e.arguments?.getString("filter")
                NotesScreen(nb, tag, filter, nav, showBack = nb != null || tag != null || filter != null)
            }
            composable(
                "note/{id}?start={start}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }, navArgument("start") { type = NavType.StringType; nullable = true; defaultValue = null }),
                enterTransition = { slideInVertically(tween(260)) { it / 6 } + fadeIn(tween(200)) },
                popExitTransition = { slideOutVertically(tween(220)) { it / 6 } + fadeOut(tween(180)) },
            ) { e ->
                EditorScreen(e.arguments?.getString("start"), onBack = { navController.popBackStack() }, onOpenNote = { nav.openNote(it) })
            }
            composable("tasks?new={new}", arguments = listOf(navArgument("new") { type = NavType.StringType; nullable = true; defaultValue = null })) { e ->
                TasksScreen(nav, focusNew = e.arguments?.getString("new") != null)
            }
            composable("more") { MoreScreen(nav) }
            composable("notebooks") { NotebooksScreen(nav) }
            composable("tags") { TagsScreen(nav) }
            composable("search?q={q}", arguments = listOf(navArgument("q") { type = NavType.StringType; nullable = true; defaultValue = null })) { e ->
                SearchScreen(e.arguments?.getString("q"), nav)
            }
            composable("calendar") { CalendarScreen(nav) }
            composable("trash") { TrashScreen(nav) }
            composable("settings") { SettingsScreen(nav) }
            composable("templates") {
                TemplatesScreen(nav, null) { id -> navController.navigate("note/$id") { popUpTo("templates") { inclusive = true } } }
            }
        }

        AnimatedVisibility(
            topLevel, modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it }, exit = slideOutVertically { it },
        ) {
            BottomBar(route, navController) { createOpen = true }
        }
    }

    if (createOpen) CreateSheet(onDismiss = { createOpen = false }) { key ->
        createOpen = false
        when (key) {
            "note" -> nav.newNote(null, null, null)
            "task" -> navController.navigate("tasks?new=1") { launchSingleTop = true }
            "template" -> nav.newFromTemplate()
            else -> nav.newNote(key, null, null)
        }
    }
}

@Composable
private fun BottomBar(route: String, nav: NavHostController, onCreate: () -> Unit) {
    val p = LocalPalette.current
    fun go(r: String) {
        nav.navigate(r) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().background(p.canvas)
                .drawBehind { drawLine(p.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                .navigationBarsPadding().height(70.dp).padding(horizontal = 10.dp).padding(top = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            NavItem("Inicio", Icons.Outlined.Home, route == "home", Modifier.weight(1f)) { go("home") }
            NavItem("Notas", Icons.Outlined.StickyNote2, route.startsWith("notes"), Modifier.weight(1f)) { go("notes") }
            Spacer(Modifier.weight(0.9f))
            NavItem("Tareas", Icons.Outlined.TaskAlt, route.startsWith("tasks"), Modifier.weight(1f)) { go("tasks") }
            NavItem("Más", Icons.Outlined.GridView, route == "more", Modifier.weight(1f)) { go("more") }
        }
        Pressable(
            Modifier.align(Alignment.TopCenter).offset(y = (-18).dp).size(58.dp),
            color = p.primary, borderColor = p.canvas, border = 4.dp, shape = CircleShape, onClick = onCreate,
        ) {
            Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, "Crear", tint = p.onPrimary, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        modifier.padding(horizontal = 4.dp).height(60.dp)
            .border(1.dp, if (selected) p.rule.copy(alpha = 0.5f) else p.line, ArchShape)
            .clip(ArchShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 10.dp)) {
            Icon(icon, label, tint = if (selected) p.ink else p.inkFaint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(label.uppercase(), style = T.micro.copy(letterSpacing = T.micro.letterSpacing * 0.8f), color = if (selected) p.ink else p.inkFaint, maxLines = 1, overflow = TextOverflow.Clip)
        }
        if (selected) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp).background(p.canary))
    }
}

private data class CreateItem(val key: String, val label: String, val sub: String, val icon: ImageVector)

@Composable
private fun CreateSheet(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val items = listOf(
        CreateItem("note", "Nota", "Una página en blanco", Icons.AutoMirrored.Outlined.NoteAdd),
        CreateItem("task", "Tarea", "Con fecha y recordatorio", Icons.Outlined.TaskAlt),
        CreateItem("template", "Desde plantilla", "Reunión, diario, viaje…", Icons.Outlined.Dashboard),
        CreateItem("audio", "Nota de voz", "Grabá y escuchá después", Icons.Outlined.Mic),
        CreateItem("camera", "Foto", "Con la cámara", Icons.Outlined.CameraAlt),
        CreateItem("scan", "Escanear", "Documentos con recorte", Icons.Outlined.DocumentScanner),
        CreateItem("sketch", "Boceto", "Dibujar a mano alzada", Icons.Outlined.Brush),
        CreateItem("file", "Archivo", "PDF y otros", Icons.Outlined.AttachFile),
    )
    BrutalSheet(onDismiss, "Crear") {
        items.forEach { SheetItem(it.icon, it.label, it.sub) { onPick(it.key) } }
    }
}
