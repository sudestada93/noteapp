package com.libreta.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.libreta.app.MainActivity
import com.libreta.app.R

/** Widget de captura rápida para la pantalla de inicio. */
class QuickWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_quick)
            fun bind(viewId: Int, action: String) {
                val i = Intent(context, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_ACTION, action)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                views.setOnClickPendingIntent(viewId, PendingIntent.getActivity(context, action.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
            bind(R.id.w_note, "note")
            bind(R.id.w_task, "task")
            bind(R.id.w_audio, "audio")
            bind(R.id.w_camera, "camera")
            bind(R.id.w_search, "search")
            manager.updateAppWidget(id, views)
        }
    }
}
