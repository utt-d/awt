package jp.awt.clock.ui

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.awt.clock.alarm.AlarmReceiver
import jp.awt.clock.alarm.AlarmService

class AlarmActivity : ComponentActivity(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var faceUpSeen = false
    private var faceDownSince = 0L
    private var commandSent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockScreen()
        sensorManager = getSystemService(SensorManager::class.java)
        val appearance = AppearancePreferences.read(this, AppearanceScreen.Alarms)
        setContent {
            AwtTheme(
                themeId = appearance.theme,
                numeralStyle = appearance.numeralStyle,
                backgroundTone = appearance.backgroundTone,
                customBackgroundArgb = appearance.customBackgroundArgb,
                customTextArgb = appearance.customTextArgb,
            ) {
                RingingScreen(
                    label = intent.getStringExtra(AlarmReceiver.EXTRA_LABEL).orEmpty().ifBlank { "アラーム" },
                    onSnooze = { sendCommand(AlarmService.ACTION_SNOOZE) },
                    onStop = { sendCommand(AlarmService.ACTION_STOP) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        sensorManager?.unregisterListener(this)
        super.onPause()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER || commandSent) return
        val z = event.values[2]
        if (z > 5.5f) {
            faceUpSeen = true
            faceDownSince = 0L
        } else if (faceUpSeen && z < -7.0f) {
            if (faceDownSince == 0L) faceDownSince = SystemClock.elapsedRealtime()
            if (SystemClock.elapsedRealtime() - faceDownSince >= 650L) {
                sendCommand(AlarmService.ACTION_SNOOZE)
            }
        } else {
            faceDownSince = 0L
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            sendCommand(AlarmService.ACTION_SNOOZE)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun configureLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // PhoneWindow has no DecorView yet at the start of onCreate on Android 16.
            // Defer access to the insets controller until the decor is attached.
            window.decorView.post {
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.systemBars())
                    controller.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        }
    }

    private fun sendCommand(actionName: String) {
        if (commandSent) return
        commandSent = true
        startService(
            Intent(this, AlarmService::class.java).apply {
                action = actionName
                putExtras(intent)
            },
        )
        finishAndRemoveTask()
    }
}

@Composable
private fun RingingScreen(label: String, onSnooze: () -> Unit, onStop: () -> Unit) {
    val colors = AwtThemeColors.current
    val transition = rememberInfiniteTransition(label = "alarmPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1_400), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(colors.backgroundTop, colors.night, colors.night),
                ),
            )
            .padding(32.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.Alarm,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = colors.secondary,
                )
                Text(
                    text = label,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = "おはようございます",
                    modifier = Modifier.padding(top = 8.dp),
                    color = colors.textMuted,
                    fontSize = 15.sp,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .scale(pulse)
                            .size(196.dp)
                            .background(colors.primary.copy(alpha = 0.14f), CircleShape),
                    )
                    Button(
                        onClick = onSnooze,
                        modifier = Modifier.size(168.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.Snooze,
                                contentDescription = null,
                                modifier = Modifier.size(34.dp),
                            )
                            Text(
                                "10分スヌーズ",
                                modifier = Modifier.padding(top = 8.dp),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.size(width = 220.dp, height = 58.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.alert),
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = null)
                    Text(
                        "アラームを停止",
                        modifier = Modifier.padding(start = 10.dp),
                        fontSize = 16.sp,
                    )
                }
                Text(
                    text = "音量キー、または端末を伏せてもスヌーズ",
                    modifier = Modifier.padding(top = 14.dp),
                    color = colors.textMuted.copy(alpha = 0.76f),
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}
