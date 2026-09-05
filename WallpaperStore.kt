package com.depthwallpaper.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

/**
 * Persists the two layers that make up a depth wallpaper:
 *  - foreground: the segmented subject, background made transparent
 *  - background: the original photo (blurred live by the wallpaper service)
 */
object WallpaperStore {
    private const val FOREGROUND_FILE = "depth_foreground.png"
    private const val BACKGROUND_FILE = "depth_background.png"

    fun save(context: Context, foreground: Bitmap, background: Bitmap) {
        writeBitmap(context, foreground, FOREGROUND_FILE)
        writeBitmap(context, background, BACKGROUND_FILE)
    }

    fun loadForeground(context: Context): Bitmap? = readBitmap(context, FOREGROUND_FILE)
    fun loadBackground(context: Context): Bitmap? = readBitmap(context, BACKGROUND_FILE)

    private fun writeBitmap(context: Context, bitmap: Bitmap, name: String) {
        FileOutputStream(File(context.filesDir, name)).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun readBitmap(context: Context, name: String): Bitmap? {
        val file = File(context.filesDir, name)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }
}
