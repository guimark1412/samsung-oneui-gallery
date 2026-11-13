package com.oneui.gallery.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.oneui.gallery.data.MediaRepository
import com.oneui.gallery.model.CollageLayout
import com.oneui.gallery.model.MediaItem
import com.oneui.gallery.ui.theme.LocalOneUIColors
import kotlinx.coroutines.launch
import kotlin.math.ceil

@Composable
fun CollageScreen(
    items: List<MediaItem>,
    onBack: () -> Unit,
    onSaved: (Uri) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOneUIColors.current
    val scope = rememberCoroutineScope()

    val selectedPhotos = remember(items) { items.filter { !it.isVideo }.take(6) }
    var layout by remember { mutableStateOf(CollageLayout.GRID) }
    var aspectLabel by remember { mutableStateOf("4:5") }
    val aspectRatio = when (aspectLabel) {
        "1:1" -> 1f
        "9:16" -> 9f / 16f
        else -> 4f / 5f
    }
    var spacingDp by remember { mutableFloatStateOf(6f) }
    var cornerDp by remember { mutableFloatStateOf(14f) }
    val bgSwatches = remember {
        listOf(
            Color(0xFF000000),
            Color(0xFFFFFFFF),
            Color(0xFF17171A),
            Color(0xFF387AFF),
            Color(0xFFF53855)
        )
    }
    var bgColor by remember { mutableStateOf(bgSwatches.first()) }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = !isSaving) {
                Icon(Icons.Rounded.Close, contentDescription = "Cancelar", tint = Color.White)
            }
            Text(
                text = "Estúdio de Colagem (${selectedPhotos.size})",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (isSaving || selectedPhotos.isEmpty()) return@Button
                    isSaving = true
                    scope.launch {
                        val saved = MediaRepository.saveCollage(
                            context = context,
                            items = selectedPhotos,
                            layout = layout,
                            aspectRatio = aspectRatio,
                            spacingPx = spacingDp,
                            cornerRadiusPx = cornerDp,
                            bgColorArgb = bgColor.toArgb()
                        )
                        isSaving = false
                        if (saved != null) {
                            Toast.makeText(context, "Colagem salva em Colagens One UI!", Toast.LENGTH_SHORT).show()
                            onSaved(saved)
                        } else {
                            Toast.makeText(context, "Erro ao salvar colagem.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlue),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                enabled = !isSaving && selectedPhotos.isNotEmpty()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Text("Salvar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Live Collage Canvas Preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .padding(spacingDp.dp)
            ) {
                CollagePreviewGrid(
                    items = selectedPhotos,
                    layout = layout,
                    spacing = spacingDp.dp,
                    corner = cornerDp.dp
                )
            }
        }

        // One UI 8.5 Bottom Collage Controls
        Surface(
            color = Color(0xFF151518),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Layout Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CollageLayout.entries.forEach { preset ->
                        val selected = preset == layout
                        FilterChip(
                            selected = selected,
                            onClick = { layout = preset },
                            label = { Text(preset.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primaryBlue,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF242429),
                                labelColor = Color(0xCCFFFFFF)
                            ),
                            border = null
                        )
                    }
                }

                // Aspect Ratio + Background Swatches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("1:1", "4:5", "9:16").forEach { ratio ->
                            val selected = ratio == aspectLabel
                            Surface(
                                onClick = { aspectLabel = ratio },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) colors.primaryBlue else Color(0xFF242429)
                            ) {
                                Text(
                                    text = ratio,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bgSwatches.forEach { swatch ->
                            val selected = swatch == bgColor
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) colors.primaryBlue else Color(0x55FFFFFF),
                                        shape = CircleShape
                                    )
                                    .clickable { bgColor = swatch }
                            )
                        }
                    }
                }

                // Spacing & Corner Sliders
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Borda", color = Color(0xCCFFFFFF), fontSize = 12.sp, modifier = Modifier.width(56.dp))
                    Slider(
                        value = spacingDp,
                        onValueChange = { spacingDp = it },
                        valueRange = 0f..18f,
                        colors = SliderDefaults.colors(thumbColor = colors.primaryBlue, activeTrackColor = colors.primaryBlue),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Cantos", color = Color(0xCCFFFFFF), fontSize = 12.sp, modifier = Modifier.width(56.dp))
                    Slider(
                        value = cornerDp,
                        onValueChange = { cornerDp = it },
                        valueRange = 0f..26f,
                        colors = SliderDefaults.colors(thumbColor = colors.primaryBlue, activeTrackColor = colors.primaryBlue),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CollagePreviewGrid(
    items: List<MediaItem>,
    layout: CollageLayout,
    spacing: Dp,
    corner: Dp
) {
    val context = LocalContext.current
    if (items.isEmpty()) return

    @Composable
    fun Cell(item: MediaItem, modifier: Modifier) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(item.uri).size(600).build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(corner))
        )
    }

    when (layout) {
        CollageLayout.COLUMNS -> {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                items.forEach { Cell(it, Modifier.weight(1f).fillMaxHeight()) }
            }
        }
        CollageLayout.ROWS -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                items.forEach { Cell(it, Modifier.weight(1f).fillMaxWidth()) }
            }
        }
        CollageLayout.HERO -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Cell(items.first(), Modifier.weight(0.58f).fillMaxWidth())
                if (items.size > 1) {
                    Row(
                        modifier = Modifier.weight(0.42f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        items.drop(1).forEach { Cell(it, Modifier.weight(1f).fillMaxHeight()) }
                    }
                }
            }
        }
        CollageLayout.GRID -> {
            val cols = if (items.size <= 2) items.size else if (items.size <= 4) 2 else 3
            val rows = ceil(items.size / cols.toFloat()).toInt().coerceAtLeast(1)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        for (c in 0 until cols) {
                            val idx = r * cols + c
                            val item = items.getOrNull(idx)
                            if (item != null) {
                                Cell(item, Modifier.weight(1f).fillMaxHeight())
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
