package jp.awt.clock.ui

import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.PathMeasure
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
internal fun AwtMotionBackdrop(
    scene: MotionScene,
    animate: Boolean,
    pagePosition: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = AwtThemeColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.backgroundTop,
                        colors.night,
                        colors.night.copy(alpha = 0.99f),
                    ),
                ),
            ),
    ) {
        when (scene) {
            MotionScene.Aurora -> OpenGlAuroraBackdrop(
                background = colors.night,
                accent = colors.primary,
                secondary = colors.secondary,
                animate = animate,
                pagePosition = pagePosition,
                modifier = Modifier.fillMaxSize(),
            )
            MotionScene.TidalLight -> WebGlTidalBackdrop(
                background = colors.night,
                accent = colors.primary,
                secondary = colors.secondary,
                animate = animate,
                pagePosition = pagePosition,
                modifier = Modifier.fillMaxSize(),
            )
            MotionScene.Still -> MotionBackdropCanvas(
                scene = scene,
                animate = false,
                pagePosition = pagePosition,
                colors = colors,
            )
        }
        content()
    }
}

@Composable
private fun MotionBackdropCanvas(
    scene: MotionScene,
    animate: Boolean,
    pagePosition: Float,
    colors: AwtPalette,
) {
    var phase by remember(scene) { mutableFloatStateOf(0f) }
    LaunchedEffect(scene, animate) {
        if (!animate || scene == MotionScene.Still) {
            phase = 0f
            return@LaunchedEffect
        }
        val durationMillis = if (scene == MotionScene.TidalLight) 14_000L else 18_000L
        val frameDelayMillis = 42L
        val startedAt = SystemClock.uptimeMillis()
        while (true) {
            phase = ((SystemClock.uptimeMillis() - startedAt) % durationMillis) /
                durationMillis.toFloat()
            delay(frameDelayMillis)
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        when (scene) {
            MotionScene.Aurora -> drawAuroraField(
                colors = colors,
                phase = phase,
                pagePosition = pagePosition,
            )
            MotionScene.TidalLight -> drawTidalLightField(
                colors = colors,
                phase = phase,
                pagePosition = pagePosition,
            )
            MotionScene.Still -> drawStillField(colors)
        }
    }
}

private fun DrawScope.drawAuroraField(
    colors: AwtPalette,
    phase: Float,
    pagePosition: Float,
) {
    if (size.minDimension <= 0f) return
    val angle = phase * 2f * PI.toFloat()
    val parallax = pagePosition * size.width * 0.032f
    val oledScale = if (colors.night == Color.Black) 0.52f else 1f

    fun light(
        center: Offset,
        radius: Float,
        color: Color,
        alpha: Float,
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to color.copy(alpha = alpha * oledScale),
                0.52f to color.copy(alpha = alpha * 0.34f * oledScale),
                1f to Color.Transparent,
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }

    light(
        center = Offset(
            x = size.width * (0.25f + 0.22f * cos(angle)) - parallax,
            y = size.height * (0.20f + 0.14f * sin(angle)),
        ),
        radius = size.minDimension * 0.48f,
        color = colors.primary,
        alpha = 0.24f,
    )
    light(
        center = Offset(
            x = size.width * (0.76f + 0.21f * sin(angle + 1.1f)) - parallax * 0.72f,
            y = size.height * (0.70f + 0.15f * cos(angle + 0.4f)),
        ),
        radius = size.minDimension * 0.64f,
        color = colors.secondary,
        alpha = 0.13f,
    )
    light(
        center = Offset(
            x = size.width * (0.52f + 0.28f * cos(angle * 2f + 2.2f)) - parallax * 0.44f,
            y = size.height * (0.43f + 0.19f * sin(angle + 2.5f)),
        ),
        radius = size.minDimension * 0.55f,
        color = colors.primary,
        alpha = 0.075f,
    )

    // A fourth broad field replaces line-shaped ribbons. Every center uses an
    // integer phase harmonic, so the loop closes exactly without a jump, while
    // the radial falloff keeps every boundary optically soft.
    light(
        center = Offset(
            x = size.width * (0.18f + 0.34f * sin(angle * 2f + 0.8f)) - parallax * 0.28f,
            y = size.height * (0.84f + 0.10f * cos(angle * 2f + 1.6f)),
        ),
        radius = size.minDimension * 0.78f,
        color = colors.secondary,
        alpha = 0.065f,
    )
}

private fun DrawScope.drawStillField(colors: AwtPalette) {
    if (colors.night == Color.Black) return
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                colors.primary.copy(alpha = 0.11f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.24f, size.height * 0.18f),
            radius = size.minDimension * 0.52f,
        ),
        radius = size.minDimension * 0.52f,
        center = Offset(size.width * 0.24f, size.height * 0.18f),
    )
}

private fun DrawScope.drawTidalLightField(
    colors: AwtPalette,
    phase: Float,
    pagePosition: Float,
) {
    if (size.minDimension <= 0f) return
    val time = phase * 2f * PI.toFloat()
    val parallaxPhase = pagePosition * 0.28f
    val oledScale = if (colors.night == Color.Black) 0.48f else 1f
    val aspect = (size.width / size.height).coerceAtLeast(0.45f)
    val ratios = floatArrayOf(1f, 1.18f, 0.82f, 1.42f, 0.68f, 1.05f)
    val weights = floatArrayOf(1f, 0.42f, 0.36f, 0.28f, 0.22f, 0.12f)
    val directions = floatArrayOf(0f, 0.48f, -0.41f, 0.22f, -0.17f, PI.toFloat() + 0.08f)
    val offsets = floatArrayOf(0f, 1.1f, -0.7f, 2f, -2.2f, 0.4f)

    drawCircle(
        brush = Brush.radialGradient(
            0f to colors.primary.copy(alpha = 0.070f * oledScale),
            0.58f to colors.primary.copy(alpha = 0.018f * oledScale),
            1f to Color.Transparent,
            center = Offset(size.width * 0.24f, size.height * 0.20f),
            radius = size.minDimension * 0.62f,
        ),
        radius = size.minDimension * 0.62f,
        center = Offset(size.width * 0.24f, size.height * 0.20f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            0f to colors.secondary.copy(alpha = 0.045f * oledScale),
            1f to Color.Transparent,
            center = Offset(size.width * 0.83f, size.height * 0.72f),
            radius = size.minDimension * 0.54f,
        ),
        radius = size.minDimension * 0.54f,
        center = Offset(size.width * 0.83f, size.height * 0.72f),
    )

    fun waveHeight(u: Float, v: Float): Float {
        val x = (u - 0.5f) * aspect
        val y = v - 0.5f
        var value = 0f
        ratios.indices.forEach { index ->
            val direction = directions[index]
            val spatial = x * sin(direction) - y * cos(direction)
            value += sin(
                spatial * 17f * ratios[index] -
                    time * sqrt(ratios[index]) +
                    offsets[index] +
                    parallaxPhase,
            ) * weights[index]
        }
        return value / 1.48f
    }

    // Seven very faint crest contours are sampled from the same six-component
    // directional field as the shoreline study. The larger displacement keeps
    // their motion visibly curved while their low opacity prevents them from
    // reading as drawn horizontal stripes; local caustics and foam remain the
    // visible focus.
    repeat(7) { row ->
        val rowFraction = (row + 1f) / 8f
        val amplitude = 0.032f + 0.008f * sin(row * 1.31f)
        val path = Path()
        val sampleStep = 1f / 36f
        var u = -sampleStep
        var first = true
        while (u <= 1f + sampleStep) {
            val displacedV = rowFraction +
                waveHeight(u, rowFraction) * amplitude +
                (u - 0.5f) * sin(row * 1.73f + 0.4f) * 0.070f
            val x = u * size.width
            val y = displacedV * size.height
            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
            u += sampleStep
        }
        drawPath(
            path = path,
            color = colors.primary.copy(alpha = 0.014f * oledScale),
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
            path = path,
            color = Color.White.copy(
                alpha = (0.007f + rowFraction * 0.005f) * oledScale,
            ),
            style = Stroke(width = 0.75.dp.toPx(), cap = StrokeCap.Round),
        )
    }

    // Local caustics follow closed elliptical paths through the interference field.
    // They remain short and irregular, matching focused light rather than a mesh.
    repeat(8) { band ->
        val seed = band * 0.6180339f
        val u = fractional(seed * 1.73f + phase * (0.10f + band * 0.006f))
        val baseV = 0.10f + fractional(seed * 2.41f) * 0.78f
        val v = baseV + waveHeight(u, baseV) * 0.026f
        val length = size.minDimension * (0.025f + fractional(seed * 3.17f) * 0.035f)
        val tilt = sin(time * 0.63f + band * 1.7f)
        val centerX = u * size.width
        val centerY = v * size.height
        val path = Path().apply {
            moveTo(centerX - length, centerY + tilt * length * 0.22f)
            cubicTo(
                centerX - length * 0.35f,
                centerY - tilt * length * 0.62f,
                centerX + length * 0.35f,
                centerY + tilt * length * 0.46f,
                centerX + length,
                centerY - tilt * length * 0.18f,
            )
        }
        drawPath(
            path = path,
            color = colors.primary.copy(alpha = 0.040f * oledScale),
            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.044f * oledScale),
            style = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round),
        )
    }

    // Whitecaps first cling to a crest, then broaden and drift into a dim residual
    // trace. This preserves the attachment-to-residue motion from the simulation.
    repeat(6) { patch ->
        val seed = patch * 0.6180339f
        val life = fractional(phase * 1.16f + seed)
        val birth = (life / 0.10f).coerceIn(0f, 1f)
        val fade = (1f - ((life - 0.58f) / 0.42f).coerceIn(0f, 1f))
        val attachment = (1f - life * 1.45f).coerceIn(0f, 1f)
        val alpha = birth * fade
        val u = fractional(seed * 1.73f + phase * 0.20f)
        val baseV = 0.14f + fractional(seed * 2.41f) * 0.70f
        val v = baseV + waveHeight(u, baseV) * 0.030f + (1f - attachment) * 0.018f
        val centerX = u * size.width
        val centerY = v * size.height
        val length = size.minDimension * (0.035f + fractional(seed * 3.17f) * 0.045f)
        val curl = sin(time * 0.72f + patch * 1.7f) * length * 0.24f
        val crestPath = Path().apply {
            moveTo(centerX - length, centerY + curl * 0.25f)
            cubicTo(
                centerX - length * 0.35f,
                centerY - curl,
                centerX + length * 0.30f,
                centerY + curl * 0.42f,
                centerX + length,
                centerY + curl * 0.12f,
            )
        }
        drawPath(
            path = crestPath,
            color = colors.primary.copy(alpha = alpha * 0.075f * oledScale),
            style = Stroke(width = (4.5f + attachment * 2f).dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
            path = crestPath,
            color = Color.White.copy(alpha = alpha * attachment * 0.14f * oledScale),
            style = Stroke(width = 0.85.dp.toPx(), cap = StrokeCap.Round),
        )

        val residual = Path().apply {
            val drift = (1f - attachment) * 10.dp.toPx()
            moveTo(centerX - length * 1.20f, centerY + drift)
            cubicTo(
                centerX - length * 0.3f,
                centerY + drift - curl * 0.35f,
                centerX + length * 0.35f,
                centerY + drift + curl * 0.20f,
                centerX + length * 1.16f,
                centerY + drift,
            )
        }
        drawPath(
            path = residual,
            color = colors.primary.copy(
                alpha = alpha * (1f - attachment) * 0.035f * oledScale,
            ),
            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

private fun fractional(value: Float): Float = value - floor(value)

@Composable
internal fun MorphingTimeDigit(
    character: Char,
    color: Color,
    glowColor: Color,
    fontSize: TextUnit,
    width: Dp,
    reduceMotion: Boolean,
    motionDirection: Int,
    delayMillis: Int,
) {
    val density = LocalDensity.current
    val numeralStyle = LocalNumeralStyle.current
    val height = with(density) { fontSize.toDp() * 1.13f }
    val progress = remember { androidx.compose.animation.core.Animatable(1f) }
    var fromCharacter by remember { mutableStateOf(character) }
    var targetCharacter by remember { mutableStateOf(character) }

    LaunchedEffect(character, reduceMotion) {
        if (reduceMotion) {
            fromCharacter = character
            targetCharacter = character
            progress.snapTo(1f)
        } else if (character != targetCharacter) {
            fromCharacter = targetCharacter
            targetCharacter = character
            progress.snapTo(0f)
            if (delayMillis > 0) delay(delayMillis.toLong())
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 460,
                    easing = FastOutSlowInEasing,
                ),
            )
            fromCharacter = targetCharacter
        }
    }

    if (!character.isDigit() && character != '–' && character != '-') {
        Box(
            modifier = Modifier
                .width(width)
                .height(height),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = character.toString(),
                color = color,
                fontSize = fontSize,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                style = TextStyle.Default,
            )
        }
        return
    }

    Canvas(
        modifier = Modifier
            .width(width)
            .height(height)
            .semantics { contentDescription = character.toString() },
    ) {
        drawMorphingDigit(
            fromCharacter = fromCharacter,
            targetCharacter = targetCharacter,
            progress = progress.value,
            color = color,
            glowColor = glowColor,
            motionDirection = motionDirection,
            numeralStyle = numeralStyle,
        )
    }
}

private data class GlyphLine(val start: Offset, val end: Offset)

private fun DrawScope.glyphLines(): List<GlyphLine> {
    val left = size.width * 0.20f
    val right = size.width * 0.80f
    val upper = size.height * 0.09f
    val middle = size.height * 0.50f
    val lower = size.height * 0.91f
    val inset = size.width * 0.10f
    val joint = size.height * 0.055f
    return listOf(
        GlyphLine(Offset(left + inset, upper), Offset(right - inset, upper)),
        GlyphLine(Offset(right, upper + joint), Offset(right, middle - joint)),
        GlyphLine(Offset(right, middle + joint), Offset(right, lower - joint)),
        GlyphLine(Offset(right - inset, lower), Offset(left + inset, lower)),
        GlyphLine(Offset(left, lower - joint), Offset(left, middle + joint)),
        GlyphLine(Offset(left, middle - joint), Offset(left, upper + joint)),
        GlyphLine(Offset(left + inset, middle), Offset(right - inset, middle)),
    )
}

internal fun digitSegmentMask(character: Char): Int = when (character) {
    '0' -> bits(0, 1, 2, 3, 4, 5)
    '1' -> bits(1, 2)
    '2' -> bits(0, 1, 6, 4, 3)
    '3' -> bits(0, 1, 6, 2, 3)
    '4' -> bits(5, 6, 1, 2)
    '5' -> bits(0, 5, 6, 2, 3)
    '6' -> bits(0, 5, 6, 4, 2, 3)
    '7' -> bits(0, 1, 2)
    '8' -> bits(0, 1, 2, 3, 4, 5, 6)
    '9' -> bits(0, 1, 2, 3, 5, 6)
    '–', '-' -> bits(6)
    else -> 0
}

private fun bits(vararg indices: Int): Int =
    indices.fold(0) { mask, index -> mask or (1 shl index) }

private fun DrawScope.drawMorphingDigit(
    fromCharacter: Char,
    targetCharacter: Char,
    progress: Float,
    color: Color,
    glowColor: Color,
    motionDirection: Int,
    numeralStyle: NumeralStyle,
) {
    if (numeralStyle == NumeralStyle.Arabic) {
        drawArabicMorphingDigit(
            fromCharacter = fromCharacter,
            targetCharacter = targetCharacter,
            progress = progress,
            color = color,
            glowColor = glowColor,
            motionDirection = motionDirection,
        )
        return
    }

    val p = FastOutSlowInEasing.transform(progress.coerceIn(0f, 1f))
    val lines = glyphLines()
    val fromMask = digitSegmentMask(fromCharacter)
    val targetMask = digitSegmentMask(targetCharacter)
    val common = fromMask and targetMask
    val removed = (0 until 7).filter { fromMask and (1 shl it) != 0 && common and (1 shl it) == 0 }
    val availableAdded = (0 until 7)
        .filter { targetMask and (1 shl it) != 0 && common and (1 shl it) == 0 }
        .toMutableList()
    val stroke = (size.width * 0.082f).coerceAtLeast(1.8.dp.toPx())
    val momentum = -motionDirection * sin(p * PI.toFloat()) * size.height * 0.045f
    val scale = 1f

    fun transform(point: Offset): Offset =
        center + (point - center) * scale + Offset(0f, momentum)

    fun drawLineLayer(line: GlyphLine, alpha: Float) {
        if (alpha <= 0.001f) return
        val start = transform(line.start)
        val end = transform(line.end)
        drawLine(
            color = glowColor.copy(alpha = 0.085f * alpha),
            start = start,
            end = end,
            strokeWidth = stroke * 2.65f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color.copy(alpha = 0.94f * alpha),
            start = start,
            end = end,
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.12f * alpha),
            start = start,
            end = end,
            strokeWidth = stroke * 0.23f,
            cap = StrokeCap.Round,
        )
    }

    (0 until 7).filter { common and (1 shl it) != 0 }.forEach { index ->
        drawLineLayer(lines[index], 1f)
    }

    removed.forEach { oldIndex ->
        val oldLine = lines[oldIndex]
        val nearestAdded = availableAdded.minByOrNull { newIndex ->
            lineDistanceSquared(oldLine, lines[newIndex])
        }
        if (nearestAdded != null) {
            availableAdded.remove(nearestAdded)
            drawLineLayer(interpolateLine(oldLine, lines[nearestAdded], p), 1f)
        } else {
            val midpoint = (oldLine.start + oldLine.end) / 2f
            drawLineLayer(
                GlyphLine(
                    start = lerpOffset(oldLine.start, midpoint, p),
                    end = lerpOffset(oldLine.end, midpoint, p),
                ),
                1f - p,
            )
        }
    }

    availableAdded.forEach { newIndex ->
        val targetLine = lines[newIndex]
        val midpoint = (targetLine.start + targetLine.end) / 2f
        drawLineLayer(
            GlyphLine(
                start = lerpOffset(midpoint, targetLine.start, p),
                end = lerpOffset(midpoint, targetLine.end, p),
            ),
            p,
        )
    }

}

private fun DrawScope.drawArabicMorphingDigit(
    fromCharacter: Char,
    targetCharacter: Char,
    progress: Float,
    color: Color,
    glowColor: Color,
    motionDirection: Int,
) {
    val p = FastOutSlowInEasing.transform(progress.coerceIn(0f, 1f))
    val contourPairs = StandardDigitOutlines.transition(fromCharacter, targetCharacter)
    val momentum = -motionDirection * sin(p * PI.toFloat()) * size.height * 0.022f
    val maximumGlyphWidth = size.width * 0.94f
    val glyphHeight = (maximumGlyphWidth / StandardDigitOutlines.aspectRatio)
        .coerceAtMost(size.height * 0.88f)
    val glyphWidth = glyphHeight * StandardDigitOutlines.aspectRatio
    val glyphLeft = (size.width - glyphWidth) / 2f
    val glyphTop = (size.height - glyphHeight) / 2f
    val contours = contourPairs.map { (fromPoints, targetPoints) ->
        fromPoints.indices.map { index ->
            val normalized = lerpOffset(fromPoints[index], targetPoints[index], p)
            Offset(
                x = glyphLeft + normalized.x * glyphWidth,
                y = glyphTop + normalized.y * glyphHeight + momentum,
            )
        }
    }
    fun Path.addClosedContour(points: List<Offset>) {
        if (points.isEmpty()) return
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { point -> lineTo(point.x, point.y) }
        close()
    }
    val outerPath = Path().apply {
        contours.firstOrNull()?.let { addClosedContour(it) }
    }
    val holesPath = Path().apply {
        fillType = PathFillType.NonZero
        contours.drop(1).forEach { addClosedContour(it) }
    }
    val path = if (contours.size > 1) {
        Path.combine(PathOperation.Difference, outerPath, holesPath)
    } else {
        outerPath
    }
    val glowWidth = (size.width * 0.035f).coerceAtLeast(1.1.dp.toPx())
    val fromEndpointAlpha = smoothEndpointAlpha(1f - p, 0.78f)
    val targetEndpointAlpha = smoothEndpointAlpha(p, 0.78f)
    val morphAlpha = (1f - maxOf(fromEndpointAlpha, targetEndpointAlpha))
        .coerceIn(0f, 1f)

    drawPath(
        path = path,
        color = glowColor.copy(alpha = 0.10f * morphAlpha),
        style = Stroke(
            width = glowWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
    drawPath(
        path = path,
        color = color.copy(alpha = 0.98f * morphAlpha),
    )
    fun drawNativeEndpoint(character: Char, alpha: Float) {
        if (alpha <= 0.001f) return
        val endpointGlowArgb = glowColor.toArgb()
        val endpointFillArgb = color.toArgb()
        val nativePath = StandardDigitOutlines.transformedPath(
            character = character,
            left = glyphLeft,
            top = glyphTop,
            width = glyphWidth,
            height = glyphHeight,
            verticalOffset = momentum,
        )
        drawIntoCanvas { canvas ->
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                strokeWidth = glowWidth
                this.color = endpointGlowArgb
                this.alpha = (alpha * 0.10f * 255f).toInt().coerceIn(0, 255)
            }
            canvas.nativeCanvas.drawPath(nativePath, glowPaint)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                this.color = endpointFillArgb
                this.alpha = (alpha * 0.98f * 255f).toInt().coerceIn(0, 255)
            }
            canvas.nativeCanvas.drawPath(nativePath, fillPaint)
        }
    }
    drawNativeEndpoint(fromCharacter, fromEndpointAlpha)
    drawNativeEndpoint(targetCharacter, targetEndpointAlpha)
}

private fun smoothEndpointAlpha(value: Float, start: Float): Float {
    val normalized = ((value - start) / (1f - start)).coerceIn(0f, 1f)
    return normalized * normalized * (3f - 2f * normalized)
}

private data class StandardDigitGlyph(
    val contours: List<List<Offset>>,
)

/**
 * Android標準のsans-serif字形を同じ輪郭点数へ正規化する。手描きの一筆線では
 * 潰れやすかった4や、0/6/8/9のカウンターも実際のフォント輪郭のまま補間できる。
 */
private object StandardDigitOutlines {
    private const val CONTOUR_SAMPLES = 84
    private const val MAX_CONTOURS = 3

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        textSize = 1_000f
        style = Paint.Style.FILL
    }
    private val commonBounds: RectF by lazy {
        RectF().also { union ->
            ('0'..'9').forEach { digit ->
                val bounds = RectF()
                pathFor(digit).computeBounds(bounds, true)
                if (union.isEmpty) union.set(bounds) else union.union(bounds)
            }
        }
    }
    val aspectRatio: Float
        get() = (commonBounds.width() / commonBounds.height()).coerceAtLeast(0.01f)

    private val glyphs = mutableMapOf<Char, StandardDigitGlyph>()
    private val transitions =
        mutableMapOf<Pair<Char, Char>, List<Pair<List<Offset>, List<Offset>>>>()

    fun transformedPath(
        character: Char,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        verticalOffset: Float,
    ): AndroidPath {
        val bounds = commonBounds
        val scaleX = width / bounds.width().coerceAtLeast(0.01f)
        val scaleY = height / bounds.height().coerceAtLeast(0.01f)
        val transform = Matrix().apply {
            setScale(scaleX, scaleY)
            postTranslate(
                left - bounds.left * scaleX,
                top - bounds.top * scaleY + verticalOffset,
            )
        }
        return pathFor(character).apply { transform(transform) }
    }

    @Synchronized
    fun transition(
        fromCharacter: Char,
        targetCharacter: Char,
    ): List<Pair<List<Offset>, List<Offset>>> =
        transitions.getOrPut(fromCharacter to targetCharacter) {
            val from = glyph(fromCharacter).contours
            val target = glyph(targetCharacter).contours
            buildList {
                val fromOuter = from.firstOrNull()
                val targetOuter = target.firstOrNull()
                if (fromOuter != null && targetOuter != null) {
                    add(fromOuter to alignedDigitContour(fromOuter, targetOuter))
                } else if (fromOuter != null) {
                    add(fromOuter to collapsedContour(fromOuter))
                } else if (targetOuter != null) {
                    add(collapsedContour(targetOuter) to targetOuter)
                }
                addAll(pairInnerContours(from.drop(1), target.drop(1)))
            }
        }

    private fun glyph(character: Char): StandardDigitGlyph =
        glyphs.getOrPut(character) {
            val path = pathFor(
                if (character.isDigit() || character == '–' || character == '-') {
                    character
                } else {
                    '–'
                },
            )
            val bounds = commonBounds
            val measure = PathMeasure(path, false)
            val contours = mutableListOf<List<Offset>>()
            val position = FloatArray(2)
            do {
                val length = measure.length
                if (length > 0.5f) {
                    val points = List(CONTOUR_SAMPLES) { sample ->
                        measure.getPosTan(length * sample / CONTOUR_SAMPLES, position, null)
                        Offset(
                            x = ((position[0] - bounds.left) / bounds.width()).coerceIn(0f, 1f),
                            y = ((position[1] - bounds.top) / bounds.height()).coerceIn(0f, 1f),
                        )
                    }
                    contours += canonicalize(points)
                }
            } while (measure.nextContour())
            val retained = contours
                .sortedByDescending { abs(digitContourArea(it)) }
                .take(MAX_CONTOURS)
            val outer = retained.firstOrNull()
            val inner = retained.drop(1).sortedBy { contourCenter(it).y }
            StandardDigitGlyph(
                contours = if (outer == null) emptyList() else listOf(outer) + inner,
            )
        }

    private fun pathFor(character: Char): AndroidPath = AndroidPath().also { path ->
        val value = if (character == '-') "–" else character.toString()
        val advance = paint.measureText("0")
        val valueAdvance = paint.measureText(value)
        paint.getTextPath(value, 0, value.length, (advance - valueAdvance) / 2f, 0f, path)
    }

    private fun canonicalize(points: List<Offset>): List<Offset> {
        if (points.isEmpty()) return points
        val start = points.indices.minByOrNull { index ->
            points[index].y * 2f + points[index].x
        } ?: 0
        return List(points.size) { offset -> points[(start + offset) % points.size] }
    }

    private fun pairInnerContours(
        sourceContours: List<List<Offset>>,
        targetContours: List<List<Offset>>,
    ): List<Pair<List<Offset>, List<Offset>>> {
        val source = sourceContours.toMutableList()
        val target = targetContours.toMutableList()
        val result = mutableListOf<Pair<List<Offset>, List<Offset>>>()
        while (source.isNotEmpty() && target.isNotEmpty()) {
            var bestSourceIndex = 0
            var bestTargetIndex = 0
            var bestScore = Float.POSITIVE_INFINITY
            source.indices.forEach { sourceIndex ->
                target.indices.forEach { targetIndex ->
                    val score = distanceSquared(
                        contourCenter(source[sourceIndex]),
                        contourCenter(target[targetIndex]),
                    )
                    if (score < bestScore) {
                        bestScore = score
                        bestSourceIndex = sourceIndex
                        bestTargetIndex = targetIndex
                    }
                }
            }
            val fromContour = source.removeAt(bestSourceIndex)
            val targetContour = target.removeAt(bestTargetIndex)
            result += fromContour to alignedDigitContour(fromContour, targetContour)
        }
        source.forEach { contour ->
            result += contour to collapsedContour(contour)
        }
        target.forEach { contour ->
            result += collapsedContour(contour) to contour
        }
        return result
    }

    private fun collapsedContour(contour: List<Offset>): List<Offset> {
        val center = contourCenter(contour)
        return List(CONTOUR_SAMPLES) { center }
    }

    private fun contourCenter(points: List<Offset>): Offset =
        if (points.isEmpty()) {
            Offset(0.5f, 0.5f)
        } else {
            points.fold(Offset.Zero) { total, point -> total + point } / points.size.toFloat()
        }

}

internal fun alignedDigitContour(
    source: List<Offset>,
    target: List<Offset>,
): List<Offset> {
    if (source.isEmpty() || target.isEmpty() || source.size != target.size) return target
    var bestScore = Float.POSITIVE_INFINITY
    var best = target
    val candidate = if (digitContourArea(source) * digitContourArea(target) < 0f) {
        target.reversed()
    } else {
        target
    }
    for (shift in candidate.indices) {
        var score = 0f
        for (index in source.indices) {
            score += distanceSquared(
                source[index],
                candidate[(index + shift) % candidate.size],
            )
        }
        if (score < bestScore) {
            bestScore = score
            best = List(candidate.size) { index ->
                candidate[(index + shift) % candidate.size]
            }
        }
    }
    return best
}

internal fun digitContourArea(points: List<Offset>): Float {
    if (points.size < 3) return 0f
    var area = 0f
    points.indices.forEach { index ->
        val current = points[index]
        val next = points[(index + 1) % points.size]
        area += current.x * next.y - next.x * current.y
    }
    return area * 0.5f
}

internal fun arabicDigitTemplate(character: Char): List<Offset> = when (character) {
    '0' -> curvedDigit(
        .50f, .02f,
        .25f, .02f, .10f, .20f, .10f, .50f,
        .10f, .80f, .26f, .98f, .50f, .98f,
        .74f, .98f, .90f, .80f, .90f, .50f,
        .90f, .20f, .75f, .02f, .50f, .02f,
    )
    '1' -> curvedDigit(
        .25f, .25f,
        .34f, .18f, .47f, .07f, .55f, .04f,
        .59f, .29f, .56f, .65f, .56f, .90f,
        .47f, .91f, .34f, .92f, .25f, .93f,
        .46f, .92f, .68f, .91f, .84f, .92f,
    )
    '2' -> curvedDigit(
        .14f, .25f,
        .18f, .09f, .35f, .03f, .53f, .03f,
        .75f, .03f, .87f, .16f, .85f, .31f,
        .82f, .46f, .61f, .56f, .42f, .68f,
        .29f, .77f, .19f, .85f, .14f, .93f,
        .36f, .92f, .62f, .92f, .87f, .92f,
    )
    '3' -> curvedDigit(
        .17f, .14f,
        .33f, .03f, .59f, .02f, .76f, .11f,
        .90f, .18f, .86f, .36f, .73f, .43f,
        .65f, .47f, .57f, .49f, .49f, .50f,
        .59f, .50f, .70f, .51f, .78f, .59f,
        .91f, .70f, .84f, .87f, .68f, .94f,
        .51f, 1.00f, .29f, .95f, .15f, .85f,
    )
    '4' -> curvedDigit(
        .69f, .04f,
        .60f, .24f, .40f, .49f, .18f, .63f,
        .34f, .64f, .57f, .64f, .83f, .64f,
        .79f, .49f, .74f, .22f, .69f, .04f,
        .70f, .27f, .70f, .70f, .70f, .97f,
    )
    '5' -> curvedDigit(
        .84f, .06f,
        .67f, .04f, .45f, .05f, .25f, .07f,
        .23f, .18f, .20f, .32f, .18f, .43f,
        .31f, .37f, .48f, .36f, .62f, .40f,
        .83f, .45f, .90f, .62f, .82f, .77f,
        .72f, .95f, .43f, 1.00f, .20f, .87f,
        .16f, .84f, .13f, .80f, .11f, .77f,
    )
    '6' -> curvedDigit(
        .78f, .13f,
        .67f, .02f, .46f, .02f, .31f, .15f,
        .15f, .29f, .10f, .55f, .18f, .75f,
        .25f, .95f, .50f, 1.00f, .69f, .90f,
        .87f, .81f, .90f, .59f, .77f, .47f,
        .65f, .34f, .43f, .35f, .28f, .45f,
        .18f, .51f, .14f, .62f, .17f, .73f,
    )
    '7' -> curvedDigit(
        .13f, .08f,
        .35f, .05f, .64f, .05f, .88f, .07f,
        .78f, .20f, .64f, .36f, .55f, .52f,
        .47f, .68f, .43f, .82f, .41f, .95f,
    )
    '8' -> curvedDigit(
        .50f, .49f,
        .30f, .43f, .20f, .31f, .25f, .17f,
        .30f, .02f, .70f, .02f, .77f, .17f,
        .84f, .31f, .69f, .43f, .50f, .49f,
        .30f, .55f, .16f, .66f, .20f, .82f,
        .24f, .99f, .73f, 1.00f, .81f, .82f,
        .88f, .66f, .70f, .55f, .50f, .49f,
    )
    '9' -> curvedDigit(
        .82f, .53f,
        .71f, .63f, .49f, .64f, .31f, .54f,
        .13f, .43f, .14f, .20f, .29f, .10f,
        .44f, .00f, .70f, .05f, .80f, .24f,
        .88f, .47f, .84f, .70f, .72f, .84f,
        .61f, .97f, .40f, .98f, .23f, .88f,
    )
    '–', '-' -> curvedDigit(
        .18f, .50f,
        .36f, .49f, .64f, .49f, .82f, .50f,
    )
    else -> curvedDigit(
        .50f, .50f,
        .50f, .50f, .50f, .50f, .50f, .50f,
    )
}

private fun curvedDigit(vararg coordinates: Float): List<Offset> {
    require(coordinates.size >= 8 && (coordinates.size - 2) % 6 == 0)
    val points = mutableListOf(Offset(coordinates[0], coordinates[1]))
    var start = points.first()
    var offset = 2
    while (offset < coordinates.size) {
        val firstControl = Offset(coordinates[offset], coordinates[offset + 1])
        val secondControl = Offset(coordinates[offset + 2], coordinates[offset + 3])
        val end = Offset(coordinates[offset + 4], coordinates[offset + 5])
        for (sample in 1..12) {
            val t = sample / 12f
            val oneMinusT = 1f - t
            points += start * (oneMinusT * oneMinusT * oneMinusT) +
                firstControl * (3f * oneMinusT * oneMinusT * t) +
                secondControl * (3f * oneMinusT * t * t) +
                end * (t * t * t)
        }
        start = end
        offset += 6
    }
    return points
}

internal fun resampleDigitPolyline(
    source: List<Offset>,
    sampleCount: Int = 56,
): List<Offset> {
    require(sampleCount >= 2)
    if (source.size < 2) return List(sampleCount) { source.firstOrNull() ?: Offset.Zero }

    val cumulative = FloatArray(source.size)
    for (index in 1 until source.size) {
        cumulative[index] = cumulative[index - 1] +
            sqrt(distanceSquared(source[index - 1], source[index]))
    }
    val totalLength = cumulative.last()
    if (totalLength <= 0.0001f) return List(sampleCount) { source.first() }

    var segment = 1
    return List(sampleCount) { sample ->
        val distance = totalLength * sample / (sampleCount - 1f)
        while (segment < cumulative.lastIndex && cumulative[segment] < distance) {
            segment++
        }
        val segmentStart = cumulative[segment - 1]
        val segmentLength = (cumulative[segment] - segmentStart).coerceAtLeast(0.0001f)
        lerpOffset(
            source[segment - 1],
            source[segment],
            ((distance - segmentStart) / segmentLength).coerceIn(0f, 1f),
        )
    }
}

private fun smoothDigitPath(points: List<Offset>): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().x, points.first().y)
    for (index in 0 until points.lastIndex) {
        val previous = points[if (index == 0) index else index - 1]
        val current = points[index]
        val next = points[index + 1]
        val after = points[if (index + 2 > points.lastIndex) index + 1 else index + 2]
        val firstControl = current + (next - previous) * 0.17f
        val secondControl = next - (after - current) * 0.17f
        cubicTo(
            firstControl.x,
            firstControl.y,
            secondControl.x,
            secondControl.y,
            next.x,
            next.y,
        )
    }
}

private fun lineDistanceSquared(first: GlyphLine, second: GlyphLine): Float {
    val direct = distanceSquared(first.start, second.start) + distanceSquared(first.end, second.end)
    val reversed = distanceSquared(first.start, second.end) + distanceSquared(first.end, second.start)
    return minOf(direct, reversed)
}

private fun interpolateLine(first: GlyphLine, second: GlyphLine, progress: Float): GlyphLine {
    val direct = distanceSquared(first.start, second.start) + distanceSquared(first.end, second.end)
    val reversed = distanceSquared(first.start, second.end) + distanceSquared(first.end, second.start)
    return if (direct <= reversed) {
        GlyphLine(
            lerpOffset(first.start, second.start, progress),
            lerpOffset(first.end, second.end, progress),
        )
    } else {
        GlyphLine(
            lerpOffset(first.start, second.end, progress),
            lerpOffset(first.end, second.start, progress),
        )
    }
}

private fun distanceSquared(first: Offset, second: Offset): Float {
    val dx = first.x - second.x
    val dy = first.y - second.y
    return dx * dx + dy * dy
}

private fun lerpOffset(start: Offset, end: Offset, progress: Float): Offset =
    start + (end - start) * progress

internal fun timerRingProgress(remainingMillis: Long, durationMillis: Long): Float {
    if (durationMillis <= 0L) return 0f
    return (1f - remainingMillis.toDouble().div(durationMillis).toFloat()).coerceIn(0f, 1f)
}

@Composable
internal fun TimeProgressHalo(
    modifier: Modifier,
    primary: Color,
    orbitProgress: Float,
    completionProgress: Float? = null,
    animate: Boolean,
) {
    TimeProgressHaloCanvas(
        modifier = modifier,
        primary = primary,
        orbitProgress = orbitProgress,
        completionProgress = completionProgress,
        animate = animate,
    )
}

@Composable
private fun TimeProgressHaloCanvas(
    modifier: Modifier,
    primary: Color,
    orbitProgress: Float,
    completionProgress: Float?,
    animate: Boolean,
) {
    Canvas(modifier) {
        val maxRadius = size.minDimension / 2f - 10.dp.toPx()
        drawCircle(
            color = primary.copy(alpha = 0.07f),
            radius = maxRadius,
            style = Stroke(width = 1.dp.toPx()),
        )

        completionProgress?.let { completion ->
            drawArc(
                color = primary.copy(alpha = 0.20f),
                startAngle = -90f,
                sweepAngle = completion.coerceIn(0f, 1f) * 360f,
                useCenter = false,
                topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                size = Size(maxRadius * 2f, maxRadius * 2f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Butt),
            )
        }

        if (!animate) return@Canvas

        val phase = ((orbitProgress % 1f) + 1f) % 1f * 360f
        val glowBrush = Brush.sweepGradient(
            0f to Color.Transparent,
            0.30f to Color.Transparent,
            0.50f to primary.copy(alpha = 0.015f),
            0.68f to primary.copy(alpha = 0.070f),
            0.84f to primary.copy(alpha = 0.21f),
            0.93f to primary.copy(alpha = 0.36f),
            0.98f to primary.copy(alpha = 0.16f),
            1f to Color.Transparent,
            center = center,
        )
        val coreBrush = Brush.sweepGradient(
            0f to Color.Transparent,
            0.36f to Color.Transparent,
            0.55f to primary.copy(alpha = 0.025f),
            0.72f to primary.copy(alpha = 0.15f),
            0.86f to primary.copy(alpha = 0.48f),
            0.93f to primary.copy(alpha = 0.68f),
            0.98f to primary.copy(alpha = 0.30f),
            1f to Color.Transparent,
            center = center,
        )
        rotate(phase, center) {
            drawCircle(
                brush = glowBrush,
                radius = maxRadius,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Butt),
            )
            drawCircle(
                brush = coreBrush,
                radius = maxRadius,
                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Butt),
            )
        }
    }
}
