package com.libreta.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.libreta.app.ui.components.BrutalButton
import com.libreta.app.ui.components.BrutalCard
import com.libreta.app.ui.components.BrutalTextField
import com.libreta.app.ui.components.Doodle
import com.libreta.app.ui.components.DoodleArt
import com.libreta.app.ui.components.Marquee
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T

@Composable
fun WelcomeScreen(onStart: (String) -> Unit) {
    val p = LocalPalette.current
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(p.canvas).systemBarsPadding().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            DoodleArt(Doodle.NOTEBOOK, size = 120.dp, modifier = Modifier.rotate(-6f))
            DoodleArt(Doodle.TASKS, size = 100.dp, modifier = Modifier.rotate(5f))
        }
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text("LIBRETA", style = T.micro, color = p.inkSoft)
            Spacer(Modifier.height(8.dp))
            Text("TU SEGUNDO\nCEREBRO,\nEN PAPEL.", style = T.display.copy(fontSize = T.display.fontSize * 1.15f, lineHeight = T.display.lineHeight * 1.1f), color = p.ink)
            Spacer(Modifier.height(14.dp))
            Text("Notas enriquecidas, tareas, recordatorios, escáner, audio, bocetos y búsqueda instantánea. Todo guardado en tu teléfono.", style = T.body, color = p.inkSoft)
        }
        Spacer(Modifier.height(24.dp))
        Marquee("NOTAS · TAREAS · LIBRETAS · ETIQUETAS · ESCÁNER · AUDIO · BOCETOS · RECORDATORIOS")
        Spacer(Modifier.height(24.dp))
        BrutalCard(Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
            Text("¿CÓMO TE LLAMÁS?", style = T.label, color = p.ink)
            Spacer(Modifier.height(10.dp))
            BrutalTextField(name, { name = it }, placeholder = "Tu nombre (opcional)", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            BrutalButton("Empezar", { onStart(name.trim()) }, icon = Icons.AutoMirrored.Filled.ArrowForward, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(32.dp))
    }
}
