package jp.awt.clock.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import jp.awt.clock.alarm.AlarmScheduler
import jp.awt.clock.alarm.NextAlarmCalculator
import jp.awt.clock.data.Alarm
import jp.awt.clock.data.AlarmStore
import jp.awt.clock.data.dayLabels
import jp.awt.clock.data.repeatSummary
import jp.awt.clock.widget.AwtWidgetProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private lateinit var store: AlarmStore
    private lateinit var scheduler: AlarmScheduler
    private var alarms by mutableStateOf<List<Alarm>>(emptyList())
    private var timerEnd by mutableLongStateOf(0L)
    private var timerDuration by mutableLongStateOf(0L)
    private var timerPausedRemaining by mutableLongStateOf(0L)
    private var resumeTick by mutableIntStateOf(0)
    private var appResumed by mutableStateOf(false)
    private var appearances by mutableStateOf(AppearanceProfiles())
    private var activeAppearanceScreen by mutableStateOf(AppearanceScreen.Alarms)

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { resumeTick++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        hideStatusBar()
        store = AlarmStore(this)
        scheduler = AlarmScheduler(this)
        alarms = store.all()
        val statePreferences = getSharedPreferences(PREFS, MODE_PRIVATE)
        timerEnd = statePreferences.getLong(KEY_TIMER_END, 0L)
        timerDuration = statePreferences.getLong(KEY_TIMER_DURATION, 0L)
        timerPausedRemaining = statePreferences.getLong(KEY_TIMER_PAUSED_REMAINING, 0L)
        if (timerEnd > System.currentTimeMillis() && timerDuration <= 0L) {
            timerDuration = timerEnd - System.currentTimeMillis()
            statePreferences.edit().putLong(KEY_TIMER_DURATION, timerDuration).apply()
        }
        appearances = AppearancePreferences.readAll(this)

        setContent {
            val appearance = appearances[activeAppearanceScreen]
            AwtTheme(
                themeId = appearance.theme,
                numeralStyle = appearance.numeralStyle,
                backgroundTone = appearance.backgroundTone,
                customBackgroundArgb = appearance.customBackgroundArgb,
                customTextArgb = appearance.customTextArgb,
            ) {
                AwtApp(
                    alarms = alarms,
                    timerEnd = timerEnd,
                    timerDuration = timerDuration,
                    timerPausedRemaining = timerPausedRemaining,
                    permissionTick = resumeTick,
                    appResumed = appResumed,
                    appearances = appearances,
                    onActiveAppearanceScreenChange = { activeAppearanceScreen = it },
                    onAppearanceChange = ::saveAppearance,
                    onSave = ::saveAlarm,
                    onToggle = ::toggleAlarm,
                    onDelete = ::deleteAlarm,
                    onManagePermission = ::openNextRequiredPermission,
                    onStartTimer = ::startTimer,
                    onPauseTimer = ::pauseTimer,
                    onResumeTimer = ::resumeTimer,
                    onCancelTimer = ::cancelTimer,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideStatusBar()
        appResumed = true
        resumeTick++
        if (::scheduler.isInitialized && scheduler.canScheduleExact()) {
            store.all().filter { it.enabled }.forEach(scheduler::schedule)
        }
    }

    override fun onPause() {
        appResumed = false
        super.onPause()
    }

    private fun hideStatusBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() {
        if (::store.isInitialized) store.close()
        super.onDestroy()
    }

    private fun saveAlarm(alarm: Alarm) {
        val saved = store.save(alarm)
        scheduler.schedule(saved)
        alarms = store.all()
        AwtWidgetProvider.updateAll(this)
    }

    private fun toggleAlarm(alarm: Alarm, enabled: Boolean) {
        val updated = alarm.copy(enabled = enabled)
        store.save(updated)
        if (enabled) scheduler.schedule(updated) else scheduler.cancel(alarm.id)
        alarms = store.all()
        AwtWidgetProvider.updateAll(this)
    }

    private fun deleteAlarm(alarm: Alarm) {
        scheduler.cancel(alarm.id)
        store.delete(alarm.id)
        alarms = store.all()
        AwtWidgetProvider.updateAll(this)
    }

    private fun startTimer(durationMillis: Long): Boolean {
        if (durationMillis < 1_000L) return false
        val end = System.currentTimeMillis() + durationMillis
        if (!scheduler.scheduleTimer(end)) return false
        timerEnd = end
        timerDuration = durationMillis
        timerPausedRemaining = 0L
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putLong(KEY_TIMER_END, end)
            .putLong(KEY_TIMER_DURATION, durationMillis)
            .remove(KEY_TIMER_PAUSED_REMAINING)
            .apply()
        return true
    }

    private fun pauseTimer(remainingMillis: Long) {
        val remaining = remainingMillis.coerceAtLeast(0L)
        if (remaining == 0L) {
            cancelTimer()
            return
        }
        scheduler.cancelTimer()
        timerEnd = 0L
        timerPausedRemaining = remaining
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .remove(KEY_TIMER_END)
            .putLong(KEY_TIMER_DURATION, timerDuration.coerceAtLeast(remaining))
            .putLong(KEY_TIMER_PAUSED_REMAINING, remaining)
            .apply()
    }

    private fun resumeTimer(): Boolean {
        val remaining = timerPausedRemaining
        if (remaining < 1_000L) return false
        val end = System.currentTimeMillis() + remaining
        if (!scheduler.scheduleTimer(end)) return false
        timerEnd = end
        timerPausedRemaining = 0L
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putLong(KEY_TIMER_END, end)
            .remove(KEY_TIMER_PAUSED_REMAINING)
            .apply()
        return true
    }

    private fun cancelTimer() {
        scheduler.cancelTimer()
        timerEnd = 0L
        timerDuration = 0L
        timerPausedRemaining = 0L
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .remove(KEY_TIMER_END)
            .remove(KEY_TIMER_DURATION)
            .remove(KEY_TIMER_PAUSED_REMAINING)
            .apply()
    }

    private fun saveAppearance(
        screen: AppearanceScreen,
        settings: AppearanceSettings,
    ) {
        appearances = appearances.updated(screen, settings)
        AppearancePreferences.write(this, screen, settings)
    }

    private fun openNextRequiredPermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED -> notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !scheduler.canScheduleExact() -> {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:$packageName"),
                    ),
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                !getSystemService(NotificationManager::class.java).canUseFullScreenIntent() -> {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:$packageName"),
                    ),
                )
            }
        }
    }

    private companion object {
        const val PREFS = "awt_state"
        const val KEY_TIMER_END = "timer_end"
        const val KEY_TIMER_DURATION = "timer_duration"
        const val KEY_TIMER_PAUSED_REMAINING = "timer_paused_remaining"
    }
}

private enum class AppTab { Alarms, Clock, Measure }
private enum class MeasureMode { Timer, Stopwatch }

private val AppTab.appearanceScreen: AppearanceScreen
    get() = when (this) {
        AppTab.Alarms -> AppearanceScreen.Alarms
        AppTab.Clock -> AppearanceScreen.Clock
        AppTab.Measure -> AppearanceScreen.Measure
    }

@Composable
private fun AwtApp(
    alarms: List<Alarm>,
    timerEnd: Long,
    timerDuration: Long,
    timerPausedRemaining: Long,
    permissionTick: Int,
    appResumed: Boolean,
    appearances: AppearanceProfiles,
    onActiveAppearanceScreenChange: (AppearanceScreen) -> Unit,
    onAppearanceChange: (AppearanceScreen, AppearanceSettings) -> Unit,
    onSave: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    onDelete: (Alarm) -> Unit,
    onManagePermission: () -> Unit,
    onStartTimer: (Long) -> Boolean,
    onPauseTimer: (Long) -> Unit,
    onResumeTimer: () -> Boolean,
    onCancelTimer: () -> Unit,
) {
    val context = LocalContext.current
    val colors = AwtThemeColors.current
    val pagerState = rememberPagerState(pageCount = { AppTab.entries.size })
    val pagerScope = rememberCoroutineScope()
    val selectedTab = AppTab.entries[pagerState.currentPage]
    val selectedAppearanceScreen = selectedTab.appearanceScreen
    val appearance = appearances[selectedAppearanceScreen]
    val pagePosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var creatingAlarm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Alarm?>(null) }
    var appearanceDialogScreen by remember { mutableStateOf<AppearanceScreen?>(null) }
    val powerSaveMode = remember(permissionTick, appResumed) {
        context.getSystemService(PowerManager::class.java).isPowerSaveMode
    }
    val suggestedMinutes = remember(creatingAlarm) {
        val suggested = LocalDateTime.now().plusMinutes(5)
        ((suggested.hour * 60 + suggested.minute + 4) / 5 * 5).floorDay()
    }
    val editorTarget = when {
        creatingAlarm -> Alarm(
            hour = suggestedMinutes / 60,
            minute = suggestedMinutes % 60,
        )
        else -> editingAlarm
    }

    LaunchedEffect(selectedAppearanceScreen) {
        onActiveAppearanceScreenChange(selectedAppearanceScreen)
    }

    if (editorTarget != null) {
        BackHandler {
            creatingAlarm = false
            editingAlarm = null
        }
        AlarmEditor(
            initial = editorTarget,
            reduceMotion = appearances.alarms.reduceMotion,
            onDismiss = {
                creatingAlarm = false
                editingAlarm = null
            },
            onSave = {
                onSave(it)
                creatingAlarm = false
                editingAlarm = null
            },
        )
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
        bottomBar = {
            AwtNavigation(selectedTab) { tab ->
                pagerScope.launch {
                    pagerState.animateScrollToPage(AppTab.entries.indexOf(tab))
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == AppTab.Alarms) {
                ExtendedFloatingActionButton(
                    onClick = { creatingAlarm = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("追加") },
                    containerColor = colors.primary,
                    contentColor = colors.night,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp),
                )
            }
        },
    ) { padding ->
        AwtMotionBackdrop(
            scene = appearance.motionScene,
            animate = appResumed &&
                appearance.motionScene != MotionScene.Still &&
                !appearance.reduceMotion &&
                !powerSaveMode,
            pagePosition = pagePosition,
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { AppTab.entries[it] },
                ) { page ->
                    val tab = AppTab.entries[page]
                    val pageAppearance = appearances[tab.appearanceScreen]
                    val pageOffset = (
                        pagerState.currentPage - page +
                            pagerState.currentPageOffsetFraction
                        ).coerceIn(-1f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = pageOffset * size.width * 0.12f
                                alpha = 1f - abs(pageOffset) * 0.06f
                            },
                    ) {
                        when (tab) {
                            AppTab.Alarms -> Column(Modifier.fillMaxSize()) {
                                ScreenTitle("アラーム")
                                PermissionNotice(permissionTick, onManagePermission)
                                AlarmList(
                                    alarms = alarms,
                                    onToggle = onToggle,
                                    onEdit = { editingAlarm = it },
                                    onDelete = { pendingDelete = it },
                                )
                            }
                            AppTab.Clock -> ClockScreen(
                                alarms = alarms,
                                appResumed = appResumed && selectedTab == AppTab.Clock,
                                appearance = pageAppearance,
                                powerSaveMode = powerSaveMode,
                            )
                            AppTab.Measure -> MeasureScreen(
                                timerEnd = timerEnd,
                                timerDuration = timerDuration,
                                timerPausedRemaining = timerPausedRemaining,
                                appResumed = appResumed && selectedTab == AppTab.Measure,
                                onStartTimer = onStartTimer,
                                onPauseTimer = onPauseTimer,
                                onResumeTimer = onResumeTimer,
                                onCancelTimer = onCancelTimer,
                                reduceMotion = pageAppearance.reduceMotion,
                                powerSaveMode = powerSaveMode,
                            )
                        }
                        AppearanceButton(
                            onClick = { appearanceDialogScreen = tab.appearanceScreen },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(bottom = if (tab == AppTab.Measure) 72.dp else 0.dp),
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { alarm ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("アラームを削除しますか？") },
            text = { Text("${alarm.timeText}「${alarm.displayLabel}」は元に戻せません。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(alarm)
                        pendingDelete = null
                    },
                ) {
                    Text("削除", color = colors.alert, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("キャンセル") }
            },
        )
    }

    appearanceDialogScreen?.let { screen ->
        AppearanceDialog(
            screen = screen,
            appearance = appearances[screen],
            powerSaveMode = powerSaveMode,
            onChange = { settings -> onAppearanceChange(screen, settings) },
            onDismiss = { appearanceDialogScreen = null },
        )
    }
}

@Composable
private fun ScreenTitle(title: String) {
    Text(
        text = title,
        color = AwtThemeColors.current.textPrimary,
        fontSize = 30.sp,
        fontWeight = FontWeight.Light,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
    )
}

@Composable
private fun PermissionNotice(permissionTick: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    val colors = AwtThemeColors.current
    val scheduler = remember(permissionTick) { AlarmScheduler(context) }
    val missingNotification = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    val missingExact = !scheduler.canScheduleExact()
    val missingFullScreen = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        !context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    val message = when {
        missingNotification -> "通知を許可して、アラームを表示できるようにしてください"
        missingExact -> "正確な時刻に鳴らすため、アラーム権限を許可してください"
        missingFullScreen -> "ロック画面に表示するため、全画面通知を許可してください"
        else -> null
    }

    AnimatedVisibility(message != null) {
        if (message != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable(onClick = onClick),
                colors = CardDefaults.cardColors(containerColor = colors.secondary.copy(alpha = 0.13f)),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = colors.secondary)
                    Text(
                        text = message,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        fontSize = 13.sp,
                    )
                    Text("設定", color = colors.secondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AwtNavigation(selected: AppTab, onSelect: (AppTab) -> Unit) {
    val colors = AwtThemeColors.current
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = colors.primary,
        selectedTextColor = colors.secondary,
        indicatorColor = colors.primary.copy(alpha = 0.18f),
        unselectedIconColor = colors.textMuted,
        unselectedTextColor = colors.textMuted,
    )
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = colors.nightSoft.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
    ) {
        NavigationBarItem(
            selected = selected == AppTab.Alarms,
            onClick = { onSelect(AppTab.Alarms) },
            icon = { Icon(Icons.Rounded.Alarm, contentDescription = null) },
            label = { Text("アラーム") },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selected == AppTab.Clock,
            onClick = { onSelect(AppTab.Clock) },
            icon = { Icon(Icons.Rounded.AccessTime, contentDescription = null) },
            label = { Text("時計") },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selected == AppTab.Measure,
            onClick = { onSelect(AppTab.Measure) },
            icon = { Icon(Icons.Rounded.Timer, contentDescription = null) },
            label = { Text("計測") },
            colors = itemColors,
        )
    }
}

@Composable
private fun AppearanceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwtThemeColors.current
    IconButton(
        onClick = onClick,
        modifier = modifier
            .padding(start = 0.dp, bottom = 6.dp)
            .size(48.dp),
    ) {
        Icon(
            Icons.Rounded.Palette,
            contentDescription = "この画面の外観設定",
            tint = colors.textMuted.copy(alpha = 0.58f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ClockScreen(
    alarms: List<Alarm>,
    appResumed: Boolean,
    appearance: AppearanceSettings,
    powerSaveMode: Boolean,
) {
    val colors = AwtThemeColors.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(appResumed, appearance.reduceMotion, powerSaveMode) {
        now = LocalDateTime.now()
        val tickMillis = if (appearance.reduceMotion || powerSaveMode) 1_000L else 33L
        while (appResumed) {
            delay(tickMillis - System.currentTimeMillis() % tickMillis)
            now = LocalDateTime.now()
        }
    }
    val nextAlarm = remember(alarms, now.minute) {
        alarms
            .filter { it.enabled }
            .map { alarm -> alarm to NextAlarmCalculator.next(alarm, ZonedDateTime.now()) }
            .minByOrNull { it.second.toInstant() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ClockFace(
                style = appearance.clockFace,
                now = now,
                reduceMotion = appearance.reduceMotion,
                animateEffects = appResumed && !appearance.reduceMotion && !powerSaveMode,
                showHalo = appearance.showClockHalo,
            )
            Text(
                text = now.format(DateTimeFormatter.ofPattern("M月d日（E）", Locale.JAPAN)),
                modifier = Modifier.padding(top = 16.dp),
                color = colors.textMuted,
                fontSize = 16.sp,
            )
            nextAlarm?.let { (alarm, occurrence) ->
                Surface(
                    modifier = Modifier.padding(top = 24.dp),
                    color = colors.nightSoft.copy(alpha = 0.66f),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Alarm, contentDescription = null, tint = colors.primary)
                        Column(Modifier.padding(start = 14.dp)) {
                            Text("次のアラーム", color = colors.textMuted, fontSize = 12.sp)
                            Text(
                                "${occurrence.format(DateTimeFormatter.ofPattern("E H:mm", Locale.JAPAN))}  ${alarm.displayLabel}",
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClockFace(
    style: ClockFaceStyle,
    now: LocalDateTime,
    reduceMotion: Boolean,
    animateEffects: Boolean,
    showHalo: Boolean,
) {
    val colors = AwtThemeColors.current
    val secondProgress = now.second + if (reduceMotion) 0f else now.nano / 1_000_000_000f
    val oneSecondOrbit = if (reduceMotion) 0f else now.nano / 1_000_000_000f
    Box(
        modifier = Modifier.size(344.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (showHalo) {
            TimeProgressHalo(
                modifier = Modifier.fillMaxSize(),
                primary = colors.primary,
                orbitProgress = oneSecondOrbit,
                animate = animateEffects,
            )
        }
        when (style) {
            ClockFaceStyle.Digital -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RollingTimeText(
                value = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                color = colors.textPrimary,
                fontSize = 94.sp,
                fontWeight = FontWeight.Light,
                reduceMotion = reduceMotion,
            )
            RollingTimeText(
                value = now.format(DateTimeFormatter.ofPattern("ss")),
                color = colors.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                reduceMotion = reduceMotion,
            )
        }

            ClockFaceStyle.SecondsArc -> Box(
            modifier = Modifier.size(310.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    color = colors.textMuted.copy(alpha = 0.16f),
                    style = Stroke(width = 7.dp.toPx()),
                )
                drawArc(
                    color = colors.primary,
                    startAngle = -90f,
                    sweepAngle = secondProgress * 6f,
                    useCenter = false,
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Butt),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RollingTimeText(
                    value = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                    color = colors.textPrimary,
                    fontSize = 62.sp,
                    fontWeight = FontWeight.Light,
                    reduceMotion = reduceMotion,
                )
                RollingTimeText(
                    value = "%02d".format(now.second),
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    reduceMotion = reduceMotion,
                )
            }
        }

            ClockFaceStyle.MinimalAnalog -> Box(
            modifier = Modifier.size(310.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 10.dp.toPx()
                drawCircle(
                    color = colors.textMuted.copy(alpha = 0.18f),
                    radius = radius,
                    style = Stroke(width = 2.dp.toPx()),
                )
                for (index in 0 until 12) {
                    val angle = index / 12f * 2f * PI.toFloat() - PI.toFloat() / 2f
                    val outer = center + Offset(cos(angle), sin(angle)) * radius
                    val inner = center + Offset(cos(angle), sin(angle)) *
                        (radius - if (index % 3 == 0) 13.dp.toPx() else 7.dp.toPx())
                    drawLine(
                        color = if (index % 3 == 0) colors.secondary else colors.textMuted,
                        start = inner,
                        end = outer,
                        strokeWidth = if (index % 3 == 0) 3.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                fun hand(angle: Float, length: Float, color: Color, width: Float) {
                    val radians = angle / 180f * PI.toFloat() - PI.toFloat() / 2f
                    drawLine(
                        color = color,
                        start = center,
                        end = center + Offset(cos(radians), sin(radians)) * length,
                        strokeWidth = width,
                        cap = StrokeCap.Round,
                    )
                }
                hand(
                    (now.hour % 12 + now.minute / 60f + secondProgress / 3_600f) * 30f,
                    radius * 0.52f,
                    colors.textPrimary,
                    8.dp.toPx(),
                )
                hand(
                    (now.minute + secondProgress / 60f) * 6f,
                    radius * 0.76f,
                    colors.textPrimary,
                    5.dp.toPx(),
                )
                hand(secondProgress * 6f, radius * 0.82f, colors.primary, 2.dp.toPx())
                drawCircle(colors.secondary, radius = 6.dp.toPx(), center = center)
            }
        }

            ClockFaceStyle.DayArc -> Box(
            modifier = Modifier.size(310.dp),
            contentAlignment = Alignment.Center,
        ) {
            val dayProgress = (
                now.hour * 3_600f + now.minute * 60f + secondProgress
                ) / 86_400f
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    color = colors.textMuted.copy(alpha = 0.14f),
                    style = Stroke(width = 9.dp.toPx()),
                )
                drawArc(
                    color = colors.primary.copy(alpha = 0.72f),
                    startAngle = -90f,
                    sweepAngle = dayProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Butt),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RollingTimeText(
                    value = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                    color = colors.textPrimary,
                    fontSize = 62.sp,
                    fontWeight = FontWeight.Light,
                    reduceMotion = reduceMotion,
                )
                Text("24 HOUR", color = colors.textMuted, fontSize = 11.sp, letterSpacing = 2.sp)
            }
        }
    }
}
}

@Composable
private fun AppearanceDialog(
    screen: AppearanceScreen,
    appearance: AppearanceSettings,
    powerSaveMode: Boolean,
    onChange: (AppearanceSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AwtThemeColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${screen.displayName}の外観") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("テーマ", color = colors.textMuted, fontSize = 13.sp)
                AwtThemeId.entries.chunked(2).forEach { rowThemes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowThemes.forEach { theme ->
                            val preview = paletteFor(theme)
                            FilterChip(
                                selected = appearance.theme == theme,
                                onClick = { onChange(appearance.copy(theme = theme)) },
                                leadingIcon = {
                                    Box(
                                        Modifier
                                            .size(14.dp)
                                            .background(preview.primary, CircleShape),
                                    )
                                },
                                label = { Text(theme.displayName) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                Text(
                    "背景色",
                    color = colors.textMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 20.dp),
                )
                BackgroundTone.entries.chunked(2).forEach { rowTones ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowTones.forEach { tone ->
                            FilterChip(
                                selected = appearance.customBackgroundArgb == null &&
                                    appearance.backgroundTone == tone,
                                onClick = {
                                    onChange(
                                        appearance.copy(
                                            backgroundTone = tone,
                                            customBackgroundArgb = null,
                                        ),
                                    )
                                },
                                leadingIcon = {
                                    Box(
                                        Modifier
                                            .size(14.dp)
                                            .background(backgroundPreview(tone), CircleShape),
                                    )
                                },
                                label = { Text(tone.displayName) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowTones.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                FullColorPicker(
                    label = "背景色・フルカラー",
                    customArgb = appearance.customBackgroundArgb,
                    fallback = colors.night,
                    onColorChange = {
                        onChange(appearance.copy(customBackgroundArgb = it))
                    },
                    onUseTheme = {
                        onChange(appearance.copy(customBackgroundArgb = null))
                    },
                )
                FullColorPicker(
                    label = "文字色・フルカラー",
                    customArgb = appearance.customTextArgb,
                    fallback = colors.textPrimary,
                    onColorChange = {
                        onChange(appearance.copy(customTextArgb = it))
                    },
                    onUseTheme = {
                        onChange(appearance.copy(customTextArgb = null))
                    },
                )
                if (screen == AppearanceScreen.Clock) {
                    Text(
                        "時計盤",
                        color = colors.textMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    ClockFaceStyle.entries.chunked(2).forEach { rowStyles ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowStyles.forEach { style ->
                                FilterChip(
                                    selected = appearance.clockFace == style,
                                    onClick = {
                                        onChange(appearance.copy(clockFace = style))
                                    },
                                    label = { Text(style.displayName) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                Text(
                    "数字スタイル",
                    color = colors.textMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NumeralStyle.entries.forEach { numeralStyle ->
                        FilterChip(
                            selected = appearance.numeralStyle == numeralStyle,
                            onClick = {
                                onChange(appearance.copy(numeralStyle = numeralStyle))
                            },
                            label = { Text(numeralStyle.displayName) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Text(
                    "背景モーション",
                    color = colors.textMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 20.dp),
                )
                MotionScene.entries.chunked(2).forEach { rowScenes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowScenes.forEach { scene ->
                            FilterChip(
                                selected = appearance.motionScene == scene,
                                onClick = { onChange(appearance.copy(motionScene = scene)) },
                                label = { Text(scene.displayName) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowScenes.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                if (screen == AppearanceScreen.Clock) {
                    SettingSwitch("時刻周囲の円形アニメーション", appearance.showClockHalo) {
                        onChange(appearance.copy(showClockHalo = it))
                    }
                }
                SettingSwitch("動きを減らす", appearance.reduceMotion) {
                    onChange(appearance.copy(reduceMotion = it))
                }
                Text(
                    when {
                        appearance.motionScene == MotionScene.Still -> "静止背景を使用します"
                        powerSaveMode -> "省電力モード中のため、背景は自動的に静止します"
                        appearance.reduceMotion -> "選択した背景を静止して表示します"
                        appearance.motionScene == MotionScene.TidalLight ->
                            "複合波、集光線、白波の残響を低輝度で表示します"
                        else -> "複数の光場が途切れずゆっくり循環します"
                    },
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完了") }
        },
    )
}

@Composable
private fun FullColorPicker(
    label: String,
    customArgb: Int?,
    fallback: Color,
    onColorChange: (Int) -> Unit,
    onUseTheme: () -> Unit,
) {
    val sourceArgb = customArgb ?: fallback.toArgb()
    var expanded by rememberSaveable(label) { mutableStateOf(false) }
    val hsv = remember(sourceArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(sourceArgb, it) }
    }
    val preview = Color(sourceArgb)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .background(preview, CircleShape),
            )
            Text(
                label,
                color = AwtThemeColors.current.textMuted,
                fontSize = 13.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            )
            TextButton(onClick = onUseTheme, enabled = customArgb != null) {
                Text("既定")
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = AwtThemeColors.current.textMuted,
            )
        }
        if (expanded) {
            ColorPaletteSurface(
                hue = hsv[0],
                saturation = hsv[1],
                brightness = hsv[2],
                onChange = { hue, saturation, brightness ->
                    onColorChange(hsvToArgb(hue, saturation, brightness))
                },
            )
        }
    }
}

@Composable
private fun ColorPaletteSurface(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onChange: (Float, Float, Float) -> Unit,
) {
    val hueColor = Color(hsvToArgb(hue, 1f, 1f))
    val rainbow = listOf(
        Color.Red,
        Color.Yellow,
        Color.Green,
        Color.Cyan,
        Color.Blue,
        Color.Magenta,
        Color.Red,
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .palettePointerInput { position, width, height ->
                onChange(
                    hue,
                    (position.x / width).coerceIn(0f, 1f),
                    (1f - position.y / height).coerceIn(0f, 1f),
                )
            },
    ) {
        drawRect(
            brush = Brush.horizontalGradient(listOf(Color.White, hueColor)),
        )
        drawRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)),
        )
        val marker = Offset(
            x = saturation.coerceIn(0f, 1f) * size.width,
            y = (1f - brightness.coerceIn(0f, 1f)) * size.height,
        )
        drawCircle(Color.Black.copy(alpha = 0.62f), 9.dp.toPx(), marker, style = Stroke(3.dp.toPx()))
        drawCircle(Color.White, 7.dp.toPx(), marker, style = Stroke(2.dp.toPx()))
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .palettePointerInput { position, width, _ ->
                onChange(
                    (position.x / width).coerceIn(0f, 1f) * 360f,
                    saturation,
                    brightness,
                )
            },
    ) {
        drawRect(brush = Brush.horizontalGradient(rainbow))
        val markerX = (hue.coerceIn(0f, 360f) / 360f) * size.width
        drawLine(
            color = Color.Black.copy(alpha = 0.70f),
            start = Offset(markerX, 0f),
            end = Offset(markerX, size.height),
            strokeWidth = 5.dp.toPx(),
        )
        drawLine(
            color = Color.White,
            start = Offset(markerX, 0f),
            end = Offset(markerX, size.height),
            strokeWidth = 2.dp.toPx(),
        )
    }
    Text(
        text = "パレットをなぞって色を選択",
        color = AwtThemeColors.current.textMuted,
        fontSize = 10.sp,
        modifier = Modifier.padding(top = 7.dp),
    )
}

private fun Modifier.palettePointerInput(
    onPosition: (Offset, Float, Float) -> Unit,
): Modifier = pointerInput(onPosition) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        fun update(position: Offset) {
            onPosition(
                position,
                size.width.toFloat().coerceAtLeast(1f),
                size.height.toFloat().coerceAtLeast(1f),
            )
        }
        update(down.position)
        var pressed = true
        while (pressed) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull()
            if (change == null) {
                pressed = false
            } else {
                update(change.position)
                pressed = change.pressed
                change.consume()
            }
        }
    }
}

internal fun hsvToArgb(hue: Float, saturation: Float, brightness: Float): Int =
    AndroidColor.HSVToColor(
        floatArrayOf(
            hue.coerceIn(0f, 360f),
            saturation.coerceIn(0f, 1f),
            brightness.coerceIn(0f, 1f),
        ),
    )

@Composable
private fun MeasureScreen(
    timerEnd: Long,
    timerDuration: Long,
    timerPausedRemaining: Long,
    appResumed: Boolean,
    onStartTimer: (Long) -> Boolean,
    onPauseTimer: (Long) -> Unit,
    onResumeTimer: () -> Boolean,
    onCancelTimer: () -> Unit,
    reduceMotion: Boolean,
    powerSaveMode: Boolean,
) {
    val modePager = rememberPagerState(pageCount = { MeasureMode.entries.size })
    val modeScope = rememberCoroutineScope()
    val mode = MeasureMode.entries[modePager.currentPage]
    Column(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = modePager,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            beyondViewportPageCount = 1,
            userScrollEnabled = true,
            key = { MeasureMode.entries[it] },
        ) { page ->
            when (MeasureMode.entries[page]) {
                MeasureMode.Timer -> TimerScreen(
                    timerEnd = timerEnd,
                    timerDuration = timerDuration,
                    timerPausedRemaining = timerPausedRemaining,
                    appResumed = appResumed && mode == MeasureMode.Timer,
                    onStart = onStartTimer,
                    onPause = onPauseTimer,
                    onResume = onResumeTimer,
                    onCancel = onCancelTimer,
                    reduceMotion = reduceMotion,
                    animateEffects = appResumed &&
                        mode == MeasureMode.Timer &&
                        !reduceMotion &&
                        !powerSaveMode,
                )
                MeasureMode.Stopwatch -> StopwatchScreen(
                    appResumed = appResumed && mode == MeasureMode.Stopwatch,
                    reduceMotion = reduceMotion,
                    powerSaveMode = powerSaveMode,
                )
            }
        }
        MeasureModeSelector(
            selected = mode,
            onSelect = { selectedMode ->
                modeScope.launch {
                    modePager.animateScrollToPage(MeasureMode.entries.indexOf(selectedMode))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun MeasureModeSelector(
    selected: MeasureMode,
    onSelect: (MeasureMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwtThemeColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = colors.nightSoft.copy(alpha = 0.94f),
    ) {
        Row(Modifier.padding(4.dp)) {
            listOf(
                MeasureMode.Timer to "タイマー",
                MeasureMode.Stopwatch to "ストップウォッチ",
            ).forEach { (mode, label) ->
                val isSelected = mode == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(
                            color = if (isSelected) colors.primary.copy(alpha = 0.20f) else Color.Transparent,
                            shape = RoundedCornerShape(20.dp),
                        )
                        .clickable { onSelect(mode) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.primary else colors.textMuted,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmList(
    alarms: List<Alarm>,
    onToggle: (Alarm, Boolean) -> Unit,
    onEdit: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit,
) {
    val colors = AwtThemeColors.current
    if (alarms.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = colors.primary.copy(alpha = 0.65f),
                    modifier = Modifier.size(54.dp),
                )
                Text(
                    "最初のアラームを追加しましょう",
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 18.dp, bottom = 100.dp),
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(alarms, key = { it.id }) { alarm ->
            AlarmCard(alarm, onToggle, onEdit, onDelete)
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: Alarm,
    onToggle: (Alarm, Boolean) -> Unit,
    onEdit: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit,
) {
    val colors = AwtThemeColors.current
    val alpha by animateFloatAsState(if (alarm.enabled) 1f else 0.45f, label = "enabled")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(alarm) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.nightSoft.copy(alpha = 0.90f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 18.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        alarm.timeText,
                        color = colors.textPrimary.copy(alpha = alpha),
                        fontSize = 39.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Light,
                    )
                    if (alarm.label.isNotBlank()) {
                        Text(
                            alarm.label,
                            color = colors.textMuted.copy(alpha = alpha),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 12.dp, bottom = 5.dp),
                        )
                    }
                }
                Text(
                    text = if (alarm.enabled) {
                        val next = NextAlarmCalculator.next(alarm, ZonedDateTime.now())
                        "${repeatSummary(alarm.repeatMask)}  ·  ${next.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.JAPAN)}"
                    } else {
                        repeatSummary(alarm.repeatMask)
                    },
                    color = colors.textMuted.copy(alpha = alpha),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Switch(checked = alarm.enabled, onCheckedChange = { onToggle(alarm, it) })
            IconButton(onClick = { onDelete(alarm) }) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "削除",
                    tint = colors.textMuted.copy(alpha = 0.65f),
                )
            }
        }
    }
}

@Composable
private fun AlarmEditor(
    initial: Alarm,
    reduceMotion: Boolean,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit,
) {
    val colors = AwtThemeColors.current
    var timeDigits by remember(initial.id) {
        mutableStateOf("%02d%02d".format(initial.hour, initial.minute))
    }
    var digitEntryActive by remember(initial.id) { mutableStateOf(false) }
    var label by remember(initial.id) { mutableStateOf(initial.label) }
    var repeatMask by remember(initial.id) { mutableIntStateOf(initial.repeatMask) }
    var gradual by remember(initial.id) { mutableStateOf(initial.gradualVolume) }
    var vibrate by remember(initial.id) { mutableStateOf(initial.vibrate) }
    var showDetails by remember(initial.id) { mutableStateOf(false) }
    val validTime = parseClockDigits(timeDigits)

    fun save() {
        val time = validTime ?: return
        onSave(
            initial.copy(
                hour = time.first,
                minute = time.second,
                label = label.trim(),
                enabled = true,
                repeatMask = repeatMask,
                gradualVolume = gradual,
                vibrate = vibrate,
            ),
        )
    }

    AwtMotionBackdrop(
        scene = MotionScene.Still,
        animate = false,
        pagePosition = 0f,
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
            bottomBar = {
                EditorActionBar(
                    onDismiss = onDismiss,
                    onSave = ::save,
                    saveEnabled = validTime != null,
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (initial.id == 0L) "アラームを追加" else "アラームを編集",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    color = colors.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(10.dp))
                DigitTimeDisplay(
                    digits = timeDigits,
                    groupSizes = listOf(2, 2),
                    fontSize = 82.sp,
                    reduceMotion = reduceMotion,
                )
                Text(
                    when {
                        timeDigits.length == 4 && validTime == null -> "00:00〜23:59の範囲で入力してください"
                        digitEntryActive -> "4桁で時刻を入力"
                        else -> "数字を押すと現在の設定を置き換えます"
                    },
                    color = if (timeDigits.length == 4 && validTime == null) colors.alert else colors.textMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 52.dp),
                )
                NumericKeypad(
                    onDigit = { digit ->
                        timeDigits = if (!digitEntryActive || timeDigits.length >= 4) {
                            digit.toString()
                        } else {
                            timeDigits + digit
                        }
                        digitEntryActive = true
                    },
                    onBackspace = {
                        if (!digitEntryActive) {
                            timeDigits = ""
                            digitEntryActive = true
                        } else {
                            timeDigits = timeDigits.dropLast(1)
                        }
                    },
                    onClear = {
                        timeDigits = ""
                        digitEntryActive = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyHeight = 52.dp,
                    isDigitEnabled = { digit ->
                        canAppendClockDigit(timeDigits, digitEntryActive, digit)
                    },
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(30) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    singleLine = true,
                    label = { Text("ラベル") },
                    placeholder = { Text("例：起床") },
                )
                Text(
                    "繰り返し",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 8.dp),
                    color = colors.textMuted,
                    fontSize = 13.sp,
                )
                Row(Modifier.fillMaxWidth()) {
                    dayLabels.forEachIndexed { index, text ->
                        val selected = repeatMask and (1 shl index) != 0
                        FilterChip(
                            selected = selected,
                            onClick = { repeatMask = repeatMask xor (1 shl index) },
                            label = {
                                Text(
                                    text,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp),
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.nightSoft.copy(alpha = 0.82f),
                    contentColor = colors.textPrimary,
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDetails = !showDetails }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("詳細設定", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            Text(
                                if (showDetails) "閉じる" else "2項目",
                                color = colors.textMuted,
                                fontSize = 12.sp,
                            )
                            Icon(
                                if (showDetails) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = colors.textMuted,
                            )
                        }
                        AnimatedVisibility(showDetails) {
                            Column {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                SettingSwitch("徐々に音量を上げる", gradual) { gradual = it }
                                SettingSwitch("バイブレーション", vibrate) { vibrate = it }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun EditorActionBar(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
) {
    val colors = AwtThemeColors.current
    Surface(
        color = colors.nightSoft.copy(alpha = 0.98f),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) {
                Text("キャンセル")
            }
            Button(
                onClick = onSave,
                enabled = saveEnabled,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Text(
                    text = "保存",
                    modifier = Modifier.padding(start = 6.dp),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun Int.floorDay(): Int = ((this % 1440) + 1440) % 1440

@Composable
private fun DigitTimeDisplay(
    digits: String,
    groupSizes: List<Int>,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
) {
    val colors = AwtThemeColors.current
    val totalDigits = groupSizes.sum()
    val display = digits.take(totalDigits).padEnd(totalDigits, '–')
    var offset = 0
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        groupSizes.forEachIndexed { groupIndex, groupSize ->
            repeat(groupSize) {
                val digitIndex = offset
                val character = display[offset++]
                AnimatedTimeGlyph(
                    character = character,
                    color = if (character.isDigit()) {
                        colors.textPrimary
                    } else {
                        colors.textMuted.copy(alpha = 0.45f)
                    },
                    glowColor = colors.primary,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Light,
                    width = fontSize.value.dp * 0.72f,
                    reduceMotion = reduceMotion,
                    motionDirection = 0,
                    delayMillis = digitIndex * 22,
                )
            }
            if (groupIndex < groupSizes.lastIndex) {
                Text(
                    text = ":",
                    color = colors.primary,
                    fontSize = fontSize * 0.72f,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun AnimatedTimeGlyph(
    character: Char,
    color: Color,
    glowColor: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    width: androidx.compose.ui.unit.Dp,
    reduceMotion: Boolean,
    motionDirection: Int,
    delayMillis: Int = 0,
) {
    MorphingTimeDigit(
        character = character,
        color = color,
        glowColor = glowColor,
        fontSize = fontSize,
        width = width,
        reduceMotion = reduceMotion,
        motionDirection = motionDirection,
        delayMillis = delayMillis,
    )
}

@Composable
private fun NumericKeypad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    keyHeight: androidx.compose.ui.unit.Dp = 52.dp,
    isDigitEnabled: (Int) -> Boolean = { true },
) {
    val colors = AwtThemeColors.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("クリア", "0", "1字削除"),
        ).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { key ->
                    val isDigit = key.length == 1 && key[0].isDigit()
                    val digit = if (isDigit) key.toInt() else null
                    val enabled = digit == null || isDigitEnabled(digit)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(keyHeight)
                            .clickable(enabled = enabled) {
                                when {
                                    digit != null -> onDigit(digit)
                                    key == "クリア" -> onClear()
                                    else -> onBackspace()
                                }
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = when {
                            isDigit && enabled -> colors.nightSoft.copy(alpha = 0.88f)
                            isDigit -> colors.nightSoft.copy(alpha = 0.32f)
                            else -> colors.primary.copy(alpha = 0.12f)
                        },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = key,
                                color = when {
                                    isDigit && enabled -> colors.textPrimary
                                    isDigit -> colors.textMuted.copy(alpha = 0.30f)
                                    else -> colors.primary
                                },
                                fontSize = if (isDigit) 22.sp else 12.sp,
                                fontWeight = if (isDigit) FontWeight.Medium else FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun canAppendClockDigit(
    digits: String,
    digitEntryActive: Boolean,
    digit: Int,
): Boolean {
    val prefix = if (!digitEntryActive || digits.length >= 4) "" else digits
    return when (prefix.length) {
        0 -> digit in 0..2
        1 -> if (prefix.first() == '2') digit in 0..3 else digit in 0..9
        2 -> digit in 0..5
        3 -> digit in 0..9
        else -> false
    }
}

internal fun parseClockDigits(digits: String): Pair<Int, Int>? {
    if (digits.length != 4 || !digits.all(Char::isDigit)) return null
    val hour = digits.substring(0, 2).toInt()
    val minute = digits.substring(2, 4).toInt()
    return if (hour in 0..23 && minute in 0..59) hour to minute else null
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun TimerScreen(
    timerEnd: Long,
    timerDuration: Long,
    timerPausedRemaining: Long,
    appResumed: Boolean,
    onStart: (Long) -> Boolean,
    onPause: (Long) -> Unit,
    onResume: () -> Boolean,
    onCancel: () -> Unit,
    reduceMotion: Boolean,
    animateEffects: Boolean,
) {
    val colors = AwtThemeColors.current
    var durationDigits by rememberSaveable { mutableStateOf("001000") }
    var digitEntryActive by rememberSaveable { mutableStateOf(false) }
    var showTimerControls by rememberSaveable { mutableStateOf(false) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var startFailed by remember { mutableStateOf(false) }
    val active = timerEnd > nowMillis
    val paused = !active && timerPausedRemaining > 0L
    val remainingMillis = (timerEnd - nowMillis).coerceAtLeast(0L)
    val selectedDuration = parseDurationDigits(durationDigits)
    val selectedMillis = selectedDuration?.let { (hours, minutes, seconds) ->
        (hours * 3_600L + minutes * 60L + seconds) * 1_000L
    } ?: 0L

    LaunchedEffect(timerEnd, appResumed, animateEffects) {
        nowMillis = System.currentTimeMillis()
        while (appResumed && timerEnd > 0 && nowMillis < timerEnd) {
            nowMillis = System.currentTimeMillis()
            delay(if (animateEffects) 33L else 1_000L)
        }
        nowMillis = System.currentTimeMillis()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(
            Modifier.height(
                when {
                    active || paused -> 132.dp
                    !active && !showTimerControls -> 132.dp
                    else -> 12.dp
                },
            ),
        )
        Box(
            modifier = Modifier
                .size(360.dp)
                .clickable(enabled = !active && !paused) {
                    showTimerControls = !showTimerControls
                },
            contentAlignment = Alignment.Center,
        ) {
            TimeProgressHalo(
                modifier = Modifier.fillMaxSize(),
                primary = colors.primary,
                orbitProgress = if (active) nowMillis.mod(1_000L) / 1_000f else 0f,
                completionProgress = when {
                    active -> timerRingProgress(remainingMillis, timerDuration)
                    paused -> timerRingProgress(timerPausedRemaining, timerDuration)
                    else -> null
                },
                animate = animateEffects && active,
            )
            Surface(
                modifier = Modifier.size(334.dp),
                shape = CircleShape,
                color = colors.primary.copy(alpha = 0.10f),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    colors.primary.copy(alpha = 0.45f),
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (active || paused) {
                        val remaining = if (active) remainingMillis else timerPausedRemaining
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.offset(y = (-27).dp),
                        ) {
                            RollingTimeText(
                                value = formatDuration(remaining),
                                color = colors.textPrimary,
                                fontSize = if (remaining >= 3_600_000L) 64.sp else 84.sp,
                                fontWeight = FontWeight.Light,
                                reduceMotion = reduceMotion,
                                motionDirection = -1,
                            )
                            Text(
                                if (paused) "一時停止中" else "残り時間",
                                color = colors.textMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.offset(y = (-27).dp),
                        ) {
                            DigitTimeDisplay(
                                digits = durationDigits,
                                groupSizes = listOf(2, 2, 2),
                                fontSize = 64.sp,
                                reduceMotion = reduceMotion,
                            )
                            Text(
                                "時　　分　　秒",
                                color = colors.textMuted,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 7.dp),
                            )
                            if (!showTimerControls) {
                                Text(
                                    "タップして設定",
                                    color = colors.textMuted.copy(alpha = 0.62f),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 38.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (active || paused) {
                            TimeOverlayButton(
                                icon = Icons.Rounded.Stop,
                                contentDescription = "タイマーを終了",
                                primary = false,
                                onClick = onCancel,
                            )
                        }
                        TimeOverlayButton(
                            icon = if (active) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = when {
                                active -> "タイマーを一時停止"
                                paused -> "タイマーを再開"
                                else -> "タイマーを開始"
                            },
                            primary = true,
                            enabled = active || paused || selectedMillis > 0L,
                            onClick = {
                                startFailed = when {
                                    active -> {
                                        onPause(remainingMillis)
                                        false
                                    }
                                    paused -> !onResume()
                                    else -> !onStart(selectedMillis)
                                }
                            },
                        )
                    }
                }
            }
        }
        Spacer(
            Modifier.height(
                when {
                    active -> 88.dp
                    paused -> 18.dp
                    showTimerControls -> 20.dp
                    else -> 8.dp
                },
            ),
        )
        if (active) {
            val remaining = (timerEnd - nowMillis).coerceAtLeast(0L)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { startFailed = !onStart(remaining + 60_000L) }) {
                    Text("＋1分")
                }
                OutlinedButton(onClick = { startFailed = !onStart(remaining + 5 * 60_000L) }) {
                    Text("＋5分")
                }
            }
        } else if (paused) {
            Text(
                "再生ボタンで続きから再開できます",
                color = colors.textMuted.copy(alpha = 0.72f),
                fontSize = 11.sp,
            )
        } else if (showTimerControls) {
            Text(
                when {
                    durationDigits.length == 6 && selectedDuration == null ->
                        "分と秒は00〜59で入力してください"
                    digitEntryActive -> "6桁で時間・分・秒を入力"
                    else -> "6桁で時間・分・秒を入力"
                },
                color = if (durationDigits.length == 6 && selectedDuration == null) {
                    colors.alert
                } else {
                    colors.textMuted
                },
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            NumericKeypad(
                onDigit = { digit ->
                    durationDigits = if (!digitEntryActive || durationDigits.length >= 6) {
                        digit.toString()
                    } else {
                        durationDigits + digit
                    }
                    digitEntryActive = true
                },
                onBackspace = {
                    if (!digitEntryActive) {
                        durationDigits = ""
                        digitEntryActive = true
                    } else {
                        durationDigits = durationDigits.dropLast(1)
                    }
                },
                onClear = {
                    durationDigits = ""
                    digitEntryActive = true
                },
                modifier = Modifier.fillMaxWidth(),
                keyHeight = 48.dp,
                isDigitEnabled = { digit ->
                    canAppendDurationDigit(durationDigits, digitEntryActive, digit)
                },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(5, 10, 20, 30).forEach { value ->
                    FilterChip(
                        selected = durationDigits == "%02d%02d00".format(value / 60, value % 60),
                        onClick = {
                            durationDigits = "%02d%02d00".format(value / 60, value % 60)
                            digitEntryActive = false
                        },
                        label = { Text("$value 分") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            AnimatedVisibility(startFailed) {
                Text(
                    "正確なアラーム権限を許可してください",
                    color = colors.alert,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        } else {
            Text(
                "時間表示を押すと設定を開きます",
                color = colors.textMuted.copy(alpha = 0.58f),
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun TimeOverlayButton(
    icon: ImageVector,
    contentDescription: String,
    primary: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val colors = AwtThemeColors.current
    Surface(
        modifier = Modifier
            .size(if (primary) 60.dp else 52.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = when {
            !enabled -> colors.nightSoft.copy(alpha = 0.44f)
            primary -> colors.primary.copy(alpha = 0.90f)
            else -> colors.nightSoft.copy(alpha = 0.86f)
        },
        shadowElevation = if (primary && enabled) 5.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = when {
                    !enabled -> colors.textMuted.copy(alpha = 0.46f)
                    primary -> colors.night
                    else -> colors.textPrimary.copy(alpha = 0.86f)
                },
                modifier = Modifier.size(if (primary) 28.dp else 23.dp),
            )
        }
    }
}

internal fun canAppendDurationDigit(
    digits: String,
    digitEntryActive: Boolean,
    digit: Int,
): Boolean {
    val prefix = if (!digitEntryActive || digits.length >= 6) "" else digits
    return when (prefix.length) {
        0, 1, 3, 5 -> digit in 0..9
        2, 4 -> digit in 0..5
        else -> false
    }
}

internal fun parseDurationDigits(digits: String): Triple<Int, Int, Int>? {
    if (digits.length != 6 || !digits.all(Char::isDigit)) return null
    val hours = digits.substring(0, 2).toInt()
    val minutes = digits.substring(2, 4).toInt()
    val seconds = digits.substring(4, 6).toInt()
    return if (minutes in 0..59 && seconds in 0..59) {
        Triple(hours, minutes, seconds)
    } else {
        null
    }
}

@Composable
private fun StopwatchScreen(
    appResumed: Boolean,
    reduceMotion: Boolean,
    powerSaveMode: Boolean,
) {
    val colors = AwtThemeColors.current
    var running by rememberSaveable { mutableStateOf(false) }
    var startedAt by rememberSaveable { mutableLongStateOf(0L) }
    var accumulated by rememberSaveable { mutableLongStateOf(0L) }
    var tick by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val laps = remember { mutableStateListOf<Long>() }
    val elapsed = accumulated + if (running) tick - startedAt else 0L

    LaunchedEffect(running, appResumed, reduceMotion, powerSaveMode) {
        while (running && appResumed) {
            tick = SystemClock.elapsedRealtime()
            delay(if (reduceMotion || powerSaveMode) 250L else 50L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(118.dp))
        Box(
            modifier = Modifier.size(360.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.offset(y = (-28).dp)) {
                StopwatchTimeText(
                    elapsed = elapsed,
                    color = colors.textPrimary,
                    reduceMotion = reduceMotion,
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 38.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimeOverlayButton(
                    icon = if (running) Icons.Rounded.Add else Icons.Rounded.Refresh,
                    contentDescription = if (running) "ラップ" else "リセット",
                    primary = false,
                    enabled = running || accumulated > 0L,
                    onClick = {
                        if (running) {
                            laps.add(0, elapsed)
                        } else {
                            accumulated = 0L
                            laps.clear()
                        }
                    },
                )
                TimeOverlayButton(
                    icon = if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = when {
                        running -> "ストップウォッチを一時停止"
                        accumulated == 0L -> "ストップウォッチを開始"
                        else -> "ストップウォッチを再開"
                    },
                    primary = true,
                    onClick = {
                        if (running) {
                            accumulated = elapsed
                            running = false
                        } else {
                            startedAt = SystemClock.elapsedRealtime()
                            tick = startedAt
                            running = true
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        LazyColumn(Modifier.fillMaxWidth()) {
            items(laps.size) { index ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("ラップ ${laps.size - index}", color = colors.textMuted)
                    Text(formatStopwatch(laps[index]), color = colors.textMuted)
                }
            }
        }
    }
}

@Composable
private fun StopwatchTimeText(
    elapsed: Long,
    color: Color,
    reduceMotion: Boolean,
) {
    val formatted = formatStopwatch(elapsed)
    val main = formatted.substringBefore('.')
    val fraction = formatted.substringAfter('.')
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        RollingTimeText(
            value = main,
            color = color,
            fontSize = 104.sp,
            fontWeight = FontWeight.Light,
            reduceMotion = reduceMotion,
        )
        Text(
            text = ".$fraction",
            color = color.copy(alpha = 0.72f),
            fontSize = 36.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun RollingTimeText(
    value: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Light,
    reduceMotion: Boolean,
    motionDirection: Int = 1,
    modifier: Modifier = Modifier,
) {
    val colors = AwtThemeColors.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        value.forEachIndexed { index, character ->
            val characterWidth = if (character.isDigit()) {
                fontSize.value.dp * 0.64f
            } else {
                fontSize.value.dp * 0.38f
            }
            if (character.isDigit()) {
                AnimatedTimeGlyph(
                    character = character,
                    color = color,
                    glowColor = colors.primary,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    width = characterWidth,
                    reduceMotion = reduceMotion,
                    motionDirection = motionDirection,
                    delayMillis = (value.lastIndex - index).coerceAtLeast(0) * 34,
                )
            } else {
                Text(
                    text = character.toString(),
                    color = color,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(characterWidth),
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis + 999L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = totalSeconds % 3_600L / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

private fun formatStopwatch(millis: Long): String {
    val minutes = millis / 60_000L
    val seconds = millis % 60_000L / 1_000L
    val hundredths = millis % 1_000L / 10L
    return "%02d:%02d.%02d".format(minutes, seconds, hundredths)
}
