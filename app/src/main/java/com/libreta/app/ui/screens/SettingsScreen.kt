package com.libreta.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.libreta.app.BuildConfig
import com.libreta.app.data.Backup
import com.libreta.app.ui.components.BrutalCard
import com.libreta.app.ui.components.BrutalDialog
import com.libreta.app.ui.components.ConfirmDialog
import com.libreta.app.ui.components.FilterPill
import com.libreta.app.ui.components.LibretaTopBar
import com.libreta.app.ui.components.NotebookPickerSheet
import com.libreta.app.ui.components.PinPad
import com.libreta.app.ui.components.SectionHeader
import com.libreta.app.ui.components.TextInputDialog
import com.libreta.app.ui.components.rememberApp
import com.libreta.app.ui.components.settingsState
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T
import com.libreta.app.ui.theme.editorFamily
import com.libreta.app.util.Biometric
import com.libreta.app.util.Fmt
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(nav: Nav) {
    val app = rememberApp()
    val repo = app.repository
    val p = LocalPalette.current
    val s by settingsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notebooks by repo.notebooks.collectAsStateWithLifecycle(emptyList())
    val notes by repo.noteItems.collectAsStateWithLifecycle(emptyList())
    val tasks by repo.tasks.collectAsStateWithLifecycle(emptyList())
    var dialog by remember { mutableStateOf<String?>(null) }
    var newPin by remember { mutableStateOf<String?>(null) }
    fun toast(t: String) = Toast.makeText(context, t, Toast.LENGTH_SHORT).show()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            runCatching { Backup.export(context, repo, uri) }.onSuccess { toast("Copia creada: $it notas") }.onFailure { toast("Error al exportar") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { Backup.import(context, repo, uri) }.onSuccess { toast("Importadas $it notas") }.onFailure { toast("Archivo de copia inválido") }
        }
    }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) toast("Notificaciones activadas") }

    Column(Modifier.fillMaxSize().background(p.canvas)) {
        LibretaTopBar("Ajustes", onBack = nav.back)
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                BrutalCard(Modifier.fillMaxWidth(), color = p.canary) {
                    Text("TU LIBRETA", style = T.micro, color = androidx.compose.ui.graphics.Color(0xFF383838))
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Stat("${notes.size}", "notas", Modifier.weight(1f))
                        Stat("${notebooks.size}", "libretas", Modifier.weight(1f))
                        Stat("${tasks.count { it.done }}", "tareas hechas", Modifier.weight(1f))
                        Stat("${notes.sumOf { it.note.wordCount }}", "palabras", Modifier.weight(1f))
                    }
                }
            }
            item {
                Group("Perfil") {
                    Item("Tu nombre", s.userName.ifBlank { "Sin nombre" }) { dialog = "name" }
                }
            }
            item {
                Group("Apariencia") {
                    Label("Tema")
                    Pills(listOf("Sistema", "Papel", "Pizarra"), s.themeMode) { scope.launch { repo.prefs.setTheme(it) } }
                    Label("Tipografía del editor")
                    Pills(listOf("Sans", "Mono", "Serif"), s.editorFont) { scope.launch { repo.prefs.setFont(it) } }
                    var size by remember(s.editorFontSize) { mutableFloatStateOf(s.editorFontSize) }
                    Label("Tamaño de texto · ${size.toInt()}")
                    Text("Así se verá tu nota.", style = T.body.copy(fontFamily = editorFamily(s.editorFont), fontSize = size.sp), color = p.ink)
                    Slider(
                        size, { size = it }, valueRange = 13f..22f, steps = 8,
                        onValueChangeFinished = { scope.launch { repo.prefs.setFontSize(size) } },
                        colors = SliderDefaults.colors(thumbColor = p.ink, activeTrackColor = p.primary, inactiveTrackColor = p.surfaceAlt),
                    )
                    Label("Papel")
                    Pills(listOf("Liso", "Rayado", "Cuadriculado", "Punteado"), s.paperStyle) { scope.launch { repo.prefs.setPaper(it) } }
                    Label("Vista de notas")
                    Pills(listOf("Lista", "Tarjetas", "Grilla", "Compacta"), s.listView) { scope.launch { repo.prefs.setListView(it) } }
                }
            }
            item {
                Group("Notas") {
                    Item("Libreta predeterminada", notebooks.firstOrNull { it.id == s.defaultNotebookId }?.name ?: "—") { dialog = "nb" }
                    Label("Vaciar papelera automáticamente")
                    Pills(listOf("Nunca", "7 días", "30 días", "60 días"), listOf(0, 7, 30, 60).indexOf(s.trashAutoDays).coerceAtLeast(0)) {
                        scope.launch { repo.prefs.setTrashDays(listOf(0, 7, 30, 60)[it]) }
                    }
                    Toggle("Vibración al tocar", s.haptics) { scope.launch { repo.prefs.setHaptics(it) } }
                }
            }
            item {
                Group("Seguridad") {
                    Item(if (s.lockEnabled) "Cambiar PIN" else "Configurar PIN", if (s.lockEnabled) "Bloqueo activo" else "Protegé la app y notas privadas") { dialog = "pin" }
                    if (s.lockEnabled) {
                        if (Biometric.available(context)) Toggle("Desbloquear con huella / rostro", s.biometric) { scope.launch { repo.prefs.setBiometric(it) } }
                        Label("Bloquear tras salir de la app")
                        Pills(listOf("Al instante", "1 min", "5 min", "15 min"), listOf(0, 1, 5, 15).indexOf(s.lockTimeoutMin).coerceAtLeast(0)) {
                            scope.launch { repo.prefs.setLockTimeout(listOf(0, 1, 5, 15)[it]) }
                        }
                        Item("Quitar PIN", "Desactiva el bloqueo") { dialog = "removepin" }
                    }
                }
            }
            item {
                Group("Recordatorios") {
                    Item("Permiso de notificaciones", "Necesario para avisos de notas y tareas") {
                        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            runCatching {
                                context.startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName))
                            }
                        }
                    }
                }
            }
            item {
                Group("Datos y copia de seguridad") {
                    Item("Exportar copia completa (.zip)", if (s.lastBackupAt > 0) "Última: " + Fmt.relative(s.lastBackupAt) else "Notas, libretas, etiquetas, tareas y adjuntos") {
                        exportLauncher.launch("libreta-" + Fmt.short(System.currentTimeMillis()).replace('/', '-') + ".zip")
                    }
                    Item("Importar copia", "Combina con tus notas actuales") { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }
                }
            }
            item {
                Group("Acerca de") {
                    Item("Libreta", "Versión ${BuildConfig.VERSION_NAME}") {}
                    Item("Licencias", "JetBrains Mono e IBM Plex (SIL OFL 1.1)") { dialog = "licenses" }
                    Item("Código fuente", "github.com/sudestada93/noteapp") {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sudestada93/noteapp"))) }
                    }
                }
            }
        }
    }

    when (dialog) {
        "name" -> TextInputDialog("Tu nombre", s.userName, "¿Cómo te llamás?", onDismiss = { dialog = null }) { scope.launch { repo.prefs.setUserName(it) }; dialog = null }
        "nb" -> NotebookPickerSheet(notebooks, s.defaultNotebookId, { dialog = null }, onCreate = { name ->
            scope.launch { val id = repo.createNotebook(name, (0..7).random(), null, null); repo.prefs.setDefaultNotebook(id) }; dialog = null
        }) { scope.launch { repo.prefs.setDefaultNotebook(it.id) }; dialog = null }
        "pin" -> Dialog(onDismissRequest = { dialog = null; newPin = null }) {
            Box(Modifier.background(p.canvas).padding(20.dp)) {
                val first = newPin
                if (first == null) PinPad("Nuevo PIN", "Elegí 4 dígitos") { newPin = it; true }
                else PinPad("Confirmá el PIN", "Ingresalo de nuevo") { pin ->
                    (pin == first).also { ok ->
                        if (ok) { scope.launch { repo.prefs.setPin(pin) }; toast("PIN configurado"); dialog = null; newPin = null }
                    }
                }
            }
        }
        "removepin" -> ConfirmDialog("Quitar PIN", "La app y las notas bloqueadas quedarán sin protección.", "Quitar", { dialog = null }) {
            scope.launch { repo.prefs.setPin(null) }; dialog = null
        }
        "licenses" -> BrutalDialog("Licencias", { dialog = null }) {
            val text = remember {
                runCatching {
                    context.assets.list("licenses")?.joinToString("\n\n————\n\n") { f -> "$f\n\n" + context.assets.open("licenses/$f").bufferedReader().readText() }
                }.getOrNull() ?: ""
            }
            Text(text, style = T.caption, color = p.ink)
        }
    }
}

@Composable
private fun Stat(v: String, l: String, modifier: Modifier) {
    Column(modifier) {
        Text(v, style = T.headingSm, color = androidx.compose.ui.graphics.Color(0xFF383838))
        Text(l.uppercase(), style = T.micro, color = androidx.compose.ui.graphics.Color(0xFF383838))
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column {
        SectionHeader(title)
        BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) { content() }
    }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), style = T.micro, color = LocalPalette.current.inkSoft, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
}

@Composable
private fun Pills(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 6.dp)) {
        options.forEachIndexed { i, o -> FilterPill(o, i == selected, { onSelect(i) }) }
    }
}

@Composable
private fun Item(title: String, subtitle: String?, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.body.copy(fontWeight = FontWeight.SemiBold), color = p.ink)
            if (subtitle != null) Text(subtitle, style = T.caption, color = p.inkSoft)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = p.inkSoft, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Toggle(title: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = T.body.copy(fontWeight = FontWeight.SemiBold), color = p.ink, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Switch(
            value, onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = p.primary, checkedThumbColor = p.surface, uncheckedTrackColor = p.surfaceAlt, uncheckedBorderColor = p.line, checkedBorderColor = p.line, uncheckedThumbColor = p.ink),
        )
    }
}
