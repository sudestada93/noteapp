package com.libreta.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.libreta.app.data.Settings
import com.libreta.app.model.Block
import com.libreta.app.ui.AppRoot
import com.libreta.app.ui.External
import com.libreta.app.ui.screens.LockScreen
import com.libreta.app.ui.screens.WelcomeScreen
import com.libreta.app.ui.theme.LibretaTheme
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.util.Clipper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
    companion object {
        const val EXTRA_OPEN_NOTE = "open_note"
        const val EXTRA_ACTION = "action"
        const val ACTION_TASKS = "tasks"
    }

    private var external by mutableStateOf<External?>(null)
    private var settings by mutableStateOf<Settings?>(null)
    private var locked by mutableStateOf(false)
    private var backgroundAt = 0L
    private var firstSettings = true

    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val app = application as LibretaApp
        lifecycleScope.launch {
            app.repository.prefs.settings.collect { s ->
                if (firstSettings) {
                    firstSettings = false
                    if (s.lockEnabled && savedInstanceState == null) locked = true
                }
                if (!s.lockEnabled) locked = false
                settings = s
            }
        }
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            val s = settings
            LibretaTheme(themeMode = s?.themeMode ?: 0) {
                val p = LocalPalette.current
                LaunchedEffect(p.dark) {
                    enableEdgeToEdge(
                        statusBarStyle = if (p.dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                        navigationBarStyle = if (p.dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                    )
                }
                Box(Modifier.fillMaxSize().background(p.canvas)) {
                    if (s != null) {
                        if (!s.onboarded) {
                            WelcomeScreen { name ->
                                lifecycleScope.launch {
                                    app.repository.prefs.setUserName(name)
                                    app.repository.prefs.setOnboarded()
                                }
                                askNotifications()
                            }
                        } else {
                            AppRoot(external) { external = null }
                        }
                        AnimatedVisibility(locked && s.lockEnabled, enter = fadeIn(), exit = fadeOut()) {
                            LockScreen { locked = false }
                        }
                    }
                }
            }
        }
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundAt = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        val s = settings ?: return
        if (s.lockEnabled && backgroundAt > 0) {
            val grace = maxOf(s.lockTimeoutMin * 60_000L, 20_000L)
            if (System.currentTimeMillis() - backgroundAt > grace) locked = true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        intent.getStringExtra(EXTRA_OPEN_NOTE)?.let { external = External.OpenNote(it); return }
        intent.getStringExtra(EXTRA_ACTION)?.let { external = External.Action(it); return }
        when (intent.action) {
            "com.libreta.app.action.NOTE" -> external = External.Action("note")
            "com.libreta.app.action.TASK" -> external = External.Action("task")
            "com.libreta.app.action.AUDIO" -> external = External.Action("audio")
            "com.libreta.app.action.CAMERA" -> external = External.Action("camera")
            "com.libreta.app.action.SCAN" -> external = External.Action("scan")
            "com.libreta.app.action.SEARCH" -> external = External.Action("search")
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> handleShare(intent)
        }
    }

    private fun handleShare(intent: Intent) {
        val app = application as LibretaApp
        val repo = app.repository
        val type = intent.type ?: ""
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        val uris: List<Uri> = if (intent.action == Intent.ACTION_SEND) {
            listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
        } else {
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        }
        lifecycleScope.launch {
            val id = withContext(Dispatchers.IO) {
                if (uris.isEmpty() && type.startsWith("text/") && text != null) {
                    if (Clipper.findUrl(text) != null) {
                        withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "✂ Recortando página…", Toast.LENGTH_SHORT).show() }
                    }
                    val clip = Clipper.clip(repo, text, subject)
                    repo.createNote(title = clip.title, blocks = clip.blocks + Block(), sourceUrl = clip.url.ifBlank { null })
                } else {
                    val blocks = uris.mapNotNull { repo.importAttachment(it) }
                    val extra = text?.let { listOf(Block(text = it)) } ?: emptyList()
                    repo.createNote(title = subject ?: blocks.firstOrNull()?.name ?: "Compartido", blocks = blocks + extra + Block())
                }
            }
            external = External.OpenNote(id)
        }
    }
}
