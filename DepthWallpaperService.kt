package com.depthwallpaper.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi

/**
 * Renders two layers on top of each other:
 *  - a blurred, slightly-zoomed background that shifts a lot with tilt/swipe
 *  - a crisp foreground subject cutout that shifts only a little
 *
 * The difference in how far each layer moves is what reads as "depth" —
 * the same trick behind iOS's depth-effect wallpapers.
 */
class DepthWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = DepthEngine()

    inner class DepthEngine : Engine(), SensorEventListener {

        private var background: Bitmap? = null
        private var foreground: Bitmap? = null
        private var visible = false

        private var xOffset = 0.5f
        private var tiltX = 0f
        private var tiltY = 0f

        private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
        private val rotationSensor by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }

        private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            background = WallpaperStore.loadBackground(this@DepthWallpaperService)
            foreground = WallpaperStore.loadForeground(this@DepthWallpaperService)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                applyBlur()
            }
        }

        @RequiresApi(Build.VERSION_CODES.S)
        private fun applyBlur() {
            backgroundPaint.setRenderEffect(
                RenderEffect.createBlurEffect(24f, 24f, Shader.TileMode.CLAMP)
            )
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                registerSensor()
                drawFrame()
            } else {
                sensorManager.unregisterListener(this)
            }
        }

        private fun registerSensor() {
            rotationSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float,
            xOffsetStep: Float, yOffsetStep: Float,
            xPixelOffset: Int, yPixelOffset: Int
        ) {
            this.xOffset = xOffset
            drawFrame()
        }

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                // Small tilt contribution layered on top of the home-screen swipe parallax.
                tiltX = event.values.getOrElse(0) { 0f }.coerceIn(-0.3f, 0.3f)
                tiltY = event.values.getOrElse(1) { 0f }.coerceIn(-0.3f, 0.3f)
                drawFrame()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        private fun drawFrame() {
            if (!visible) return
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                canvas?.let { render(it) }
            } finally {
                canvas?.let { holder.unlockCanvasAndPost(it) }
            }
        }

        private fun render(canvas: Canvas) {
            val w = canvas.width
            val h = canvas.height
            canvas.drawColor(Color.BLACK)

            val bg = background
            val fg = foreground

            // Background shifts further than the foreground — that gap is
            // the core of the depth illusion.
            val bgParallax = 60f
            val fgParallax = 12f
            val swipeShiftBg = (xOffset - 0.5f) * bgParallax
            val swipeShiftFg = (xOffset - 0.5f) * fgParallax

            bg?.let {
                val scale = maxOf(w.toFloat() / it.width, h.toFloat() / it.height) * 1.12f
                val dx = (w - it.width * scale) / 2f + swipeShiftBg + tiltX * 40f
                val dy = (h - it.height * scale) / 2f + tiltY * 40f
                canvas.save()
                canvas.translate(dx, dy)
                canvas.scale(scale, scale)
                canvas.drawBitmap(it, 0f, 0f, backgroundPaint)
                canvas.restore()
            }

            fg?.let {
                val scale = maxOf(w.toFloat() / it.width, h.toFloat() / it.height)
                val dx = (w - it.width * scale) / 2f + swipeShiftFg + tiltX * 8f
                val dy = (h - it.height * scale) / 2f + tiltY * 8f
                canvas.save()
                canvas.translate(dx, dy)
                canvas.scale(scale, scale)
                canvas.drawBitmap(it, 0f, 0f, null)
                canvas.restore()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            drawFrame()
        }

        override fun onDestroy() {
            super.onDestroy()
            sensorManager.unregisterListener(this)
        }
    }
}
