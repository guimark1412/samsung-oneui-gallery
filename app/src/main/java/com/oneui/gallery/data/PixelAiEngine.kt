package com.oneui.gallery.data

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImageDiagnostics(
    val lumaBins: FloatArray,
    val redBins: FloatArray,
    val greenBins: FloatArray,
    val blueBins: FloatArray
)

object PixelAiEngine {

    private const val HIST_BINS = 32

    suspend fun analyzeBitmap(bitmap: Bitmap): ImageDiagnostics = withContext(Dispatchers.Default) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val lumaCounts = IntArray(HIST_BINS)
        val redCounts = IntArray(HIST_BINS)
        val greenCounts = IntArray(HIST_BINS)
        val blueCounts = IntArray(HIST_BINS)

        val step = if (pixels.size > 400_000) 2 else 1
        var i = 0
        while (i < pixels.size) {
            val c = pixels[i]
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            val luma = ((r * 77 + g * 150 + b * 29) ushr 8).coerceIn(0, 255)

            lumaCounts[luma shr 3]++
            redCounts[r shr 3]++
            greenCounts[g shr 3]++
            blueCounts[b shr 3]++

            i += step
        }

        ImageDiagnostics(
            lumaBins = normalizeHistogram(lumaCounts),
            redBins = normalizeHistogram(redCounts),
            greenBins = normalizeHistogram(greenCounts),
            blueBins = normalizeHistogram(blueCounts)
        )
    }

    private fun normalizeHistogram(counts: IntArray): FloatArray {
        val maxVal = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
        return FloatArray(counts.size) { idx ->
            (counts[idx].toFloat() / maxVal.toFloat()).coerceIn(0.02f, 1f)
        }
    }
}
