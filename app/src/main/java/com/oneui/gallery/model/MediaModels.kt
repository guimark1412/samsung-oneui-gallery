package com.oneui.gallery.model

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateTaken: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val bucketId: String,
    val bucketName: String,
    val duration: Long = 0L,
    val isVideo: Boolean = false,
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
    val isPrivate: Boolean = false,
    val dateGroupLabel: String = ""
)

data class AlbumItem(
    val id: String,
    val name: String,
    val coverUri: Uri,
    val items: List<MediaItem>,
    val videoCount: Int
)

data class StoryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverUri: Uri,
    val items: List<MediaItem>
)

data class FilterPreset(
    val id: String,
    val name: String,
    val matrix: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FilterPreset) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

data class OverlayTextItem(
    val text: String,
    val color: Color,
    val positionFraction: Offset = Offset(0.5f, 0.5f),
    val scale: Float = 1f
)

enum class CropAspect(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    SQUARE("1:1", 1f),
    FOUR_FIVE("4:5", 4f / 5f),
    FIVE_FOUR("5:4", 5f / 4f),
    FOUR_THREE("4:3", 4f / 3f),
    SIXTEEN_NINE("16:9", 16f / 9f),
    NINE_SIXTEEN("9:16", 9f / 16f)
}

enum class CollageLayout(val label: String) {
    GRID("Grade"),
    COLUMNS("Colunas"),
    ROWS("Linhas"),
    HERO("Destaque")
}

object BuiltInFilters {
    val presets: List<FilterPreset> = listOf(
        FilterPreset(
            id = "original",
            name = "Original",
            matrix = floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        ),
        FilterPreset(
            id = "vivid",
            name = "Vivo",
            matrix = floatArrayOf(
                1.22f, -0.05f, -0.05f, 0f, 8f,
                -0.05f, 1.22f, -0.05f, 0f, 8f,
                -0.05f, -0.05f, 1.22f, 0f, 8f,
                0f, 0f, 0f, 1f, 0f
            )
        ),
        FilterPreset(
            id = "warm",
            name = "Quente",
            matrix = floatArrayOf(
                1.14f, 0f, 0f, 0f, 14f,
                0f, 1.04f, 0f, 0f, 4f,
                0f, 0f, 0.88f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
        ),
        FilterPreset(
            id = "cool",
            name = "Frio",
            matrix = floatArrayOf(
                0.88f, 0f, 0f, 0f, -8f,
                0f, 1.02f, 0f, 0f, 4f,
                0f, 0f, 1.18f, 0f, 16f,
                0f, 0f, 0f, 1f, 0f
            )
        ),
        FilterPreset(
            id = "bw",
            name = "P&B",
            matrix = floatArrayOf(
                0.33f, 0.59f, 0.08f, 0f, 0f,
                0.33f, 0.59f, 0.08f, 0f, 0f,
                0.33f, 0.59f, 0.08f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        ),
        FilterPreset(
            id = "noir",
            name = "Noir",
            matrix = floatArrayOf(
                0.42f, 0.72f, 0.10f, 0f, -24f,
                0.42f, 0.72f, 0.10f, 0f, -24f,
                0.42f, 0.72f, 0.10f, 0f, -24f,
                0f, 0f, 0f, 1f, 0f
            )
        ),
        FilterPreset(
            id = "sepia",
            name = "Sépia",
            matrix = floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        ),
        FilterPreset(
            id = "retro",
            name = "Retrô",
            matrix = floatArrayOf(
                1.08f, 0.08f, 0.0f, 0f, 12f,
                0.04f, 0.96f, 0.04f, 0f, 8f,
                0.0f, 0.08f, 0.82f, 0f, 18f,
                0f, 0f, 0f, 1f, 0f
            )
        ),
        FilterPreset(
            id = "cyber",
            name = "Cyberpunk",
            matrix = floatArrayOf(
                1.25f, -0.10f, 0.15f, 0f, -6f,
                -0.08f, 1.05f, 0.12f, 0f, 6f,
                0.20f, 0.05f, 1.30f, 0f, 14f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
}
