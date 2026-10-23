package com.libreta.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.libreta.app.LibretaApp
import com.libreta.app.data.Settings

@Composable
fun rememberApp(): LibretaApp = LocalContext.current.applicationContext as LibretaApp

@Composable
fun settingsState(): State<Settings> = rememberApp().repository.prefs.settings.collectAsStateWithLifecycle(initialValue = Settings(onboarded = true))
