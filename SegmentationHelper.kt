package com.depthwallpaper.app

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

object SegmentationHelper {

    private val options = SelfieSegmenterOptions.Builder()
        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
        .enableRawSizeMask()
        .build()

    /**
     * Returns a bitmap the same size as [source] with everything except the
     * detected subject made transparent — this becomes the "foreground" layer
     * that stays crisp while the background blurs and shifts behind it.
     *
     * Note: selfie segmentation is tuned for people. For pets/objects, swap
     * this out for ML Kit's (currently beta) Subject Segmentation API or a
     * manually-supplied alpha mask.
     */
    suspend fun extractForeground(source: Bitmap): Bitmap = suspendCancellableCoroutine { cont ->
        val segmenter = Segmentation.getClient(options)
        val input = InputImage.fromBitmap(source, 0)

        segmenter.process(input)
            .addOnSuccessListener { mask ->
                try {
                    val maskBuffer = mask.buffer
                    val maskWidth = mask.width
                    val maskHeight = mask.height

                    val scaleX = source.width.toFloat() / maskWidth
                    val scaleY = source.height.toFloat() / maskHeight

                    maskBuffer.rewind()
                    val confidences = FloatArray(maskWidth * maskHeight)
                    maskBuffer.asFloatBuffer().get(confidences)

                    val pixels = IntArray(source.width * source.height)
                    source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)

                    for (y in 0 until source.height) {
                        for (x in 0 until source.width) {
                            val mx = (x / scaleX).toInt().coerceIn(0, maskWidth - 1)
                            val my = (y / scaleY).toInt().coerceIn(0, maskHeight - 1)
                            val confidence = confidences[my * maskWidth + mx]
                            if (confidence < 0.5f) {
                                pixels[y * source.width + x] = Color.TRANSPARENT
                            }
                        }
                    }

                    val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, source.width, 0, 0, source.width, source.height)

                    segmenter.close()
                    cont.resume(result)
                } catch (e: Exception) {
                    segmenter.close()
                    cont.resumeWithException(e)
                }
            }
            .addOnFailureListener { e ->
                segmenter.close()
                cont.resumeWithException(e)
            }
    }
}
