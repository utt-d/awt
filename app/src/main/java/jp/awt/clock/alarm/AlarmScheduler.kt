package jp.awt.clock.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import jp.awt.clock.data.Alarm
import jp.awt.clock.ui.MainActivity

class AlarmScheduler(private val context: Context) {
    private val manager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()

    fun schedule(alarm: Alarm): Boolean {
        cancel(alarm.id)
        if (!alarm.enabled || !canScheduleExact()) return false

        val next = NextAlarmCalculator.next(alarm).toInstant().toEpochMilli()
        val operation = PendingIntent.getBroadcast(
            context,
            alarm.id.requestCode(),
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_FIRE
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val show = PendingIntent.getActivity(
            context,
            alarm.id.requestCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(next, show), operation)
        return true
    }

    fun cancel(id: Long) {
        manager.cancel(
            PendingIntent.getBroadcast(
                context,
                id.requestCode(),
                Intent(context, AlarmReceiver::class.java).apply { action = AlarmReceiver.ACTION_FIRE },
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ) ?: return,
        )
    }

    fun scheduleSnooze(id: Long, label: String, gradualVolume: Boolean, vibrate: Boolean) {
        if (!canScheduleExact()) return
        val operation = PendingIntent.getBroadcast(
            context,
            id.snoozeRequestCode(),
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_FIRE
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, id)
                putExtra(AlarmReceiver.EXTRA_LABEL, label)
                putExtra(AlarmReceiver.EXTRA_GRADUAL, gradualVolume)
                putExtra(AlarmReceiver.EXTRA_VIBRATE, vibrate)
                putExtra(AlarmReceiver.EXTRA_IS_SNOOZE, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.setAlarmClock(
            AlarmManager.AlarmClockInfo(System.currentTimeMillis() + SNOOZE_MILLIS, null),
            operation,
        )
    }

    fun scheduleTimer(endAtMillis: Long): Boolean {
        cancelTimer()
        if (!canScheduleExact()) return false
        val operation = PendingIntent.getBroadcast(
            context,
            TIMER_REQUEST_CODE,
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_FIRE
                putExtra(AlarmReceiver.EXTRA_IS_TIMER, true)
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, TIMER_ID)
                putExtra(AlarmReceiver.EXTRA_LABEL, "タイマー")
                putExtra(AlarmReceiver.EXTRA_GRADUAL, false)
                putExtra(AlarmReceiver.EXTRA_VIBRATE, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(endAtMillis, null), operation)
        return true
    }

    fun cancelTimer() {
        val pending = PendingIntent.getBroadcast(
            context,
            TIMER_REQUEST_CODE,
            Intent(context, AlarmReceiver::class.java).apply { action = AlarmReceiver.ACTION_FIRE },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        manager.cancel(pending)
        pending.cancel()
    }

    private fun Long.requestCode(): Int = hashCode() and 0x3fffffff
    private fun Long.snoozeRequestCode(): Int = requestCode() xor 0x5a5a0000

    companion object {
        const val TIMER_ID = -10_001L
        private const val TIMER_REQUEST_CODE = 0x0a170001
        private const val SNOOZE_MILLIS = 10 * 60 * 1000L
    }
}

