package jp.awt.clock

import android.app.Application
import jp.awt.clock.alarm.AlarmNotifications

class AwtApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AlarmNotifications.createChannel(this)
    }
}

