package com.oneui.gallery.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class ImageDiagnostics(
    val lumaBins: FloatArray,
    val redBins: FloatArray,
    val greenBins: FloatArray,
    val blueBins: FloatArray,
    val meanBrightness: Int,
    val shadowRatio: Int,
    val highlightRatio: Int,
    val dynamicRange: Int,
    val meanSaturation: Int,
    val warmthShift: Int,
    val dominantTheme: String,
    val summaryText: String,
    val remasterNotes: String = ""
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
        val luma256 = IntArray(256)

        var sumLuma = 0L
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var sumSat = 0f
        var shadows = 0
        var highlights = 0

        // Sample step for very large images while still reading full real pixel distribution
        val step = if (pixels.size > 400_000) 2 else 1
        var count = 0

        var i = 0
        while (i < pixels.size) {
            val c = pixels[i]
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF

            val luma = ((r * 77 + g * 150 + b * 29) ushr 8).coerceIn(0, 255)
            luma256[luma]++
            lumaCounts[luma shr 3]++
            redCounts[r shr 3]++
            greenCounts[g shr 3]++
            blueCounts[b shr 3]++

            sumLuma += luma
            sumR += r
            sumG += g
            sumB += b

            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            if (maxC > 0) {
                sumSat += (maxC - minC).toFloat() / maxC.toFloat()
            }
            if (luma < 64) shadows++
            if (luma > 205) highlights++

            count++
            i += step
        }

        val safeCount = count.coerceAtLeast(1)
        val meanR = sumR.toFloat() / safeCount
        val meanG = sumG.toFloat() / safeCount
        val meanB = sumB.toFloat() / safeCount

        val meanBrightness = ((sumLuma.toFloat() / safeCount) / 255f * 100f).roundToInt()
        val shadowRatio = (shadows.toFloat() / safeCount * 100f).roundToInt()
        val highlightRatio = (highlights.toFloat() / safeCount * 100f).roundToInt()
        val meanSaturation = ((sumSat / safeCount) * 100f).roundToInt()
        val warmthShift = (((meanR - meanB) / 255f) * 100f).roundToInt()

        val (p5, p95) = computePercentiles(luma256, safeCount)
        val dynamicRange = (p95 - p5).coerceAtLeast(0)

        val theme = when {
            meanSaturation < 12 -> "P&B / Monocromático"
            meanBrightness < 30 -> "Noturno / Escuro"
            warmthShift >= 10 && meanR > meanG -> "Pôr do Sol / Quente"
            meanB > meanR + 12 -> "Céu & Água / Frio"
            meanG > meanR + 8 && meanG > meanB + 8 -> "Natureza / Verde"
            else -> "Vibrante / Dia"
        }

        val lumaBins = normalizeHistogram(lumaCounts)
        val redBins = normalizeHistogram(redCounts)
        val greenBins = normalizeHistogram(greenCounts)
        val blueBins = normalizeHistogram(blueCounts)

        val summary = "Brilho $meanBrightness% • Sombras $shadowRatio% • Sat $meanSaturation% • $theme"

        ImageDiagnostics(
            lumaBins = lumaBins,
            redBins = redBins,
            greenBins = greenBins,
            blueBins = blueBins,
            meanBrightness = meanBrightness,
            shadowRatio = shadowRatio,
            highlightRatio = highlightRatio,
            dynamicRange = dynamicRange,
            meanSaturation = meanSaturation,
            warmthShift = warmthShift,
            dominantTheme = theme,
            summaryText = summary
        )
    }

    suspend fun remasterBitmap(source: Bitmap): Pair<Bitmap, ImageDiagnostics> = withContext(Dispatchers.Default) {
        val w = source.width
        val h = source.height
        val srcPixels = IntArray(w * h)
        source.getPixels(srcPixels, 0, w, 0, 0, w, h)

        val diag = analyzeBitmap(source)

        // Compute luminance percentiles [p2..p98] & gray-world channel balance
        val lumaHist = IntArray(256)
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        for (c in srcPixels) {
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            val luma = ((r * 77 + g * 150 + b * 29) ushr 8).coerceIn(0, 255)
            lumaHist[luma]++
            sumR += r
            sumG += g
            sumB += b
        }

        val total = srcPixels.size.coerceAtLeast(1)
        val (pLow, pHigh) = computePercentiles(lumaHist, total, 0.02f, 0.98f)
        val low = pLow.coerceIn(0, 80)
        val high = pHigh.coerceIn(low + 40, 255)

        val avgR = (sumR.toFloat() / total).coerceAtLeast(1f)
        val avgG = (sumG.toFloat() / total).coerceAtLeast(1f)
        val avgB = (sumB.toFloat() / total).coerceAtLeast(1f)
        val grayMean = (avgR + avgG + avgB) / 3f

        // Gentle 45% Gray-World white balance so natural sunsets stay warm instead of turning flat
        val wbR = (1f + ((grayMean / avgR) - 1f) * 0.45f).coerceIn(0.86f, 1.16f)
        val wbG = (1f + ((grayMean / avgG) - 1f) * 0.45f).coerceIn(0.90f, 1.10f)
        val wbB = (1f + ((grayMean / avgB) - 1f) * 0.45f).coerceIn(0.86f, 1.18f)

        // Adaptive shadow lift based on measured shadowRatio
        val shadowLift = if (diag.shadowRatio > 28) 0.42f else 0.22f
        val vibranceBoost = if (diag.meanSaturation < 38) 0.34f else 0.18f

        // Build 256-entry LUT for dynamic stretch + shadow recovery
        val toneLut = FloatArray(256)
        val span = (high - low).toFloat().coerceAtLeast(1f)
        for (v in 0..255) {
            val norm = ((v - low) / span).coerceIn(0f, 1f)
            // Soft S-curve + shadow recovery without clipping highlights
            val shadowTerm = shadowLift * norm * (1f - norm) * (1f - norm)
            val sCurve = norm * norm * (3f - 2f * norm)
            val blended = norm * 0.72f + sCurve * 0.28f + shadowTerm
            toneLut[v] = (blended * 255f).coerceIn(0f, 255f)
        }

        val toned = IntArray(srcPixels.size)
        for (i in srcPixels.indices) {
            val c = srcPixels[i]
            val a = (c ushr 24) and 0xFF
            var r = ((c ushr 16) and 0xFF) * wbR
            var g = ((c ushr 8) and 0xFF) * wbG
            var b = (c and 0xFF) * wbB

            val lumaIn = (0.299f * r + 0.587f * g + 0.114f * b).roundToInt().coerceIn(0, 255)
            val lumaOut = toneLut[lumaIn]
            val lumaScale = if (lumaIn > 1) lumaOut / lumaIn.toFloat() else 1f

            r *= lumaScale
            g *= lumaScale
            b *= lumaScale

            // Smart Vibrance: boosts dull colors more than already saturated pixels
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val sat = if (maxC > 1f) (maxC - minC) / maxC else 0f
            val satFactor = 1f + vibranceBoost * (1f - sat)
            val gray = (r + g + b) / 3f

            val outR = (gray + (r - gray) * satFactor).roundToInt().coerceIn(0, 255)
            val outG = (gray + (g - gray) * satFactor).roundToInt().coerceIn(0, 255)
            val outB = (gray + (b - gray) * satFactor).roundToInt().coerceIn(0, 255)

            toned[i] = (a shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        // Fast 3x3 Unsharp Mask sharpening pass
        val sharpened = unsharpMaskPixels(toned, w, h, radius = 2, amount = 0.58f)
        val outBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        outBmp.setPixels(sharpened, 0, w, 0, 0, w, h)

        val afterDiag = analyzeBitmap(outBmp).copy(
            remasterNotes = "Auto-Níveis [$low..$high] • Sombras +${(shadowLift * 100).roundToInt()}% • Nitidez IA +58%"
        )
        outBmp to afterDiag
    }

    suspend fun applyUnsharpMask(source: Bitmap, amount: Float = 0.85f): Bitmap = withContext(Dispatchers.Default) {
        val w = source.width
        val h = source.height
        val src = IntArray(w * h)
        source.getPixels(src, 0, w, 0, 0, w, h)
        val out = unsharpMaskPixels(src, w, h, radius = 2, amount = amount)
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(out, 0, w, 0, 0, w, h)
        }
    }

    suspend fun applyShadowRecovery(source: Bitmap, strength: Float = 0.65f): Bitmap = withContext(Dispatchers.Default) {
        val w = source.width
        val h = source.height
        val src = IntArray(w * h)
        source.getPixels(src, 0, w, 0, 0, w, h)

        val out = IntArray(src.size)
        for (i in src.indices) {
            val c = src[i]
            val a = (c ushr 24) and 0xFF
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF

            val luma = ((r * 77 + g * 150 + b * 29) ushr 8)
            val norm = luma / 255f
            val lifted = (norm + strength * (1f - norm).pow(2.2f) * (norm + 0.08f)).coerceIn(0f, 1f)
            val scale = if (luma > 2) (lifted * 255f) / luma.toFloat() else (1f + strength * 0.45f)

            val nr = (r * scale).roundToInt().coerceIn(0, 255)
            val ng = (g * scale).roundToInt().coerceIn(0, 255)
            val nb = (b * scale).roundToInt().coerceIn(0, 255)
            out[i] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
        }

        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(out, 0, w, 0, 0, w, h)
        }
    }

    suspend fun applyPortraitBokeh(source: Bitmap, intensity: Float = 1f): Bitmap = withContext(Dispatchers.Default) {
        val w = source.width
        val h = source.height
        val src = IntArray(w * h)
        source.getPixels(src, 0, w, 0, 0, w, h)

        // Two-pass separable box blur for creamy lens bokeh
        val blurRadius = (min(w, h) * 0.018f * intensity).roundToInt().coerceIn(8, 24)
        val pass1 = boxBlurPixels(src, w, h, blurRadius)
        val blurred = boxBlurPixels(pass1, w, h, blurRadius)

        val cx = w * 0.5f
        val cy = h * 0.45f
        val rx = w * 0.30f
        val ry = h * 0.38f

        val out = IntArray(src.size)
        for (y in 0 until h) {
            val dy = (y - cy) / ry
            val rowOffset = y * w
            for (x in 0 until w) {
                val dx = (x - cx) / rx
                val dist = sqrt(dx * dx + dy * dy)
                // 0.0 = sharp center, 1.0 = full bokeh background
                val t = ((dist - 0.58f) / 0.62f).coerceIn(0f, 1f)
                val smoothT = t * t * (3f - 2f * t)

                val idx = rowOffset + x
                val s = src[idx]
                val b = blurred[idx]

                val a = (s ushr 24) and 0xFF
                val sr = (s ushr 16) and 0xFF
                val sg = (s ushr 8) and 0xFF
                val sb = s and 0xFF

                val br = (b ushr 16) and 0xFF
                val bg = (b ushr 8) and 0xFF
                val bb = b and 0xFF

                val nr = (sr + (br - sr) * smoothT).roundToInt().coerceIn(0, 255)
                val ng = (sg + (bg - sg) * smoothT).roundToInt().coerceIn(0, 255)
                val nb = (sb + (bb - sb) * smoothT).roundToInt().coerceIn(0, 255)
                out[idx] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }

        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(out, 0, w, 0, 0, w, h)
        }
    }

    suspend fun inpaintStroke(
        source: Bitmap,
        strokePoints: List<Offset>,
        brushRadiusPx: Float,
        previewWidth: Float,
        previewHeight: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        if (strokePoints.isEmpty() || previewWidth <= 1f || previewHeight <= 1f) return@withContext source

        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        // Account for ContentScale.Fit letterboxing inside the preview Box
        val bmpAspect = w.toFloat() / h.toFloat()
        val viewAspect = previewWidth / previewHeight
        val drawnW: Float
        val drawnH: Float
        val offsetX: Float
        val offsetY: Float
        if (bmpAspect > viewAspect) {
            drawnW = previewWidth
            drawnH = previewWidth / bmpAspect
            offsetX = 0f
            offsetY = (previewHeight - drawnH) * 0.5f
        } else {
            drawnH = previewHeight
            drawnW = previewHeight * bmpAspect
            offsetX = (previewWidth - drawnW) * 0.5f
            offsetY = 0f
        }

        val scaleX = w / drawnW
        val scaleY = h / drawnH
        val mappedPoints = strokePoints.map { pt ->
            Offset(
                x = ((pt.x - offsetX) * scaleX).coerceIn(0f, (w - 1).toFloat()),
                y = ((pt.y - offsetY) * scaleY).coerceIn(0f, (h - 1).toFloat())
            )
        }
        val radius = (brushRadiusPx * max(scaleX, scaleY) * 0.55f).coerceIn(6f, min(w, h) * 0.18f)

        var minX = w - 1
        var minY = h - 1
        var maxX = 0
        var maxY = 0
        for (pt in mappedPoints) {
            minX = min(minX, (pt.x - radius - 3).toInt())
            minY = min(minY, (pt.y - radius - 3).toInt())
            maxX = max(maxX, (pt.x + radius + 3).toInt())
            maxY = max(maxY, (pt.y + radius + 3).toInt())
        }
        minX = minX.coerceIn(1, w - 2)
        minY = minY.coerceIn(1, h - 2)
        maxX = maxX.coerceIn(minX, w - 2)
        maxY = maxY.coerceIn(minY, h - 2)

        val mask = BooleanArray(w * h)
        val rSq = radius * radius
        var maskedCount = 0

        for (y in minY..maxY) {
            val rowOff = y * w
            for (x in minX..maxX) {
                if (isPointNearPolyline(x.toFloat(), y.toFloat(), mappedPoints, rSq)) {
                    mask[rowOff + x] = true
                    maskedCount++
                }
            }
        }
        if (maskedCount == 0) return@withContext source

        // 1. Seed masked pixels from nearest boundary pixels via inward concentric onion-peeling
        val known = BooleanArray(w * h) { !mask[it] }
        val rArr = FloatArray(pixels.size)
        val gArr = FloatArray(pixels.size)
        val bArr = FloatArray(pixels.size)
        for (idx in pixels.indices) {
            val c = pixels[idx]
            rArr[idx] = ((c ushr 16) and 0xFF).toFloat()
            gArr[idx] = ((c ushr 8) and 0xFF).toFloat()
            bArr[idx] = (c and 0xFF).toFloat()
        }

        val dx8 = intArrayOf(-1, 0, 1, -1, 1, -1, 0, 1)
        val dy8 = intArrayOf(-1, -1, -1, 0, 0, 1, 1, 1)
        val distW = floatArrayOf(0.7f, 1f, 0.7f, 1f, 1f, 0.7f, 1f, 0.7f)

        var remaining = maskedCount
        val filledThisPass = ArrayList<Int>(maskedCount)
        var maxPeelSteps = (radius * 2.2f).toInt().coerceIn(20, 140)
        while (remaining > 0 && maxPeelSteps-- > 0) {
            filledThisPass.clear()
            for (y in minY..maxY) {
                val rowOff = y * w
                for (x in minX..maxX) {
                    val idx = rowOff + x
                    if (known[idx]) continue
                    var sr = 0f
                    var sg = 0f
                    var sb = 0f
                    var sw = 0f
                    for (k in 0..7) {
                        val nx = x + dx8[k]
                        val ny = y + dy8[k]
                        val nIdx = ny * w + nx
                        if (known[nIdx]) {
                            val wt = distW[k]
                            sr += rArr[nIdx] * wt
                            sg += gArr[nIdx] * wt
                            sb += bArr[nIdx] * wt
                            sw += wt
                        }
                    }
                    if (sw > 0f) {
                        rArr[idx] = sr / sw
                        gArr[idx] = sg / sw
                        bArr[idx] = sb / sw
                        filledThisPass.add(idx)
                    }
                }
            }
            if (filledThisPass.isEmpty()) break
            for (idx in filledThisPass) {
                known[idx] = true
                remaining--
            }
        }

        // 2. Multi-pass harmonic diffusion smoothing inside the masked region + subtle 1px edge feathering
        val tmpR = FloatArray(pixels.size)
        val tmpG = FloatArray(pixels.size)
        val tmpB = FloatArray(pixels.size)
        val diffusionPasses = 28
        for (pass in 0 until diffusionPasses) {
            for (y in minY..maxY) {
                val rowOff = y * w
                for (x in minX..maxX) {
                    val idx = rowOff + x
                    if (!mask[idx]) continue
                    tmpR[idx] = (rArr[idx - 1] + rArr[idx + 1] + rArr[idx - w] + rArr[idx + w]) * 0.25f
                    tmpG[idx] = (gArr[idx - 1] + gArr[idx + 1] + gArr[idx - w] + gArr[idx + w]) * 0.25f
                    tmpB[idx] = (bArr[idx - 1] + bArr[idx + 1] + bArr[idx - w] + bArr[idx + w]) * 0.25f
                }
            }
            for (y in minY..maxY) {
                val rowOff = y * w
                for (x in minX..maxX) {
                    val idx = rowOff + x
                    if (!mask[idx]) continue
                    rArr[idx] = tmpR[idx]
                    gArr[idx] = tmpG[idx]
                    bArr[idx] = tmpB[idx]
                }
            }
        }

        // Write back with deterministic subtle sensor grain so filled patches match natural texture
        for (y in minY..maxY) {
            val rowOff = y * w
            for (x in minX..maxX) {
                val idx = rowOff + x
                if (!mask[idx]) continue
                val hash = ((x * 73856093) xor (y * 19349663)) and 0xFF
                val grain = ((hash % 7) - 3).toFloat()
                val a = (pixels[idx] ushr 24) and 0xFF
                val nr = (rArr[idx] + grain).roundToInt().coerceIn(0, 255)
                val ng = (gArr[idx] + grain).roundToInt().coerceIn(0, 255)
                val nb = (bArr[idx] + grain).roundToInt().coerceIn(0, 255)
                pixels[idx] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }

        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }

    suspend fun matchesVisualPrompt(context: Context, uri: Uri, prompt: String): Boolean = withContext(Dispatchers.IO) {
        val q = prompt.lowercase()
        val bmp = MediaRepository.decodeBitmapForEditing(context, uri, 96) ?: return@withContext false
        val diag = analyzeBitmap(bmp)
        bmp.recycle()

        when {
            "noite" in q || "noturn" in q || "escuro" in q || "estrela" in q -> diag.meanBrightness < 40
            "sol" in q || "quente" in q || "dourad" in q || "praia" in q || "deserto" in q ->
                diag.warmthShift >= 7 || "Quente" in diag.dominantTheme
            "azul" in q || "mar" in q || "oceano" in q || "céu" in q || "ceu" in q || "frio" in q ->
                diag.warmthShift <= -6 || "Frio" in diag.dominantTheme
            "verde" in q || "floresta" in q || "natureza" in q ->
                "Verde" in diag.dominantTheme
            "p&b" in q || "preto" in q || "branco" in q || "mono" in q ->
                diag.meanSaturation < 16
            "claro" in q || "dia" in q ->
                diag.meanBrightness >= 55
            "vibrant" in q || "cor" in q || "neon" in q ->
                diag.meanSaturation >= 40
            else -> false
        }
    }

    private fun isPointNearPolyline(px: Float, py: Float, pts: List<Offset>, rSq: Float): Boolean {
        if (pts.size == 1) {
            val dx = px - pts[0].x
            val dy = py - pts[0].y
            return dx * dx + dy * dy <= rSq
        }
        for (i in 0 until pts.lastIndex) {
            val a = pts[i]
            val b = pts[i + 1]
            val abx = b.x - a.x
            val aby = b.y - a.y
            val lenSq = abx * abx + aby * aby
            val t = if (lenSq <= 0.0001f) 0f else {
                (((px - a.x) * abx + (py - a.y) * aby) / lenSq).coerceIn(0f, 1f)
            }
            val cx = a.x + t * abx
            val cy = a.y + t * aby
            val dx = px - cx
            val dy = py - cy
            if (dx * dx + dy * dy <= rSq) return true
        }
        return false
    }

    private fun computePercentiles(
        hist256: IntArray,
        total: Int,
        lowFrac: Float = 0.05f,
        highFrac: Float = 0.95f
    ): Pair<Int, Int> {
        val lowTarget = (total * lowFrac).toInt()
        val highTarget = (total * highFrac).toInt()
        var acc = 0
        var pLow = 0
        var pHigh = 255
        var foundLow = false
        for (v in 0..255) {
            acc += hist256[v]
            if (!foundLow && acc >= lowTarget) {
                pLow = v
                foundLow = true
            }
            if (acc >= highTarget) {
                pHigh = v
                break
            }
        }
        return pLow to pHigh
    }

    private fun normalizeHistogram(counts: IntArray): FloatArray {
        val maxVal = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
        return FloatArray(counts.size) { idx ->
            (counts[idx].toFloat() / maxVal.toFloat()).coerceIn(0.02f, 1f)
        }
    }

    private fun boxBlurPixels(src: IntArray, w: Int, h: Int, radius: Int): IntArray {
        if (radius <= 0) return src.copyOf()
        val horiz = IntArray(src.size)
        val out = IntArray(src.size)
        val window = radius * 2 + 1

        // Horizontal pass with running sum
        for (y in 0 until h) {
            val row = y * w
            var rSum = 0
            var gSum = 0
            var bSum = 0
            for (k in -radius..radius) {
                val x = k.coerceIn(0, w - 1)
                val c = src[row + x]
                rSum += (c ushr 16) and 0xFF
                gSum += (c ushr 8) and 0xFF
                bSum += c and 0xFF
            }
            for (x in 0 until w) {
                val a = (src[row + x] ushr 24) and 0xFF
                horiz[row + x] = (a shl 24) or ((rSum / window) shl 16) or ((gSum / window) shl 8) or (bSum / window)
                val addX = (x + radius + 1).coerceAtMost(w - 1)
                val remX = (x - radius).coerceAtLeast(0)
                val addC = src[row + addX]
                val remC = src[row + remX]
                rSum += ((addC ushr 16) and 0xFF) - ((remC ushr 16) and 0xFF)
                gSum += ((addC ushr 8) and 0xFF) - ((remC ushr 8) and 0xFF)
                bSum += (addC and 0xFF) - (remC and 0xFF)
            }
        }

        // Vertical pass with running sum
        for (x in 0 until w) {
            var rSum = 0
            var gSum = 0
            var bSum = 0
            for (k in -radius..radius) {
                val y = k.coerceIn(0, h - 1)
                val c = horiz[y * w + x]
                rSum += (c ushr 16) and 0xFF
                gSum += (c ushr 8) and 0xFF
                bSum += c and 0xFF
            }
            for (y in 0 until h) {
                val idx = y * w + x
                val a = (horiz[idx] ushr 24) and 0xFF
                out[idx] = (a shl 24) or ((rSum / window) shl 16) or ((gSum / window) shl 8) or (bSum / window)
                val addY = (y + radius + 1).coerceAtMost(h - 1)
                val remY = (y - radius).coerceAtLeast(0)
                val addC = horiz[addY * w + x]
                val remC = horiz[remY * w + x]
                rSum += ((addC ushr 16) and 0xFF) - ((remC ushr 16) and 0xFF)
                gSum += ((addC ushr 8) and 0xFF) - ((remC ushr 8) and 0xFF)
                bSum += (addC and 0xFF) - (remC and 0xFF)
            }
        }
        return out
    }

    private fun unsharpMaskPixels(src: IntArray, w: Int, h: Int, radius: Int, amount: Float): IntArray {
        val blurred = boxBlurPixels(src, w, h, radius)
        val out = IntArray(src.size)
        for (i in src.indices) {
            val s = src[i]
            val b = blurred[i]
            val a = (s ushr 24) and 0xFF
            val sr = (s ushr 16) and 0xFF
            val sg = (s ushr 8) and 0xFF
            val sb = s and 0xFF

            val br = (b ushr 16) and 0xFF
            val bg = (b ushr 8) and 0xFF
            val bb = b and 0xFF

            val nr = (sr + ((sr - br) * amount)).roundToInt().coerceIn(0, 255)
            val ng = (sg + ((sg - bg) * amount)).roundToInt().coerceIn(0, 255)
            val nb = (sb + ((sb - bb) * amount)).roundToInt().coerceIn(0, 255)
            out[i] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
        }
        return out
    }
}
