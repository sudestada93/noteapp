package com.libreta.app

import android.app.Application
import com.libreta.app.data.LibretaDatabase
import com.libreta.app.data.Prefs
import com.libreta.app.data.Repository
import com.libreta.app.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LibretaApp : Application() {
    /** Ámbito de la aplicación para escrituras que deben sobrevivir a la navegación. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Repository(this, LibretaDatabase.build(this), Prefs(this))
        ReminderScheduler.createChannel(this)
        com.libreta.app.ui.components.setAttachmentsRoot(repository.attachmentsDir)
        appScope.launch {
            repository.ensureSeed()
            repository.purgeOldTrash()
        }
    }
}
