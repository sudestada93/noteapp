package com.libreta.app.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.libreta.app.data.NoteEntity
import com.libreta.app.data.TaskEntity

object ReminderScheduler {
    const val CHANNEL = "reminders"
    const val EXTRA_KIND = "kind"
    const val EXTRA_ID = "id"
    const val KIND_NOTE = "note"
    const val KIND_TASK = "task"

    fun createChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CHANNEL, "Recordatorios", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Recordatorios de notas y tareas"
            enableVibration(true)
        }
        nm.createNotificationChannel(ch)
    }

    private fun pending(context: Context, kind: String, id: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.libreta.app.REMIND.$kind.$id"
            putExtra(EXTRA_KIND, kind)
            putExtra(EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(
            context, (kind + id).hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun schedule(context: Context, kind: String, id: String, at: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context, kind, id)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(context: Context, kind: String, id: String) {
        context.getSystemService(AlarmManager::class.java).cancel(pending(context, kind, id))
    }

    fun cancelNote(context: Context, id: String) = cancel(context, KIND_NOTE, id)
    fun cancelTask(context: Context, id: String) = cancel(context, KIND_TASK, id)

    fun syncNote(context: Context, n: NoteEntity) {
        val at = n.reminderAt
        if (at != null && !n.reminderDone && !n.isTrashed && at > System.currentTimeMillis()) schedule(context, KIND_NOTE, n.id, at)
        else cancelNote(context, n.id)
    }

    fun syncTask(context: Context, t: TaskEntity) {
        val at = t.reminderAt
        if (at != null && !t.done && at > System.currentTimeMillis()) schedule(context, KIND_TASK, t.id, at)
        else cancelTask(context, t.id)
    }
}
