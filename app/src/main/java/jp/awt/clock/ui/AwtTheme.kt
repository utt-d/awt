package jp.awt.clock.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Immutable
data class AwtPalette(
    val night: Color,
    val nightSoft: Color,
    val backgroundTop: Color,
    val primary: Color,
    val secondary: Color,
    val alert: Color,
    val textPrimary: Color = Color(0xFFF4F7F9),
    val textMuted: Color = Color(0xFFAEB9C6),
)

private val palettes = mapOf(
    AwtThemeId.Dawn to AwtPalette(
        night = Color(0xFF101521),
        nightSoft = Color(0xFF192333),
        backgroundTop = Color(0xFF23374B),
        primary = Color(0xFFBFE9DF),
        secondary = Color(0xFFF5C978),
        alert = Color(0xFFFF8F7A),
    ),
    AwtThemeId.Lagoon to AwtPalette(
        night = Color(0xFF071820),
        nightSoft = Color(0xFF0E2A34),
        backgroundTop = Color(0xFF145064),
        primary = Color(0xFF83E9D8),
        secondary = Color(0xFFF0D38A),
        alert = Color(0xFFFF8D80),
    ),
    AwtThemeId.Ember to AwtPalette(
        night = Color(0xFF1A1012),
        nightSoft = Color(0xFF2B191B),
        backgroundTop = Color(0xFF5A2C2B),
        primary = Color(0xFFFFD5B8),
        secondary = Color(0xFFFFB45E),
        alert = Color(0xFFFF8175),
    ),
    AwtThemeId.Aurora to AwtPalette(
        night = Color(0xFF0B1524),
        nightSoft = Color(0xFF172640),
        backgroundTop = Color(0xFF284470),
        primary = Color(0xFF82F5D0),
        secondary = Color(0xFFC7ADFF),
        alert = Color(0xFFFF8FB0),
    ),
    AwtThemeId.VioletNight to AwtPalette(
        night = Color(0xFF151022),
        nightSoft = Color(0xFF251B37),
        backgroundTop = Color(0xFF432E61),
        primary = Color(0xFFDCCAFF),
        secondary = Color(0xFFF2C778),
        alert = Color(0xFFFF8A9D),
    ),
    AwtThemeId.Forest to AwtPalette(
        night = Color(0xFF0D1712),
        nightSoft = Color(0xFF17271E),
        backgroundTop = Color(0xFF294835),
        primary = Color(0xFFC5E9CD),
        secondary = Color(0xFFE7C877),
        alert = Color(0xFFFF927E),
    ),
    AwtThemeId.Moon to AwtPalette(
        night = Color(0xFF111720),
        nightSoft = Color(0xFF202837),
        backgroundTop = Color(0xFF3A485D),
        primary = Color(0xFFDDEAF6),
        secondary = Color(0xFFB9C9EC),
        alert = Color(0xFFFF9A91),
    ),
    AwtThemeId.Oled to AwtPalette(
        night = Color.Black,
        nightSoft = Color(0xFF0D0D0D),
        backgroundTop = Color.Black,
        primary = Color(0xFFE1FFF6),
        secondary = Color(0xFFF6D27A),
        alert = Color(0xFFFF887A),
        textPrimary = Color(0xFFF7F7F7),
        textMuted = Color(0xFFB8B8B8),
    ),
)

val LocalAwtPalette = staticCompositionLocalOf { palettes.getValue(AwtThemeId.Dawn) }
val LocalNumeralStyle = staticCompositionLocalOf { NumeralStyle.Arabic }

fun paletteFor(themeId: AwtThemeId): AwtPalette =
    palettes[themeId] ?: palettes.getValue(AwtThemeId.Dawn)

fun backgroundPreview(tone: BackgroundTone): Color = when (tone) {
    BackgroundTone.Theme -> Color(0xFF23374B)
    BackgroundTone.Midnight -> Color(0xFF172743)
    BackgroundTone.Ocean -> Color(0xFF0D4051)
    BackgroundTone.Plum -> Color(0xFF3A2353)
    BackgroundTone.Forest -> Color(0xFF254534)
    BackgroundTone.Slate -> Color(0xFF303846)
    BackgroundTone.Black -> Color.Black
}

private fun AwtPalette.withBackground(tone: BackgroundTone): AwtPalette = when (tone) {
    BackgroundTone.Theme -> this
    BackgroundTone.Midnight -> copy(
        night = Color(0xFF080D19),
        nightSoft = Color(0xFF111A2A),
        backgroundTop = Color(0xFF172743),
    )
    BackgroundTone.Ocean -> copy(
        night = Color(0xFF041319),
        nightSoft = Color(0xFF09252E),
        backgroundTop = Color(0xFF0D4051),
    )
    BackgroundTone.Plum -> copy(
        night = Color(0xFF120A1D),
        nightSoft = Color(0xFF241431),
        backgroundTop = Color(0xFF3A2353),
    )
    BackgroundTone.Forest -> copy(
        night = Color(0xFF07120C),
        nightSoft = Color(0xFF102218),
        backgroundTop = Color(0xFF254534),
    )
    BackgroundTone.Slate -> copy(
        night = Color(0xFF0D1118),
        nightSoft = Color(0xFF1A202A),
        backgroundTop = Color(0xFF303846),
    )
    BackgroundTone.Black -> copy(
        night = Color.Black,
        nightSoft = Color(0xFF0B0D10),
        backgroundTop = Color.Black,
    )
}

private fun AwtPalette.withCustomColors(
    backgroundArgb: Int?,
    textArgb: Int?,
): AwtPalette {
    val customBackground = backgroundArgb?.let { Color(it) }
    val customText = textArgb?.let { Color(it) }
    val backgroundAdjusted = if (customBackground == null) {
        this
    } else {
        copy(
            night = customBackground,
            nightSoft = lerp(customBackground, Color.White, 0.075f),
            backgroundTop = lerp(customBackground, Color.White, 0.16f),
        )
    }
    return if (customText == null) {
        backgroundAdjusted
    } else {
        backgroundAdjusted.copy(
            textPrimary = customText,
            textMuted = lerp(customText, backgroundAdjusted.night, 0.34f),
        )
    }
}

object AwtThemeColors {
    val current: AwtPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalAwtPalette.current
}

@Composable
fun AwtTheme(
    themeId: AwtThemeId = AwtThemeId.Dawn,
    numeralStyle: NumeralStyle = NumeralStyle.Arabic,
    backgroundTone: BackgroundTone = BackgroundTone.Theme,
    customBackgroundArgb: Int? = null,
    customTextArgb: Int? = null,
    content: @Composable () -> Unit,
) {
    val palette = paletteFor(themeId)
        .withBackground(backgroundTone)
        .withCustomColors(customBackgroundArgb, customTextArgb)
    val scheme = darkColorScheme(
        primary = palette.primary,
        onPrimary = palette.night,
        secondary = palette.secondary,
        onSecondary = palette.night,
        tertiary = palette.alert,
        background = palette.night,
        onBackground = palette.textPrimary,
        surface = palette.nightSoft,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.backgroundTop,
        onSurfaceVariant = palette.textMuted,
        error = palette.alert,
    )
    androidx.compose.runtime.CompositionLocalProvider(
        LocalAwtPalette provides palette,
        LocalNumeralStyle provides numeralStyle,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography(),
            content = content,
        )
    }
}
