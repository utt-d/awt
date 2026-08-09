package jp.awt.clock.ui

import android.content.Context
import android.content.SharedPreferences

enum class AwtThemeId(val displayName: String) {
    Dawn("Dawn"),
    Lagoon("Lagoon"),
    Ember("Ember"),
    Aurora("Aurora"),
    VioletNight("Violet Night"),
    Forest("Forest"),
    Moon("Moon"),
    Oled("OLED"),
}

enum class ClockFaceStyle(val displayName: String) {
    Digital("デジタル"),
    SecondsArc("秒アーク"),
    MinimalAnalog("アナログ"),
    DayArc("24時間"),
}

enum class MotionScene(val displayName: String) {
    Aurora("Aurora"),
    TidalLight("水面の光"),
    Still("静止"),
}

enum class NumeralStyle(val displayName: String) {
    Arabic("標準数字"),
    Segmented("線分数字"),
}

enum class BackgroundTone(val displayName: String) {
    Theme("テーマ連動"),
    Midnight("深夜"),
    Ocean("深海"),
    Plum("紫紺"),
    Forest("森林"),
    Slate("墨色"),
    Black("黒"),
}

enum class AppearanceScreen(
    val displayName: String,
    internal val storagePrefix: String,
) {
    Alarms("アラーム", "alarms_"),
    Clock("時計", "clock_"),
    Measure("計測", "measure_"),
}

data class AppearanceSettings(
    val theme: AwtThemeId = AwtThemeId.Dawn,
    val clockFace: ClockFaceStyle = ClockFaceStyle.Digital,
    val motionScene: MotionScene = MotionScene.Aurora,
    val numeralStyle: NumeralStyle = NumeralStyle.Arabic,
    val backgroundTone: BackgroundTone = BackgroundTone.Theme,
    val customBackgroundArgb: Int? = null,
    val customTextArgb: Int? = null,
    val showClockHalo: Boolean = true,
    val reduceMotion: Boolean = false,
)

data class AppearanceProfiles(
    val alarms: AppearanceSettings = AppearanceSettings(),
    val clock: AppearanceSettings = AppearanceSettings(),
    val measure: AppearanceSettings = AppearanceSettings(),
) {
    operator fun get(screen: AppearanceScreen): AppearanceSettings = when (screen) {
        AppearanceScreen.Alarms -> alarms
        AppearanceScreen.Clock -> clock
        AppearanceScreen.Measure -> measure
    }

    fun updated(
        screen: AppearanceScreen,
        settings: AppearanceSettings,
    ): AppearanceProfiles = when (screen) {
        AppearanceScreen.Alarms -> copy(alarms = settings)
        AppearanceScreen.Clock -> copy(clock = settings)
        AppearanceScreen.Measure -> copy(measure = settings)
    }
}

object AppearancePreferences {
    private const val PREFS = "awt_appearance"
    private const val KEY_THEME = "theme"
    private const val KEY_CLOCK_FACE = "clock_face"
    private const val KEY_MOTION_SCENE = "motion_scene"
    private const val KEY_NUMERAL_STYLE = "numeral_style"
    private const val KEY_BACKGROUND_TONE = "background_tone"
    private const val KEY_CUSTOM_BACKGROUND = "custom_background"
    private const val KEY_CUSTOM_TEXT = "custom_text"
    private const val KEY_SHOW_CLOCK_HALO = "show_clock_halo"
    private const val KEY_REDUCE_MOTION = "reduce_motion"
    private const val KEY_SCOPED_MIGRATION = "screen_profiles_migrated"

    fun readAll(context: Context): AppearanceProfiles {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val legacy = readSettings(preferences, prefix = "", fallback = AppearanceSettings())
        if (!preferences.getBoolean(KEY_SCOPED_MIGRATION, false)) {
            return AppearanceProfiles(
                alarms = legacy,
                clock = legacy,
                measure = legacy,
            ).also { profiles ->
                writeAll(context, profiles)
            }
        }
        val defaults = AppearanceSettings()
        return AppearanceProfiles(
            alarms = readSettings(
                preferences,
                AppearanceScreen.Alarms.storagePrefix,
                defaults,
            ),
            clock = readSettings(
                preferences,
                AppearanceScreen.Clock.storagePrefix,
                defaults,
            ),
            measure = readSettings(
                preferences,
                AppearanceScreen.Measure.storagePrefix,
                defaults,
            ),
        )
    }

    fun read(context: Context, screen: AppearanceScreen): AppearanceSettings =
        readAll(context)[screen]

    fun write(
        context: Context,
        screen: AppearanceScreen,
        settings: AppearanceSettings,
    ) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        writeSettings(editor, screen.storagePrefix, settings)
        editor.putBoolean(KEY_SCOPED_MIGRATION, true)
        editor.apply()
    }

    private fun writeAll(context: Context, profiles: AppearanceProfiles) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        AppearanceScreen.entries.forEach { screen ->
            writeSettings(editor, screen.storagePrefix, profiles[screen])
        }
        editor.putBoolean(KEY_SCOPED_MIGRATION, true)
        editor.apply()
    }

    private fun readSettings(
        preferences: SharedPreferences,
        prefix: String,
        fallback: AppearanceSettings,
    ): AppearanceSettings =
        AppearanceSettings(
            theme = enumValueOrDefault(
                preferences.getString(prefix + KEY_THEME, null),
                fallback.theme,
            ),
            clockFace = enumValueOrDefault(
                preferences.getString(prefix + KEY_CLOCK_FACE, null),
                fallback.clockFace,
            ),
            motionScene = enumValueOrDefault(
                preferences.getString(prefix + KEY_MOTION_SCENE, null),
                fallback.motionScene,
            ),
            numeralStyle = enumValueOrDefault(
                preferences.getString(prefix + KEY_NUMERAL_STYLE, null),
                fallback.numeralStyle,
            ),
            backgroundTone = enumValueOrDefault(
                preferences.getString(prefix + KEY_BACKGROUND_TONE, null),
                fallback.backgroundTone,
            ),
            customBackgroundArgb = if (preferences.contains(prefix + KEY_CUSTOM_BACKGROUND)) {
                preferences.getInt(prefix + KEY_CUSTOM_BACKGROUND, 0)
            } else {
                fallback.customBackgroundArgb
            },
            customTextArgb = if (preferences.contains(prefix + KEY_CUSTOM_TEXT)) {
                preferences.getInt(prefix + KEY_CUSTOM_TEXT, 0)
            } else {
                fallback.customTextArgb
            },
            showClockHalo = preferences.getBoolean(
                prefix + KEY_SHOW_CLOCK_HALO,
                fallback.showClockHalo,
            ),
            reduceMotion = preferences.getBoolean(
                prefix + KEY_REDUCE_MOTION,
                fallback.reduceMotion,
            ),
        )

    private fun writeSettings(
        editor: SharedPreferences.Editor,
        prefix: String,
        settings: AppearanceSettings,
    ) {
        editor
            .putString(prefix + KEY_THEME, settings.theme.name)
            .putString(prefix + KEY_CLOCK_FACE, settings.clockFace.name)
            .putString(prefix + KEY_MOTION_SCENE, settings.motionScene.name)
            .putString(prefix + KEY_NUMERAL_STYLE, settings.numeralStyle.name)
            .putString(prefix + KEY_BACKGROUND_TONE, settings.backgroundTone.name)
            .putBoolean(prefix + KEY_SHOW_CLOCK_HALO, settings.showClockHalo)
            .putBoolean(prefix + KEY_REDUCE_MOTION, settings.reduceMotion)
        if (settings.customBackgroundArgb == null) {
            editor.remove(prefix + KEY_CUSTOM_BACKGROUND)
        } else {
            editor.putInt(prefix + KEY_CUSTOM_BACKGROUND, settings.customBackgroundArgb)
        }
        if (settings.customTextArgb == null) {
            editor.remove(prefix + KEY_CUSTOM_TEXT)
        } else {
            editor.putInt(prefix + KEY_CUSTOM_TEXT, settings.customTextArgb)
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        stored: String?,
        default: T,
    ): T = enumValues<T>().firstOrNull { it.name == stored } ?: default
}
