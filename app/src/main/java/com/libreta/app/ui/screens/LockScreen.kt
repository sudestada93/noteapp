package com.libreta.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.libreta.app.ui.components.Doodle
import com.libreta.app.ui.components.DoodleArt
import com.libreta.app.ui.components.PinPad
import com.libreta.app.ui.components.rememberApp
import com.libreta.app.ui.components.settingsState
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.util.Biometric

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val p = LocalPalette.current
    val repo = rememberApp().repository
    val settings by settingsState()
    val context = LocalContext.current
    val bio = settings.biometric && Biometric.available(context)
    LaunchedEffect(bio) { if (bio) Biometric.prompt(context) { onUnlock() } }
    Column(
        Modifier.fillMaxSize().background(p.canvas).systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        DoodleArt(Doodle.LOCK, size = 110.dp)
        Spacer(Modifier.height(10.dp))
        PinPad("Libreta bloqueada", "Ingresá tu PIN", onBiometric = if (bio) ({ Biometric.prompt(context) { onUnlock() } }) else null) { pin ->
            repo.prefs.checkPin(pin, settings).also { if (it) onUnlock() }
        }
    }
}
