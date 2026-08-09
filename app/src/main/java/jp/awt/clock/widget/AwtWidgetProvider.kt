package jp.awt.clock.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import jp.awt.clock.R
import jp.awt.clock.alarm.NextAlarmCalculator
import jp.awt.clock.data.AlarmStore
import jp.awt.clock.data.repeatSummary
import jp.awt.clock.ui.MainActivity
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class AwtWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { manager.updateAppWidget(it, views(context)) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, AwtWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            ids.forEach { manager.updateAppWidget(it, views(context)) }
        }

        private fun views(context: Context): RemoteViews {
            val nextAlarm = AlarmStore(context).use { store ->
                store.all()
                    .filter { it.enabled }
                    .minByOrNull { NextAlarmCalculator.next(it).toInstant() }
            }
            val remote = RemoteViews(context.packageName, R.layout.awt_widget)
            if (nextAlarm == null) {
                remote.setTextViewText(R.id.widget_time, "--:--")
                remote.setTextViewText(R.id.widget_label, "アラームはありません")
                remote.setTextViewText(R.id.widget_next, "タップして追加")
            } else {
                val next = NextAlarmCalculator.next(nextAlarm, ZonedDateTime.now())
                remote.setTextViewText(R.id.widget_time, nextAlarm.timeText)
                remote.setTextViewText(R.id.widget_label, nextAlarm.displayLabel)
                remote.setTextViewText(
                    R.id.widget_next,
                    "${next.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}  ${next.format(DateTimeFormatter.ofPattern("M/d"))}  ·  ${repeatSummary(nextAlarm.repeatMask)}",
                )
            }
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            remote.setOnClickPendingIntent(R.id.widget_root, open)
            return remote
        }
    }
}

