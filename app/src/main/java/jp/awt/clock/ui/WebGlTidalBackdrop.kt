package jp.awt.clock.ui

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
internal fun WebGlTidalBackdrop(
    background: Color,
    accent: Color,
    secondary: Color,
    animate: Boolean,
    pagePosition: Float,
    modifier: Modifier = Modifier,
) {
    OpenGlMotionBackdrop(
        shaderAsset = "tidal_surface.frag",
        background = background,
        accent = accent,
        secondary = secondary,
        animate = animate,
        pagePosition = pagePosition,
        modifier = modifier,
    )
}

@Composable
internal fun OpenGlAuroraBackdrop(
    background: Color,
    accent: Color,
    secondary: Color,
    animate: Boolean,
    pagePosition: Float,
    modifier: Modifier = Modifier,
) {
    OpenGlMotionBackdrop(
        shaderAsset = "aurora_surface.frag",
        background = background,
        accent = accent,
        secondary = secondary,
        animate = animate,
        pagePosition = pagePosition,
        modifier = modifier,
    )
}

@Composable
private fun OpenGlMotionBackdrop(
    shaderAsset: String,
    background: Color,
    accent: Color,
    secondary: Color,
    animate: Boolean,
    pagePosition: Float,
    modifier: Modifier,
) {
    AndroidView(
        factory = { context ->
            MotionOpenGlView(context, shaderAsset).apply {
                setScene(background, accent, secondary, animate, pagePosition)
            }
        },
        update = { view ->
            view.setScene(background, accent, secondary, animate, pagePosition)
        },
        onRelease = { view ->
            view.release()
        },
        modifier = modifier,
    )
}

/**
 * WebViewによるWebGLは端末側のブロックリストに左右されるため、同じGLSLを
 * OpenGL ES 2.0で直接実行する。描画式はWebGL版と共通で、ネットワークは使わない。
 */
private class MotionOpenGlView(
    context: Context,
    shaderAsset: String,
) : GLSurfaceView(context) {
    private val renderer = MotionRenderer(
        context.assets.open(shaderAsset)
            .bufferedReader()
            .use { it.readText() },
    )
    private var animateScene = true
    private var renderTickScheduled = false
    private var resumed = false

    private val renderTick = object : Runnable {
        override fun run() {
            renderTickScheduled = false
            if (animateScene && isShown && windowVisibility == View.VISIBLE) {
                requestRender()
                renderTickScheduled = true
                postDelayed(this, FRAME_INTERVAL_MILLIS)
            }
        }
    }

    init {
        setEGLContextClientVersion(2)
        setPreserveEGLContextOnPause(true)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun setScene(
        background: Color,
        accent: Color,
        secondary: Color,
        animate: Boolean,
        pagePosition: Float,
    ) {
        renderer.setScene(background, accent, secondary, animate, pagePosition)
        animateScene = animate
        requestRender()
        updateTicker()
    }

    fun release() {
        removeCallbacks(renderTick)
        renderTickScheduled = false
        if (resumed) {
            onPause()
            resumed = false
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!resumed) {
            onResume()
            resumed = true
        }
        updateTicker()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(renderTick)
        renderTickScheduled = false
        if (resumed) {
            onPause()
            resumed = false
        }
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateTicker()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width > 0 && height > 0) {
            // 背景は面状の光なので、長辺の物理pxではなく低解像度で描き、
            // GPU負荷と電池消費を抑えつつSurfaceView側で滑らかに拡大する。
            val scale = (width / TARGET_RENDER_WIDTH).coerceAtLeast(1f)
            holder.setFixedSize(
                (width / scale).toInt().coerceAtLeast(1),
                (height / scale).toInt().coerceAtLeast(1),
            )
        }
    }

    private fun updateTicker() {
        val shouldRun = animateScene && isShown && windowVisibility == View.VISIBLE
        if (shouldRun && !renderTickScheduled) {
            renderTickScheduled = true
            post(renderTick)
        } else if (!shouldRun && renderTickScheduled) {
            removeCallbacks(renderTick)
            renderTickScheduled = false
        }
    }

    private companion object {
        const val FRAME_INTERVAL_MILLIS = 42L
        const val TARGET_RENDER_WIDTH = 320f
    }
}

private class MotionRenderer(
    private val fragmentShaderSource: String,
) : GLSurfaceView.Renderer {
    private val vertices: FloatBuffer = ByteBuffer
        .allocateDirect(FULLSCREEN_VERTICES.size * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(FULLSCREEN_VERTICES)
            position(0)
        }

    @Volatile
    private var background = floatArrayOf(0.04f, 0.08f, 0.12f)

    @Volatile
    private var accent = floatArrayOf(0.51f, 0.91f, 0.85f)

    @Volatile
    private var secondary = floatArrayOf(0.73f, 0.60f, 0.95f)

    @Volatile
    private var pagePosition = 0f

    @Volatile
    private var animate = true

    private var program = 0
    private var width = 1
    private var height = 1
    private var positionLocation = -1
    private var resolutionLocation = -1
    private var timeLocation = -1
    private var backgroundLocation = -1
    private var accentLocation = -1
    private var secondaryLocation = -1
    private var pagePositionLocation = -1
    private var pausedAtNanos = 0L
    private var frozenSeconds = 0f
    private val startedAtNanos = SystemClock.elapsedRealtimeNanos()

    fun setScene(
        background: Color,
        accent: Color,
        secondary: Color,
        animate: Boolean,
        pagePosition: Float,
    ) {
        this.background = background.toRgb()
        this.accent = accent.toRgb()
        this.secondary = secondary.toRgb()
        this.pagePosition = pagePosition
        if (this.animate != animate) {
            if (animate) {
                pausedAtNanos = 0L
            } else {
                frozenSeconds = elapsedSeconds()
                pausedAtNanos = SystemClock.elapsedRealtimeNanos()
            }
            this.animate = animate
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        program = createProgram(VERTEX_SHADER, fragmentShaderSource)
        positionLocation = GLES20.glGetAttribLocation(program, "a_position")
        resolutionLocation = GLES20.glGetUniformLocation(program, "u_resolution")
        timeLocation = GLES20.glGetUniformLocation(program, "u_time")
        backgroundLocation = GLES20.glGetUniformLocation(program, "u_background")
        accentLocation = GLES20.glGetUniformLocation(program, "u_accent")
        secondaryLocation = GLES20.glGetUniformLocation(program, "u_secondary")
        pagePositionLocation = GLES20.glGetUniformLocation(program, "u_page")
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = width.coerceAtLeast(1)
        this.height = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, this.width, this.height)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (program == 0) return
        val backgroundNow = background
        val accentNow = accent
        val secondaryNow = secondary

        GLES20.glUseProgram(program)
        vertices.position(0)
        GLES20.glEnableVertexAttribArray(positionLocation)
        GLES20.glVertexAttribPointer(
            positionLocation,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            vertices,
        )
        GLES20.glUniform2f(resolutionLocation, width.toFloat(), height.toFloat())
        GLES20.glUniform1f(timeLocation, elapsedSeconds())
        GLES20.glUniform3fv(backgroundLocation, 1, backgroundNow, 0)
        GLES20.glUniform3fv(accentLocation, 1, accentNow, 0)
        GLES20.glUniform3fv(secondaryLocation, 1, secondaryNow, 0)
        GLES20.glUniform1f(pagePositionLocation, pagePosition)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        GLES20.glDisableVertexAttribArray(positionLocation)
    }

    private fun elapsedSeconds(): Float {
        if (!animate && pausedAtNanos != 0L) return frozenSeconds
        return (SystemClock.elapsedRealtimeNanos() - startedAtNanos) / 1_000_000_000f
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        return GLES20.glCreateProgram().also { newProgram ->
            GLES20.glAttachShader(newProgram, vertexShader)
            GLES20.glAttachShader(newProgram, fragmentShader)
            GLES20.glLinkProgram(newProgram)
            val status = IntArray(1)
            GLES20.glGetProgramiv(newProgram, GLES20.GL_LINK_STATUS, status, 0)
            check(status[0] == GLES20.GL_TRUE) {
                "Motion shader link failed: ${GLES20.glGetProgramInfoLog(newProgram)}"
            }
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
        }
    }

    private fun compileShader(type: Int, source: String): Int =
        GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val status = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
            check(status[0] == GLES20.GL_TRUE) {
                "Motion shader compile failed: ${GLES20.glGetShaderInfoLog(shader)}"
            }
        }

    private fun Color.toRgb(): FloatArray {
        val argb = toArgb()
        return floatArrayOf(
            ((argb ushr 16) and 0xFF) / 255f,
            ((argb ushr 8) and 0xFF) / 255f,
            (argb and 0xFF) / 255f,
        )
    }

    private companion object {
        val FULLSCREEN_VERTICES = floatArrayOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            -1f, 1f,
            1f, -1f,
            1f, 1f,
        )

        const val VERTEX_SHADER = """
            attribute vec2 a_position;
            void main() {
                gl_Position = vec4(a_position, 0.0, 1.0);
            }
        """
    }
}
