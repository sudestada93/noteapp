package com.libreta.app.reminders

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.libreta.app.LibretaApp
import com.libreta.app.MainActivity
import com.libreta.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(ReminderScheduler.EXTRA_KIND) ?: return
        val id = intent.getStringExtra(ReminderScheduler.EXTRA_ID) ?: return
        val repo = (context.applicationContext as LibretaApp).repository
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val (title, text) = if (kind == ReminderScheduler.KIND_NOTE) {
                    val n = repo.getNote(id) ?: return@launch
                    if (n.reminderDone || n.isTrashed) return@launch
                    (n.title.ifBlank { "Nota sin título" }) to (n.preview.ifBlank { "Recordatorio de nota" })
                } else {
                    val t = repo.getTask(id) ?: return@launch
                    if (t.done) return@launch
                    t.title to (if (t.flagged) "🚩 Tarea marcada" else "Tarea pendiente")
                }
                notify(context, kind, id, title, text)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        fun notify(context: Context, kind: String, id: String, title: String, text: String) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            val notifId = (kind + id).hashCode()
            val open = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (kind == ReminderScheduler.KIND_NOTE) putExtra(MainActivity.EXTRA_OPEN_NOTE, id)
                else putExtra(MainActivity.EXTRA_ACTION, MainActivity.ACTION_TASKS)
            }
            val openPi = PendingIntent.getActivity(context, notifId, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            fun action(a: String): PendingIntent {
                val i = Intent(context, ReminderActionReceiver::class.java).apply {
                    action = "com.libreta.app.$a.$kind.$id"
                    putExtra("op", a)
                    putExtra(ReminderScheduler.EXTRA_KIND, kind)
                    putExtra(ReminderScheduler.EXTRA_ID, id)
                }
                return PendingIntent.getBroadcast(context, (a + kind + id).hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
            val n = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_libreta)
                .setColor(0xFF4A2C17.toInt())
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openPi)
                .addAction(0, "Hecho", action("DONE"))
                .addAction(0, "Posponer 10 min", action("SNOOZE"))
                .build()
            context.getSystemService(NotificationManager::class.java).notify(notifId, n)
        }
    }
}

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(ReminderScheduler.EXTRA_KIND) ?: return
        val id = intent.getStringExtra(ReminderScheduler.EXTRA_ID) ?: return
        val op = intent.getStringExtra("op") ?: return
        context.getSystemService(NotificationManager::class.java).cancel((kind + id).hashCode())
        val repo = (context.applicationContext as LibretaApp).repository
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val later = System.currentTimeMillis() + 10 * 60_000L
                if (kind == ReminderScheduler.KIND_NOTE) {
                    if (op == "DONE") repo.completeReminder(id) else repo.setReminder(id, later)
                } else {
                    val t = repo.getTask(id) ?: return@launch
                    if (op == "DONE") repo.saveTask(t.copy(done = true)) else repo.saveTask(t.copy(reminderAt = later))
                }
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as LibretaApp
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                app.repository.db.notes().pendingReminders().forEach { ReminderScheduler.syncNote(context, it) }
                app.repository.db.tasks().pendingReminders().forEach { ReminderScheduler.syncTask(context, it) }
            } finally {
                pending.finish()
            }
        }
    }
}
