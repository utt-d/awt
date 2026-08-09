package jp.awt.clock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import jp.awt.clock.data.AlarmStore
import jp.awt.clock.widget.AwtWidgetProvider

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
        val isTransient = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false) ||
            intent.getBooleanExtra(EXTRA_IS_TIMER, false)

        val stored = if (isTransient) null else AlarmStore(context).use { it.get(id) }
        if (!isTransient && (stored == null || !stored.enabled)) return

        if (stored != null) {
            if (stored.repeatMask == 0) {
                AlarmStore(context).use { it.setEnabled(id, false) }
            } else {
                AlarmScheduler(context).schedule(stored)
            }
            AwtWidgetProvider.updateAll(context)
        }

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START
            putExtra(EXTRA_ALARM_ID, id)
            putExtra(EXTRA_LABEL, stored?.displayLabel ?: intent.getStringExtra(EXTRA_LABEL).orEmpty())
            putExtra(EXTRA_GRADUAL, stored?.gradualVolume ?: intent.getBooleanExtra(EXTRA_GRADUAL, false))
            putExtra(EXTRA_VIBRATE, stored?.vibrate ?: intent.getBooleanExtra(EXTRA_VIBRATE, true))
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        const val ACTION_FIRE = "jp.awt.clock.action.FIRE"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_LABEL = "label"
        const val EXTRA_GRADUAL = "gradual"
        const val EXTRA_VIBRATE = "vibrate"
        const val EXTRA_IS_SNOOZE = "is_snooze"
        const val EXTRA_IS_TIMER = "is_timer"
    }
}
