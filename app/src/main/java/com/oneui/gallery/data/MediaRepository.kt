package com.oneui.gallery.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.toArgb
import com.oneui.gallery.model.AlbumItem
import com.oneui.gallery.model.CollageLayout
import com.oneui.gallery.model.CropAspect
import com.oneui.gallery.model.DrawStroke
import com.oneui.gallery.model.MediaItem
import com.oneui.gallery.model.OverlayTextItem
import com.oneui.gallery.model.StoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object MediaRepository {

    private const val PREFS_NAME = "oneui_gallery_prefs"
    private const val KEY_FAVORITES = "favorite_ids"
    private const val KEY_TRASHED = "trashed_ids"
    private const val KEY_PRIVATE = "private_ids"
    private const val KEY_PRIVATE_PIN = "private_pin"
    private const val KEY_DARK_MODE = "dark_mode"

    fun isDarkMode(context: Context): Boolean? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_DARK_MODE)) prefs.getBoolean(KEY_DARK_MODE, true) else null
    }

    fun setDarkMode(context: Context, dark: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE, dark)
            .apply()
    }

    private fun getIdSet(context: Context, key: String): MutableSet<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    private fun saveIdSet(context: Context, key: String, set: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(key, set)
            .apply()
    }

    fun toggleFavorite(context: Context, id: Long): Boolean {
        val favs = getIdSet(context, KEY_FAVORITES)
        val key = id.toString()
        val nowFav = if (favs.contains(key)) {
            favs.remove(key)
            false
        } else {
            favs.add(key)
            true
        }
        saveIdSet(context, KEY_FAVORITES, favs)
        return nowFav
    }

    fun setFavorites(context: Context, ids: List<Long>, favorite: Boolean) {
        val favs = getIdSet(context, KEY_FAVORITES)
        ids.forEach { id ->
            if (favorite) favs.add(id.toString()) else favs.remove(id.toString())
        }
        saveIdSet(context, KEY_FAVORITES, favs)
    }

    fun moveToTrash(context: Context, ids: List<Long>) {
        val trashed = getIdSet(context, KEY_TRASHED)
        ids.forEach { trashed.add(it.toString()) }
        saveIdSet(context, KEY_TRASHED, trashed)
    }

    fun restoreFromTrash(context: Context, ids: List<Long>) {
        val trashed = getIdSet(context, KEY_TRASHED)
        ids.forEach { trashed.remove(it.toString()) }
        saveIdSet(context, KEY_TRASHED, trashed)
    }

    fun moveToPrivate(context: Context, ids: List<Long>) {
        val privates = getIdSet(context, KEY_PRIVATE)
        ids.forEach { privates.add(it.toString()) }
        saveIdSet(context, KEY_PRIVATE, privates)
    }

    fun removeFromPrivate(context: Context, ids: List<Long>) {
        val privates = getIdSet(context, KEY_PRIVATE)
        ids.forEach { privates.remove(it.toString()) }
        saveIdSet(context, KEY_PRIVATE, privates)
    }

    fun getPrivatePin(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PRIVATE_PIN, "0000") ?: "0000"
    }

    fun setPrivatePin(context: Context, pin: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PRIVATE_PIN, pin)
            .apply()
    }

    suspend fun deletePermanently(context: Context, items: List<MediaItem>) = withContext(Dispatchers.IO) {
        val trashed = getIdSet(context, KEY_TRASHED)
        val favs = getIdSet(context, KEY_FAVORITES)
        val privates = getIdSet(context, KEY_PRIVATE)
        items.forEach { item ->
            runCatching {
                context.contentResolver.delete(item.uri, null, null)
            }
            trashed.remove(item.id.toString())
            favs.remove(item.id.toString())
            privates.remove(item.id.toString())
        }
        saveIdSet(context, KEY_TRASHED, trashed)
        saveIdSet(context, KEY_FAVORITES, favs)
        saveIdSet(context, KEY_PRIVATE, privates)
    }

    suspend fun loadMedia(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
        val favs = getIdSet(context, KEY_FAVORITES)
        val trashed = getIdSet(context, KEY_TRASHED)
        val privates = getIdSet(context, KEY_PRIVATE)
        val result = mutableListOf<MediaItem>()

        val now = Calendar.getInstance()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val yesterdayStart = todayStart - 86_400_000L
        val localePt = Locale("pt", "BR")
        val sameYearFmt = SimpleDateFormat("d 'de' MMMM", localePt)
        val otherYearFmt = SimpleDateFormat("d 'de' MMMM 'de' yyyy", localePt)

        fun formatGroup(timestampMs: Long): String {
            if (timestampMs <= 0L) return "Sem data"
            if (timestampMs >= todayStart) return "Hoje"
            if (timestampMs >= yesterdayStart) return "Ontem"
            val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
            return if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                sameYearFmt.format(Date(timestampMs))
            } else {
                otherYearFmt.format(Date(timestampMs))
            }
        }

        // Query Images
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val dateTaken = cursor.getLong(dateTakenCol).let {
                        if (it > 0L) it else cursor.getLong(dateAddedCol) * 1000L
                    }
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    result.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            name = cursor.getString(nameCol) ?: "Imagem_$id",
                            dateTaken = dateTaken,
                            size = cursor.getLong(sizeCol),
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            mimeType = cursor.getString(mimeCol) ?: "image/jpeg",
                            bucketId = cursor.getString(bucketIdCol) ?: "default",
                            bucketName = cursor.getString(bucketNameCol) ?: "Imagens",
                            isVideo = false,
                            isFavorite = favs.contains(id.toString()),
                            isTrashed = trashed.contains(id.toString()),
                            isPrivate = privates.contains(id.toString()),
                            dateGroupLabel = formatGroup(dateTaken)
                        )
                    )
                }
            }
        }

        // Query Videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )

        runCatching {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_TAKEN} DESC, ${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val dateTaken = cursor.getLong(dateTakenCol).let {
                        if (it > 0L) it else cursor.getLong(dateAddedCol) * 1000L
                    }
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    result.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            name = cursor.getString(nameCol) ?: "Video_$id",
                            dateTaken = dateTaken,
                            size = cursor.getLong(sizeCol),
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            mimeType = cursor.getString(mimeCol) ?: "video/mp4",
                            bucketId = cursor.getString(bucketIdCol) ?: "video",
                            bucketName = cursor.getString(bucketNameCol) ?: "Vídeos",
                            duration = cursor.getLong(durationCol),
                            isVideo = true,
                            isFavorite = favs.contains(id.toString()),
                            isTrashed = trashed.contains(id.toString()),
                            isPrivate = privates.contains(id.toString()),
                            dateGroupLabel = formatGroup(dateTaken)
                        )
                    )
                }
            }
        }

        result.sortedByDescending { it.dateTaken }
    }

    fun buildAlbums(activeItems: List<MediaItem>): List<AlbumItem> {
        return activeItems
            .groupBy { it.bucketId }
            .mapNotNull { (bucketId, items) ->
                val sorted = items.sortedByDescending { it.dateTaken }
                val first = sorted.firstOrNull() ?: return@mapNotNull null
                AlbumItem(
                    id = bucketId,
                    name = first.bucketName,
                    coverUri = first.uri,
                    items = sorted,
                    videoCount = sorted.count { it.isVideo }
                )
            }
            .sortedByDescending { it.items.size }
    }

    fun buildStories(activeItems: List<MediaItem>): List<StoryItem> {
        val photos = activeItems.filter { !it.isVideo }
        if (photos.isEmpty()) return emptyList()
        val stories = mutableListOf<StoryItem>()

        val recent = photos.take(8)
        if (recent.isNotEmpty()) {
            stories.add(
                StoryItem(
                    id = "recent",
                    title = "Destaques Recentes",
                    subtitle = "${recent.size} momentos capturados",
                    coverUri = recent.first().uri,
                    items = recent
                )
            )
        }

        val favorites = photos.filter { it.isFavorite }
        if (favorites.isNotEmpty()) {
            stories.add(
                StoryItem(
                    id = "favs",
                    title = "Favoritos da Galeria",
                    subtitle = "${favorites.size} fotos marcadas",
                    coverUri = favorites.first().uri,
                    items = favorites
                )
            )
        }

        val travel = photos.filter {
            it.bucketName.contains("Viagem", ignoreCase = true) ||
                it.bucketName.contains("Camera", ignoreCase = true) ||
                it.bucketName.contains("Câmera", ignoreCase = true)
        }
        if (travel.size >= 2) {
            stories.add(
                StoryItem(
                    id = "travel",
                    title = "Coleção de Viagem & Arte",
                    subtitle = "${travel.size} lembranças especiais",
                    coverUri = travel.first().uri,
                    items = travel.take(10)
                )
            )
        }

        if (photos.size > 1) {
            stories.add(
                StoryItem(
                    id = "all_moments",
                    title = "Todos os Momentos",
                    subtitle = "Retrospectiva com ${photos.size} fotos",
                    coverUri = photos.last().uri,
                    items = photos.take(15)
                )
            )
        }

        return stories
    }

    suspend fun generateSamplePhotos(context: Context): Int = withContext(Dispatchers.IO) {
        data class SampleSpec(
            val title: String,
            val folder: String,
            val c1: Int,
            val c2: Int,
            val c3: Int,
            val style: Int,
            val daysAgo: Int
        )

        val specs = listOf(
            SampleSpec("PorDoSol_Praia", "Câmera", 0xFFFF5E62.toInt(), 0xFFFF9966.toInt(), 0xFF1A1F36.toInt(), 0, 0),
            SampleSpec("Montanhas_Aurora", "Viagem", 0xFF0F2027.toInt(), 0xFF203A43.toInt(), 0xFF00F2FE.toInt(), 1, 0),
            SampleSpec("Cidade_Noturna", "Câmera", 0xFF141E30.toInt(), 0xFF243B55.toInt(), 0xFFF53855.toInt(), 2, 0),
            SampleSpec("Floresta_Nevoa", "Viagem", 0xFF134E5E.toInt(), 0xFF71B280.toInt(), 0xFFE8F5E9.toInt(), 1, 1),
            SampleSpec("Bokeh_Neon", "Screenshots", 0xFF2B1055.toInt(), 0xFF7597DE.toInt(), 0xFFFF4081.toInt(), 2, 1),
            SampleSpec("Deserto_Dourado", "Viagem", 0xFFD38312.toInt(), 0xFFA83279.toInt(), 0xFFFFE082.toInt(), 0, 1),
            SampleSpec("Oceano_Profundo", "Downloads", 0xFF005C97.toInt(), 0xFF363795.toInt(), 0xFF80D8FF.toInt(), 3, 3),
            SampleSpec("OneUI_Geometria", "Screenshots", 0xFF0A0F1E.toInt(), 0xFF387AFF.toInt(), 0xFF00E5FF.toInt(), 3, 4),
            SampleSpec("Retrato_Studio", "Câmera", 0xFF232526.toInt(), 0xFF414345.toInt(), 0xFFFFB74D.toInt(), 2, 5),
            SampleSpec("Valle_Alvorada", "Viagem", 0xFF42275A.toInt(), 0xFF734B6D.toInt(), 0xFFFF8A65.toInt(), 0, 7),
            SampleSpec("Minimal_Waves", "Downloads", 0xFF1D2B64.toInt(), 0xFFF8CDDA.toInt(), 0xFFFFFFFF.toInt(), 3, 10),
            SampleSpec("Noite_Estrelada", "Câmera", 0xFF000428.toInt(), 0xFF004E92.toInt(), 0xFFFFF59D.toInt(), 1, 14)
        )

        var savedCount = 0
        val now = System.currentTimeMillis()

        specs.forEachIndexed { idx, spec ->
            val width = 1080
            val height = if (idx % 3 == 0) 1080 else 1350
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Sky / Background gradient
            paint.shader = LinearGradient(
                0f, 0f, width.toFloat() * 0.4f, height.toFloat(),
                intArrayOf(spec.c1, spec.c2, spec.c3),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.shader = null

            when (spec.style) {
                0 -> {
                    // Sun & layered horizon
                    paint.shader = RadialGradient(
                        width * 0.55f, height * 0.36f, width * 0.35f,
                        intArrayOf(0xFFFFFFFF.toInt(), spec.c3 and 0x00FFFFFF or 0x99000000.toInt(), 0x00000000),
                        null, Shader.TileMode.CLAMP
                    )
                    canvas.drawCircle(width * 0.55f, height * 0.36f, width * 0.35f, paint)
                    paint.shader = null

                    paint.color = 0xEEFFFFFF.toInt()
                    canvas.drawCircle(width * 0.55f, height * 0.36f, width * 0.11f, paint)

                    // Foreground hills
                    for (layer in 0..2) {
                        val path = Path()
                        val baseY = height * (0.62f + layer * 0.11f)
                        path.moveTo(0f, height.toFloat())
                        path.lineTo(0f, baseY)
                        path.cubicTo(
                            width * 0.3f, baseY - 90f + layer * 25f,
                            width * 0.65f, baseY + 70f - layer * 15f,
                            width.toFloat(), baseY - 30f
                        )
                        path.lineTo(width.toFloat(), height.toFloat())
                        path.close()
                        paint.color = (0x55000000 + layer * 0x33000000).toInt()
                        canvas.drawPath(path, paint)
                    }
                }
                1 -> {
                    // Mountain peaks + stars/aurora
                    paint.color = 0xAAFFFFFF.toInt()
                    for (s in 0..28) {
                        val sx = ((s * 173 + idx * 97) % width).toFloat()
                        val sy = ((s * 89 + idx * 53) % (height * 0.45f).toInt()).toFloat()
                        canvas.drawCircle(sx, sy, (s % 4 + 1.5f), paint)
                    }
                    // Peaks
                    val p1 = Path().apply {
                        moveTo(0f, height.toFloat())
                        lineTo(0f, height * 0.72f)
                        lineTo(width * 0.32f, height * 0.45f)
                        lineTo(width * 0.62f, height * 0.68f)
                        lineTo(width * 0.85f, height * 0.48f)
                        lineTo(width.toFloat(), height * 0.66f)
                        lineTo(width.toFloat(), height.toFloat())
                        close()
                    }
                    paint.color = 0xCC0D1321.toInt()
                    canvas.drawPath(p1, paint)
                }
                2 -> {
                    // Bokeh light circles
                    for (b in 0..16) {
                        val bx = ((b * 211 + idx * 131) % width).toFloat()
                        val by = ((b * 193 + idx * 83) % height).toFloat()
                        val br = 45f + (b % 5) * 42f
                        paint.color = if (b % 2 == 0) (spec.c3 and 0x00FFFFFF) or 0x55000000 else 0x33FFFFFF
                        canvas.drawCircle(bx, by, br, paint)
                    }
                }
                else -> {
                    // Abstract One UI Waves & Circles
                    for (w in 0..4) {
                        paint.color = (0x24FFFFFF + (w * 0x10000000)).toInt()
                        canvas.drawCircle(
                            width * (0.2f + w * 0.16f),
                            height * (0.35f + (w % 2) * 0.25f),
                            width * (0.28f - w * 0.03f),
                            paint
                        )
                    }
                }
            }

            // Subtle corner badge
            paint.color = 0x55FFFFFF
            paint.textSize = 36f
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            canvas.drawText(spec.title.replace("_", " "), 52f, height - 56f, paint)

            val timestamp = now - spec.daysAgo * 86_400_000L - (idx * 180_000L)
            val saved = saveBitmapToGallery(
                context = context,
                bitmap = bmp,
                displayName = "${spec.title}_${idx + 1}.jpg",
                subFolder = spec.folder,
                dateTakenMs = timestamp
            )
            bmp.recycle()
            if (saved != null) savedCount++
        }

        savedCount
    }

    suspend fun saveEditedImage(
        context: Context,
        sourceUri: Uri,
        rotationDegrees: Float,
        flipH: Boolean,
        flipV: Boolean,
        cropAspect: CropAspect,
        filterMatrix: FloatArray,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        warmth: Float,
        vignette: Float,
        strokes: List<DrawStroke>,
        overlayTexts: List<OverlayTextItem>,
        previewWidth: Float,
        previewHeight: Float,
        baseBitmap: Bitmap? = null
    ): Uri? = withContext(Dispatchers.IO) {
        val original = baseBitmap?.copy(Bitmap.Config.ARGB_8888, false)
            ?: decodeSampledBitmap(context, sourceUri, 2560)
            ?: return@withContext null

        // 1. Apply rotation & flips
        val transformMatrix = Matrix().apply {
            postRotate(rotationDegrees)
            postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f)
        }
        val transformed = Bitmap.createBitmap(
            original, 0, 0, original.width, original.height, transformMatrix, true
        )
        if (transformed != original) original.recycle()

        // 2. Apply aspect ratio crop centered
        val cropped = if (cropAspect.ratio != null) {
            val targetRatio = cropAspect.ratio
            val currentRatio = transformed.width.toFloat() / transformed.height.toFloat()
            val cropW: Int
            val cropH: Int
            if (currentRatio > targetRatio) {
                cropH = transformed.height
                cropW = (cropH * targetRatio).roundToInt().coerceAtMost(transformed.width)
            } else {
                cropW = transformed.width
                cropH = (cropW / targetRatio).roundToInt().coerceAtMost(transformed.height)
            }
            val startX = ((transformed.width - cropW) / 2).coerceAtLeast(0)
            val startY = ((transformed.height - cropH) / 2).coerceAtLeast(0)
            val c = Bitmap.createBitmap(transformed, startX, startY, cropW, cropH)
            if (c != transformed) transformed.recycle()
            c
        } else {
            transformed
        }

        // 3. Apply ColorMatrix (Filter + Brightness + Contrast + Saturation + Warmth)
        val output = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val combinedMatrix = buildCombinedColorMatrix(
            filterMatrix = filterMatrix,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            warmth = warmth
        )
        paint.colorFilter = ColorMatrixColorFilter(combinedMatrix)
        canvas.drawBitmap(cropped, 0f, 0f, paint)
        paint.colorFilter = null
        cropped.recycle()

        // 4. Vignette
        if (vignette > 0.01f) {
            val radius = (output.width.coerceAtLeast(output.height) * 0.72f)
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    output.width / 2f,
                    output.height / 2f,
                    radius,
                    intArrayOf(0x00000000, ((vignette * 190).roundToInt() shl 24)),
                    floatArrayOf(0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, output.width.toFloat(), output.height.toFloat(), vignettePaint)
        }

        // 5. Strokes mapped from preview coordinates to output bitmap coordinates
        if (strokes.isNotEmpty() && previewWidth > 0f && previewHeight > 0f) {
            val scaleX = output.width / previewWidth
            val scaleY = output.height / previewHeight
            val strokeScale = min(scaleX, scaleY)
            strokes.forEach { stroke ->
                if (stroke.points.size >= 2) {
                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = stroke.color.toArgb()
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        strokeWidth = stroke.strokeWidth * strokeScale
                    }
                    val path = Path()
                    val first = stroke.points.first()
                    path.moveTo(first.x * scaleX, first.y * scaleY)
                    for (i in 1 until stroke.points.size) {
                        val pt = stroke.points[i]
                        path.lineTo(pt.x * scaleX, pt.y * scaleY)
                    }
                    canvas.drawPath(path, strokePaint)
                }
            }
        }

        // 6. Overlay Texts
        overlayTexts.forEach { item ->
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = item.color.toArgb()
                textSize = (output.width * 0.065f) * item.scale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                setShadowLayer(8f, 2f, 4f, 0xAA000000.toInt())
            }
            canvas.drawText(
                item.text,
                item.positionFraction.x * output.width,
                item.positionFraction.y * output.height,
                textPaint
            )
        }

        val savedUri = saveBitmapToGallery(
            context = context,
            bitmap = output,
            displayName = "Editada_${System.currentTimeMillis()}.jpg",
            subFolder = "Galeria Editadas",
            dateTakenMs = System.currentTimeMillis()
        )
        output.recycle()
        savedUri
    }

    fun buildCombinedColorMatrix(
        filterMatrix: FloatArray,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        warmth: Float
    ): ColorMatrix {
        val result = ColorMatrix(filterMatrix)

        // Saturation
        if (saturation != 1f) {
            val satMatrix = ColorMatrix().apply { setSaturation(saturation) }
            result.postConcat(satMatrix)
        }

        // Contrast & Brightness & Warmth
        val bOffset = brightness * 100f
        val cScale = contrast
        val translate = (-0.5f * cScale + 0.5f) * 255f + bOffset
        val rWarm = warmth * 26f
        val bWarm = -warmth * 26f

        val adjustMatrix = ColorMatrix(
            floatArrayOf(
                cScale, 0f, 0f, 0f, translate + rWarm,
                0f, cScale, 0f, 0f, translate,
                0f, 0f, cScale, 0f, translate + bWarm,
                0f, 0f, 0f, 1f, 0f
            )
        )
        result.postConcat(adjustMatrix)
        return result
    }

    suspend fun decodeBitmapForEditing(context: Context, uri: Uri, maxDim: Int = 1280): Bitmap? =
        withContext(Dispatchers.IO) {
            decodeSampledBitmap(context, uri, maxDim)
        }

    suspend fun saveDirectBitmap(
        context: Context,
        bitmap: Bitmap,
        prefix: String = "Remaster_IA",
        subFolder: String = "Galeria Editadas"
    ): Uri? = withContext(Dispatchers.IO) {
        saveBitmapToGallery(
            context = context,
            bitmap = bitmap,
            displayName = "${prefix}_${System.currentTimeMillis()}.jpg",
            subFolder = subFolder,
            dateTakenMs = System.currentTimeMillis()
        )
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOpts)
        }
        var sampleSize = 1
        while (boundsOpts.outWidth / sampleSize > maxDim || boundsOpts.outHeight / sampleSize > maxDim) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        }
    }

    suspend fun saveCollage(
        context: Context,
        items: List<MediaItem>,
        layout: CollageLayout,
        aspectRatio: Float,
        spacingPx: Float,
        cornerRadiusPx: Float,
        bgColorArgb: Int
    ): Uri? = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext null
        val selected = items.take(6)
        val outW = 1440
        val outH = (outW / aspectRatio).roundToInt().coerceIn(720, 2560)
        val output = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(bgColorArgb)

        val scaleFactor = outW / 360f
        val gap = spacingPx * scaleFactor
        val corner = cornerRadiusPx * scaleFactor

        val rects = computeCollageRects(selected.size, layout, outW.toFloat(), outH.toFloat(), gap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        rects.forEachIndexed { idx, rect ->
            val media = selected.getOrNull(idx) ?: return@forEachIndexed
            val bmp = decodeSampledBitmap(context, media.uri, 1080) ?: return@forEachIndexed

            canvas.save()
            val clipPath = Path().apply {
                addRoundRect(rect, corner, corner, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)

            val scale = max(rect.width() / bmp.width, rect.height() / bmp.height)
            val drawW = bmp.width * scale
            val drawH = bmp.height * scale
            val left = rect.left + (rect.width() - drawW) / 2f
            val top = rect.top + (rect.height() - drawH) / 2f
            val dst = RectF(left, top, left + drawW, top + drawH)
            canvas.drawBitmap(bmp, null, dst, paint)
            canvas.restore()
            bmp.recycle()
        }

        val uri = saveBitmapToGallery(
            context = context,
            bitmap = output,
            displayName = "Colagem_${System.currentTimeMillis()}.jpg",
            subFolder = "Colagens One UI",
            dateTakenMs = System.currentTimeMillis()
        )
        output.recycle()
        uri
    }

    private fun computeCollageRects(
        count: Int,
        layout: CollageLayout,
        w: Float,
        h: Float,
        gap: Float
    ): List<RectF> {
        val rects = mutableListOf<RectF>()
        when (layout) {
            CollageLayout.COLUMNS -> {
                val cellW = (w - gap * (count + 1)) / count
                for (i in 0 until count) {
                    val left = gap + i * (cellW + gap)
                    rects.add(RectF(left, gap, left + cellW, h - gap))
                }
            }
            CollageLayout.ROWS -> {
                val cellH = (h - gap * (count + 1)) / count
                for (i in 0 until count) {
                    val top = gap + i * (cellH + gap)
                    rects.add(RectF(gap, top, w - gap, top + cellH))
                }
            }
            CollageLayout.HERO -> {
                if (count == 1) {
                    rects.add(RectF(gap, gap, w - gap, h - gap))
                } else {
                    val heroH = (h - gap * 3f) * 0.58f
                    rects.add(RectF(gap, gap, w - gap, gap + heroH))
                    val subCount = count - 1
                    val subTop = gap * 2f + heroH
                    val subW = (w - gap * (subCount + 1)) / subCount
                    for (i in 0 until subCount) {
                        val left = gap + i * (subW + gap)
                        rects.add(RectF(left, subTop, left + subW, h - gap))
                    }
                }
            }
            CollageLayout.GRID -> {
                val cols = if (count <= 2) count else if (count <= 4) 2 else 3
                val rows = ceil(count / cols.toFloat()).toInt().coerceAtLeast(1)
                val cellW = (w - gap * (cols + 1)) / cols
                val cellH = (h - gap * (rows + 1)) / rows
                for (i in 0 until count) {
                    val r = i / cols
                    val c = i % cols
                    val left = gap + c * (cellW + gap)
                    val top = gap + r * (cellH + gap)
                    rects.add(RectF(left, top, left + cellW, top + cellH))
                }
            }
        }
        return rects
    }

    private fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        subFolder: String,
        dateTakenMs: Long
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$subFolder")
                put(MediaStore.Images.Media.DATE_TAKEN, dateTakenMs)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return null
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                uri
            }.getOrNull()
        } else {
            @Suppress("DEPRECATION")
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val targetDir = File(picturesDir, subFolder).apply { if (!exists()) mkdirs() }
            val file = File(targetDir, displayName)
            runCatching {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                    put(MediaStore.Images.Media.DATE_TAKEN, dateTakenMs)
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }.getOrNull()
        }
    }
}
