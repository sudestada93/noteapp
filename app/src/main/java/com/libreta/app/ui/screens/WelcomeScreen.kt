package com.libreta.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.libreta.app.ui.components.Art
import com.libreta.app.ui.components.BrutalTextField
import com.libreta.app.ui.components.Engraving
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T

@Composable
fun WelcomeScreen(onStart: (String) -> Unit) {
    val p = LocalPalette.current
    var name by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().background(p.canvas).systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Engraving(Art.BOOKS, Modifier.fillMaxWidth().height(210.dp))
        Spacer(Modifier.height(44.dp))
        Text(
            buildAnnotatedString {
                append("“La memoria es frágil;\nla ")
                withStyle(SpanStyle(background = p.canary.copy(alpha = 0.85f))) { append("tinta") }
                append(", paciente.”")
            },
            style = T.display.copy(fontWeight = FontWeight.Normal, fontSize = T.display.fontSize * 0.95f, lineHeight = T.display.lineHeight * 1.08f),
            color = p.ink,
        )
        Spacer(Modifier.height(18.dp))
        Text("— Libreta", style = T.subheading.copy(fontWeight = FontWeight.Normal), color = p.inkSoft)
        Spacer(Modifier.height(40.dp))
        Text("¿Cómo te llamás?", style = T.micro, color = p.inkFaint)
        BrutalTextField(name, { name = it }, placeholder = "Tu nombre (opcional)", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(44.dp))
        Row(
            Modifier.clickable { onStart(name.trim()) }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Text("Empezar", style = T.heading.copy(fontWeight = FontWeight.Normal), color = p.ink)
            Spacer(Modifier.width(16.dp))
            Box(Modifier.width(70.dp).height(1.dp).background(p.ink))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = p.ink, modifier = Modifier.padding(start = 0.dp))
        }
        Spacer(Modifier.height(40.dp))
    }
}
