package jp.awt.clock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import jp.awt.clock.data.AlarmStore
import jp.awt.clock.widget.AwtWidgetProvider

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = AlarmScheduler(context)
        AwtWidgetProvider.updateAll(context)
        if (!scheduler.canScheduleExact()) return
        AlarmStore(context).use { store ->
            store.all().filter { it.enabled }.forEach(scheduler::schedule)
        }
    }
}
