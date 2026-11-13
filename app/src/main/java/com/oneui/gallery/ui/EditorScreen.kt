package com.oneui.gallery.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.oneui.gallery.data.MediaRepository
import com.oneui.gallery.model.BuiltInFilters
import com.oneui.gallery.model.CropAspect
import com.oneui.gallery.model.DrawStroke
import com.oneui.gallery.model.MediaItem
import com.oneui.gallery.model.OverlayTextItem
import com.oneui.gallery.ui.theme.LocalOneUIColors
import com.oneui.gallery.ui.theme.OneUIShapeMedium
import com.oneui.gallery.ui.theme.OneUIShapeSmall
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class EditorTab(val label: String, val icon: ImageVector) {
    TRANSFORM("Transformar", Icons.Outlined.Crop),
    FILTERS("Filtros", Icons.Outlined.AutoAwesome),
    TONE("Ajustes", Icons.Outlined.Tune),
    DRAW("Desenhar", Icons.Outlined.Brush)
}

@Composable
fun EditorScreen(
    item: MediaItem,
    onBack: () -> Unit,
    onSaved: (Uri) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOneUIColors.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(EditorTab.FILTERS) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var flipH by remember { mutableStateOf(false) }
    var flipV by remember { mutableStateOf(false) }
    var cropAspect by remember { mutableStateOf(CropAspect.ORIGINAL) }

    var selectedFilter by remember { mutableStateOf(BuiltInFilters.presets.first()) }
    var brightness by remember { mutableFloatStateOf(0f) } // -0.6..0.6
    var contrast by remember { mutableFloatStateOf(1f) }   // 0.5..1.8
    var saturation by remember { mutableFloatStateOf(1f) } // 0..2
    var warmth by remember { mutableFloatStateOf(0f) }     // -1..1
    var vignette by remember { mutableFloatStateOf(0f) }   // 0..1

    val strokes = remember { mutableStateListOf<DrawStroke>() }
    var currentStrokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var brushColor by remember { mutableStateOf(Color(0xFFF53855)) }
    var brushWidth by remember { mutableFloatStateOf(14f) }

    val overlayTexts = remember { mutableStateListOf<OverlayTextItem>() }
    var showTextDialog by remember { mutableStateOf(false) }
    var draftText by remember { mutableStateOf("") }

    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    var isSaving by remember { mutableStateOf(false) }

    val liveColorMatrix = remember(selectedFilter, brightness, contrast, saturation, warmth) {
        val androidMatrix = MediaRepository.buildCombinedColorMatrix(
            filterMatrix = selectedFilter.matrix,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            warmth = warmth
        )
        ColorMatrix(androidMatrix.array)
    }

    val brushSwatches = remember {
        listOf(
            Color(0xFFFFFFFF),
            Color(0xFF000000),
            Color(0xFFF53855),
            Color(0xFF387AFF),
            Color(0xFF34C759),
            Color(0xFFFFCC00),
            Color(0xFFAF52DE)
        )
    }

    fun resetAll() {
        rotationDegrees = 0f
        flipH = false
        flipV = false
        cropAspect = CropAspect.ORIGINAL
        selectedFilter = BuiltInFilters.presets.first()
        brightness = 0f
        contrast = 1f
        saturation = 1f
        warmth = 0f
        vignette = 0f
        strokes.clear()
        overlayTexts.clear()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Editor Top Bar (One UI style)
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
                text = "Editor",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { resetAll() }, enabled = !isSaving) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Restaurar", tint = Color(0xCCFFFFFF))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Button(
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    scope.launch {
                        val savedUri = MediaRepository.saveEditedImage(
                            context = context,
                            sourceUri = item.uri,
                            rotationDegrees = rotationDegrees,
                            flipH = flipH,
                            flipV = flipV,
                            cropAspect = cropAspect,
                            filterMatrix = selectedFilter.matrix,
                            brightness = brightness,
                            contrast = contrast,
                            saturation = saturation,
                            warmth = warmth,
                            vignette = vignette,
                            strokes = strokes.toList(),
                            overlayTexts = overlayTexts.toList(),
                            previewWidth = previewSize.width.toFloat(),
                            previewHeight = previewSize.height.toFloat()
                        )
                        isSaving = false
                        if (savedUri != null) {
                            Toast.makeText(context, "Cópia salva em Galeria Editadas!", Toast.LENGTH_SHORT).show()
                            onSaved(savedUri)
                        } else {
                            Toast.makeText(context, "Não foi possível salvar a imagem.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlue),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text("Salvar cópia", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Interactive Preview Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val aspectRatioModifier = if (cropAspect.ratio != null) {
                Modifier.aspectRatio(cropAspect.ratio!!)
            } else {
                Modifier.fillMaxSize()
            }

            Box(
                modifier = aspectRatioModifier
                    .clip(RoundedCornerShape(10.dp))
                    .clipToBounds()
                    .onSizeChanged { previewSize = it }
                    .then(
                        if (activeTab == EditorTab.DRAW) {
                            Modifier.pointerInput(brushColor, brushWidth) {
                                detectDragGestures(
                                    onDragStart = { startOffset ->
                                        currentStrokePoints = listOf(startOffset)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentStrokePoints = currentStrokePoints + change.position
                                    },
                                    onDragEnd = {
                                        if (currentStrokePoints.size >= 2) {
                                            strokes.add(
                                                DrawStroke(
                                                    points = currentStrokePoints,
                                                    color = brushColor,
                                                    strokeWidth = brushWidth
                                                )
                                            )
                                        }
                                        currentStrokePoints = emptyList()
                                    },
                                    onDragCancel = {
                                        currentStrokePoints = emptyList()
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.uri)
                        .crossfade(false)
                        .build(),
                    contentDescription = "Preview",
                    contentScale = if (cropAspect.ratio != null) ContentScale.Crop else ContentScale.Fit,
                    colorFilter = ColorFilter.colorMatrix(liveColorMatrix),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = rotationDegrees
                            scaleX = if (flipH) -1f else 1f
                            scaleY = if (flipV) -1f else 1f
                        }
                )

                // Vignette preview overlay
                if (vignette > 0.01f) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = (vignette * 0.78f).coerceIn(0f, 0.85f))
                                ),
                                center = center,
                                radius = size.maxDimension * 0.68f
                            )
                        )
                    }
                }

                // Freehand drawing strokes overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    fun drawSingleStroke(points: List<Offset>, color: Color, width: Float) {
                        if (points.size < 2) return
                        val path = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                    strokes.forEach { s -> drawSingleStroke(s.points, s.color, s.strokeWidth) }
                    if (currentStrokePoints.size >= 2) {
                        drawSingleStroke(currentStrokePoints, brushColor, brushWidth)
                    }
                }

                // Overlay Texts
                overlayTexts.forEach { txt ->
                    Text(
                        text = txt.text,
                        color = txt.color,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Editor Controls Panel (One UI Rounded Sheet)
        Surface(
            color = Color(0xFF151518),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) {
                when (activeTab) {
                    EditorTab.TRANSFORM -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TransformButton(
                                    icon = Icons.AutoMirrored.Rounded.RotateLeft,
                                    label = "Girar -90°",
                                    onClick = { rotationDegrees = (rotationDegrees - 90f) % 360f }
                                )
                                TransformButton(
                                    icon = Icons.AutoMirrored.Rounded.RotateRight,
                                    label = "Girar +90°",
                                    onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f }
                                )
                                TransformButton(
                                    icon = Icons.Outlined.Flip,
                                    label = "Espelhar H",
                                    active = flipH,
                                    onClick = { flipH = !flipH }
                                )
                                TransformButton(
                                    icon = Icons.Outlined.Flip,
                                    label = "Espelhar V",
                                    active = flipV,
                                    onClick = { flipV = !flipV }
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                            ) {
                                CropAspect.entries.forEach { aspect ->
                                    val selected = cropAspect == aspect
                                    FilterChip(
                                        selected = selected,
                                        onClick = { cropAspect = aspect },
                                        label = { Text(aspect.label, fontSize = 12.sp) },
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
                        }
                    }

                    EditorTab.FILTERS -> {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(BuiltInFilters.presets) { preset ->
                                val isSelected = preset.id == selectedFilter.id
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(68.dp)
                                        .clickable { selectedFilter = preset }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(item.uri)
                                            .size(160)
                                            .build(),
                                        contentDescription = preset.name,
                                        contentScale = ContentScale.Crop,
                                        colorFilter = ColorFilter.colorMatrix(ColorMatrix(preset.matrix)),
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(OneUIShapeSmall)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 0.dp,
                                                color = if (isSelected) colors.primaryBlue else Color.Transparent,
                                                shape = OneUIShapeSmall
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = preset.name,
                                        color = if (isSelected) colors.primaryBlue else Color(0xCCFFFFFF),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    EditorTab.TONE -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(152.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                        ) {
                            ToneSliderRow(
                                label = "Brilho",
                                value = brightness,
                                valueRange = -0.5f..0.5f,
                                display = "${(brightness * 100).roundToInt()}",
                                accent = colors.primaryBlue,
                                onValueChange = { brightness = it }
                            )
                            ToneSliderRow(
                                label = "Contraste",
                                value = contrast,
                                valueRange = 0.5f..1.7f,
                                display = "${((contrast - 1f) * 100).roundToInt()}",
                                accent = colors.primaryBlue,
                                onValueChange = { contrast = it }
                            )
                            ToneSliderRow(
                                label = "Saturação",
                                value = saturation,
                                valueRange = 0f..2f,
                                display = "${((saturation - 1f) * 100).roundToInt()}",
                                accent = colors.primaryBlue,
                                onValueChange = { saturation = it }
                            )
                            ToneSliderRow(
                                label = "Temperatura",
                                value = warmth,
                                valueRange = -1f..1f,
                                display = "${(warmth * 100).roundToInt()}",
                                accent = colors.primaryBlue,
                                onValueChange = { warmth = it }
                            )
                            ToneSliderRow(
                                label = "Vinheta",
                                value = vignette,
                                valueRange = 0f..1f,
                                display = "${(vignette * 100).roundToInt()}",
                                accent = colors.primaryBlue,
                                onValueChange = { vignette = it }
                            )
                        }
                    }

                    EditorTab.DRAW -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    brushSwatches.forEach { swatch ->
                                        val selected = swatch == brushColor
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(swatch)
                                                .border(
                                                    width = if (selected) 2.5.dp else 1.dp,
                                                    color = if (selected) colors.primaryBlue else Color(0x55FFFFFF),
                                                    shape = CircleShape
                                                )
                                                .clickable { brushColor = swatch }
                                        )
                                    }
                                }
                                Row {
                                    IconButton(
                                        onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) },
                                        enabled = strokes.isNotEmpty()
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.Undo,
                                            contentDescription = "Desfazer traço",
                                            tint = if (strokes.isNotEmpty()) Color.White else Color(0x44FFFFFF)
                                        )
                                    }
                                    IconButton(onClick = { showTextDialog = true }) {
                                        Icon(
                                            Icons.Outlined.TextFields,
                                            contentDescription = "Adicionar Texto",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Espessura", color = Color(0xCCFFFFFF), fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Slider(
                                    value = brushWidth,
                                    onValueChange = { brushWidth = it },
                                    valueRange = 4f..36f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = colors.primaryBlue,
                                        activeTrackColor = colors.primaryBlue
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Bottom Editor Mode Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 12.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EditorTab.entries.forEach { tab ->
                        val selected = tab == activeTab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { activeTab = tab }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (selected) colors.primaryBlue else Color(0x99FFFFFF),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = tab.label,
                                color = if (selected) colors.primaryBlue else Color(0x99FFFFFF),
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            containerColor = colors.surfaceCard,
            shape = OneUIShapeMedium,
            title = { Text("Adicionar texto", color = colors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    placeholder = { Text("Digite seu texto...") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (draftText.isNotBlank()) {
                            overlayTexts.add(OverlayTextItem(text = draftText.trim(), color = brushColor))
                            draftText = ""
                        }
                        showTextDialog = false
                    }
                ) {
                    Text("Adicionar", color = colors.primaryBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun TransformButton(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalOneUIColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) colors.primaryBlue.copy(alpha = 0.22f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) colors.primaryBlue else Color.White,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (active) colors.primaryBlue else Color(0xCCFFFFFF),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ToneSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    display: String,
    accent: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            color = Color(0xDDFFFFFF),
            fontSize = 12.sp,
            modifier = Modifier.width(86.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color(0xFF34343B)
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = display,
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(34.dp)
        )
    }
}
