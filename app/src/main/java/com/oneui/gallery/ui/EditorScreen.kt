package com.oneui.gallery.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Brush
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

private enum class EditorTab(val label: String, val icon: ImageVector) {
    TRANSFORM("Recortar", Icons.Outlined.Crop),
    FILTERS("Filtros", Icons.Outlined.AutoAwesome),
    TONE("Ajustes", Icons.Outlined.Tune),
    DRAW("Desenhar", Icons.Outlined.Brush)
}

private enum class CropHandle {
    NONE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    MOVE
}

private data class CropRectNorm(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
)

private fun fitCropToAspect(
    current: CropRectNorm,
    targetAspectRatio: Float?,
    imageRatio: Float
): CropRectNorm {
    if (targetAspectRatio == null || imageRatio <= 0f) return current
    val k = targetAspectRatio / imageRatio // normW / normH = k
    val curW = (current.right - current.left).coerceAtLeast(0.12f)
    val curH = (current.bottom - current.top).coerceAtLeast(0.12f)
    val cx = (current.left + current.right) * 0.5f
    val cy = (current.top + current.bottom) * 0.5f

    var newW = curW
    var newH = newW / k
    if (newH > 1f) {
        newH = 1f
        newW = newH * k
    }
    if (newW > 1f) {
        newW = 1f
        newH = newW / k
    }
    if (curW * curH > 0.85f) {
        if (k >= 1f) {
            newW = 1f
            newH = (1f / k).coerceAtMost(1f)
        } else {
            newH = 1f
            newW = k.coerceAtMost(1f)
        }
    }
    val halfW = newW * 0.5f
    val halfH = newH * 0.5f
    val clx = cx.coerceIn(halfW, 1f - halfW)
    val cly = cy.coerceIn(halfH, 1f - halfH)
    return CropRectNorm(
        left = (clx - halfW).coerceIn(0f, 1f),
        top = (cly - halfH).coerceIn(0f, 1f),
        right = (clx + halfW).coerceIn(0f, 1f),
        bottom = (cly + halfH).coerceIn(0f, 1f)
    )
}

private fun applyCropDrag(
    cur: CropRectNorm,
    handle: CropHandle,
    dx: Float,
    dy: Float,
    targetAspectRatio: Float?,
    imageRatio: Float
): CropRectNorm {
    val minNorm = 0.14f
    if (handle == CropHandle.MOVE) {
        val w = cur.right - cur.left
        val h = cur.bottom - cur.top
        val nl = (cur.left + dx).coerceIn(0f, 1f - w)
        val nt = (cur.top + dy).coerceIn(0f, 1f - h)
        return CropRectNorm(nl, nt, nl + w, nt + h)
    }

    if (targetAspectRatio == null || imageRatio <= 0f) {
        var l = cur.left
        var t = cur.top
        var r = cur.right
        var b = cur.bottom
        when (handle) {
            CropHandle.TOP_LEFT -> {
                l = (l + dx).coerceIn(0f, r - minNorm)
                t = (t + dy).coerceIn(0f, b - minNorm)
            }
            CropHandle.TOP_RIGHT -> {
                r = (r + dx).coerceIn(l + minNorm, 1f)
                t = (t + dy).coerceIn(0f, b - minNorm)
            }
            CropHandle.BOTTOM_LEFT -> {
                l = (l + dx).coerceIn(0f, r - minNorm)
                b = (b + dy).coerceIn(t + minNorm, 1f)
            }
            CropHandle.BOTTOM_RIGHT -> {
                r = (r + dx).coerceIn(l + minNorm, 1f)
                b = (b + dy).coerceIn(t + minNorm, 1f)
            }
            CropHandle.LEFT -> l = (l + dx).coerceIn(0f, r - minNorm)
            CropHandle.RIGHT -> r = (r + dx).coerceIn(l + minNorm, 1f)
            CropHandle.TOP -> t = (t + dy).coerceIn(0f, b - minNorm)
            CropHandle.BOTTOM -> b = (b + dy).coerceIn(t + minNorm, 1f)
            else -> Unit
        }
        return CropRectNorm(l, t, r, b)
    }

    // Aspect-locked resize
    val k = targetAspectRatio / imageRatio // normW = normH * k
    val minW = if (k >= 1f) minNorm * k else minNorm
    val minH = minW / k

    return when (handle) {
        CropHandle.BOTTOM_RIGHT, CropHandle.RIGHT, CropHandle.BOTTOM -> {
            val anchorL = cur.left
            val anchorT = cur.top
            val maxW = min(1f - anchorL, (1f - anchorT) * k).coerceAtLeast(minW)
            val candW = when (handle) {
                CropHandle.BOTTOM -> (cur.bottom + dy - anchorT) * k
                CropHandle.RIGHT -> cur.right + dx - anchorL
                else -> ((cur.right + dx - anchorL) + (cur.bottom + dy - anchorT) * k) * 0.5f
            }
            val w = candW.coerceIn(minW.coerceAtMost(maxW), maxW)
            val h = w / k
            CropRectNorm(anchorL, anchorT, anchorL + w, anchorT + h)
        }
        CropHandle.TOP_LEFT, CropHandle.LEFT, CropHandle.TOP -> {
            val anchorR = cur.right
            val anchorB = cur.bottom
            val maxW = min(anchorR, anchorB * k).coerceAtLeast(minW)
            val candW = when (handle) {
                CropHandle.TOP -> (anchorB - (cur.top + dy)) * k
                CropHandle.LEFT -> anchorR - (cur.left + dx)
                else -> ((anchorR - (cur.left + dx)) + (anchorB - (cur.top + dy)) * k) * 0.5f
            }
            val w = candW.coerceIn(minW.coerceAtMost(maxW), maxW)
            val h = w / k
            CropRectNorm(anchorR - w, anchorB - h, anchorR, anchorB)
        }
        CropHandle.TOP_RIGHT -> {
            val anchorL = cur.left
            val anchorB = cur.bottom
            val maxW = min(1f - anchorL, anchorB * k).coerceAtLeast(minW)
            val candW = ((cur.right + dx - anchorL) + (anchorB - (cur.top + dy)) * k) * 0.5f
            val w = candW.coerceIn(minW.coerceAtMost(maxW), maxW)
            val h = w / k
            CropRectNorm(anchorL, anchorB - h, anchorL + w, anchorB)
        }
        CropHandle.BOTTOM_LEFT -> {
            val anchorR = cur.right
            val anchorT = cur.top
            val maxW = min(anchorR, (1f - anchorT) * k).coerceAtLeast(minW)
            val candW = ((anchorR - (cur.left + dx)) + (cur.bottom + dy - anchorT) * k) * 0.5f
            val w = candW.coerceIn(minW.coerceAtMost(maxW), maxW)
            val h = w / k
            CropRectNorm(anchorR - w, anchorT, anchorR, anchorT + h)
        }
        else -> cur
    }
}

@Composable
fun EditorScreen(
    item: MediaItem,
    onBack: () -> Unit,
    onSaved: (Uri) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val colors = LocalOneUIColors.current
    val scope = rememberCoroutineScope()

    var sourceBitmap by remember(item.uri) { mutableStateOf<Bitmap?>(null) }
    var transformedBitmap by remember(item.uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.uri) {
        val loaded = withContext(Dispatchers.IO) {
            MediaRepository.decodeBitmapForEditing(context, item.uri, 1440)
        }
        sourceBitmap = loaded
    }

    var activeTab by remember { mutableStateOf(EditorTab.TRANSFORM) }
    var baseRotationDegrees by remember { mutableFloatStateOf(0f) }
    var fineRotationDegrees by remember { mutableFloatStateOf(0f) }
    var flipH by remember { mutableStateOf(false) }
    var flipV by remember { mutableStateOf(false) }
    var cropAspect by remember { mutableStateOf(CropAspect.FREE) }
    var cropNorm by remember { mutableStateOf(CropRectNorm()) }

    LaunchedEffect(sourceBitmap, baseRotationDegrees, flipH, flipV) {
        val src = sourceBitmap ?: return@LaunchedEffect
        if (baseRotationDegrees == 0f && !flipH && !flipV) {
            transformedBitmap = src
        } else {
            transformedBitmap = withContext(Dispatchers.Default) {
                val m = Matrix().apply {
                    postRotate(baseRotationDegrees)
                    postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f)
                }
                Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
            }
        }
    }

    DisposableEffect(item.uri) {
        onDispose {
            val tb = transformedBitmap
            val sb = sourceBitmap
            if (tb != null && tb !== sb && !tb.isRecycled) tb.recycle()
            if (sb != null && !sb.isRecycled) sb.recycle()
        }
    }

    var selectedFilter by remember { mutableStateOf(BuiltInFilters.presets.first()) }
    var brightness by remember { mutableFloatStateOf(0f) } // -0.5..0.5
    var contrast by remember { mutableFloatStateOf(1f) }   // 0.5..1.7
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

    fun resolveTargetRatio(aspect: CropAspect, imageRatio: Float): Float? = when (aspect) {
        CropAspect.FREE -> null
        CropAspect.ORIGINAL -> imageRatio
        else -> aspect.ratio
    }

    fun resetTransform() {
        baseRotationDegrees = 0f
        fineRotationDegrees = 0f
        flipH = false
        flipV = false
        cropAspect = CropAspect.FREE
        cropNorm = CropRectNorm()
    }

    fun resetAll() {
        resetTransform()
        selectedFilter = BuiltInFilters.presets.first()
        brightness = 0f
        contrast = 1f
        saturation = 1f
        warmth = 0f
        vignette = 0f
        strokes.clear()
        overlayTexts.clear()
    }

    val totalRotation = baseRotationDegrees + fineRotationDegrees

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
                            rotationDegrees = totalRotation,
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
                            previewHeight = previewSize.height.toFloat(),
                            cropRectNorm = Rect(
                                cropNorm.left,
                                cropNorm.top,
                                cropNorm.right,
                                cropNorm.bottom
                            )
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
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val bmp = transformedBitmap
            if (bmp == null) {
                CircularProgressIndicator(color = colors.primaryBlue)
            } else {
                val maxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val maxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                val imgRatio = bmp.width.toFloat() / bmp.height.toFloat()

                if (activeTab == EditorTab.TRANSFORM) {
                    // Fitted full image inside preview box + interactive One UI Crop Overlay
                    val boxRatio = maxW / maxH
                    val fitW: Float
                    val fitH: Float
                    if (imgRatio > boxRatio) {
                        fitW = maxW
                        fitH = (maxW / imgRatio).coerceAtMost(maxH)
                    } else {
                        fitH = maxH
                        fitW = (maxH * imgRatio).coerceAtMost(maxW)
                    }
                    val fitLeft = (maxW - fitW) * 0.5f
                    val fitTop = (maxH - fitH) * 0.5f
                    val fitWdp = with(density) { fitW.toDp() }
                    val fitHdp = with(density) { fitH.toDp() }
                    val handleRadiusPx = with(density) { 28.dp.toPx() }
                    val fineScale = 1f + (abs(fineRotationDegrees) / 45f) * 0.28f

                    Box(
                        modifier = Modifier
                            .size(width = fitWdp, height = fitHdp)
                            .clip(RoundedCornerShape(6.dp))
                            .clipToBounds()
                            .pointerInput(cropAspect, imgRatio, fitW, fitH) {
                                var activeHandle = CropHandle.NONE
                                detectDragGestures(
                                    onDragStart = { start ->
                                        val bx1 = cropNorm.left * fitW
                                        val by1 = cropNorm.top * fitH
                                        val bx2 = cropNorm.right * fitW
                                        val by2 = cropNorm.bottom * fitH

                                        val dTL = hypot(start.x - bx1, start.y - by1)
                                        val dTR = hypot(start.x - bx2, start.y - by1)
                                        val dBL = hypot(start.x - bx1, start.y - by2)
                                        val dBR = hypot(start.x - bx2, start.y - by2)
                                        val minCorner = minOf(dTL, dTR, dBL, dBR)

                                        activeHandle = when {
                                            minCorner <= handleRadiusPx -> when (minCorner) {
                                                dTL -> CropHandle.TOP_LEFT
                                                dTR -> CropHandle.TOP_RIGHT
                                                dBL -> CropHandle.BOTTOM_LEFT
                                                else -> CropHandle.BOTTOM_RIGHT
                                            }
                                            abs(start.x - bx1) <= handleRadiusPx && start.y in (by1 - 12f)..(by2 + 12f) -> CropHandle.LEFT
                                            abs(start.x - bx2) <= handleRadiusPx && start.y in (by1 - 12f)..(by2 + 12f) -> CropHandle.RIGHT
                                            abs(start.y - by1) <= handleRadiusPx && start.x in (bx1 - 12f)..(bx2 + 12f) -> CropHandle.TOP
                                            abs(start.y - by2) <= handleRadiusPx && start.x in (bx1 - 12f)..(bx2 + 12f) -> CropHandle.BOTTOM
                                            start.x in bx1..bx2 && start.y in by1..by2 -> CropHandle.MOVE
                                            else -> CropHandle.NONE
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (activeHandle != CropHandle.NONE) {
                                            change.consume()
                                            cropNorm = applyCropDrag(
                                                cur = cropNorm,
                                                handle = activeHandle,
                                                dx = dragAmount.x / fitW,
                                                dy = dragAmount.y / fitH,
                                                targetAspectRatio = resolveTargetRatio(cropAspect, imgRatio),
                                                imageRatio = imgRatio
                                            )
                                        }
                                    },
                                    onDragEnd = { activeHandle = CropHandle.NONE },
                                    onDragCancel = { activeHandle = CropHandle.NONE }
                                )
                            }
                    ) {
                        val imageBmp = remember(bmp) { bmp.asImageBitmap() }
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationZ = fineRotationDegrees
                                    scaleX = fineScale
                                    scaleY = fineScale
                                }
                        ) {
                            drawImage(
                                image = imageBmp,
                                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                colorFilter = ColorFilter.colorMatrix(liveColorMatrix)
                            )
                        }

                        // One UI Crop Frame Overlay (dark exterior scrim, 3x3 grid, L corner brackets)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val l = cropNorm.left * size.width
                            val t = cropNorm.top * size.height
                            val r = cropNorm.right * size.width
                            val b = cropNorm.bottom * size.height
                            val cw = (r - l).coerceAtLeast(1f)
                            val ch = (b - t).coerceAtLeast(1f)

                            val scrimPath = Path().apply {
                                fillType = PathFillType.EvenOdd
                                addRect(Rect(0f, 0f, size.width, size.height))
                                addRect(Rect(l, t, r, b))
                            }
                            drawPath(scrimPath, Color(0xA8000000))

                            // Rule of thirds lines
                            val gridColor = Color(0x66FFFFFF)
                            val gridStroke = 1.dp.toPx()
                            drawLine(gridColor, Offset(l + cw / 3f, t), Offset(l + cw / 3f, b), gridStroke)
                            drawLine(gridColor, Offset(l + cw * 2f / 3f, t), Offset(l + cw * 2f / 3f, b), gridStroke)
                            drawLine(gridColor, Offset(l, t + ch / 3f), Offset(r, t + ch / 3f), gridStroke)
                            drawLine(gridColor, Offset(l, t + ch * 2f / 3f), Offset(r, t + ch * 2f / 3f), gridStroke)

                            // Crisp border
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(l, t),
                                size = androidx.compose.ui.geometry.Size(cw, ch),
                                style = Stroke(width = 1.4.dp.toPx())
                            )

                            // L-shaped corner brackets + edge handles
                            val bracketLen = min(22.dp.toPx(), min(cw, ch) * 0.35f)
                            val bracketStroke = 3.5.dp.toPx()
                            val cap = StrokeCap.Round

                            // Top-Left
                            drawLine(Color.White, Offset(l, t), Offset(l + bracketLen, t), bracketStroke, cap)
                            drawLine(Color.White, Offset(l, t), Offset(l, t + bracketLen), bracketStroke, cap)
                            // Top-Right
                            drawLine(Color.White, Offset(r - bracketLen, t), Offset(r, t), bracketStroke, cap)
                            drawLine(Color.White, Offset(r, t), Offset(r, t + bracketLen), bracketStroke, cap)
                            // Bottom-Left
                            drawLine(Color.White, Offset(l, b), Offset(l + bracketLen, b), bracketStroke, cap)
                            drawLine(Color.White, Offset(l, b - bracketLen), Offset(l, b), bracketStroke, cap)
                            // Bottom-Right
                            drawLine(Color.White, Offset(r - bracketLen, b), Offset(r, b), bracketStroke, cap)
                            drawLine(Color.White, Offset(r, b - bracketLen), Offset(r, b), bracketStroke, cap)

                            // Edge center handles
                            val edgeLen = min(18.dp.toPx(), min(cw, ch) * 0.25f)
                            val cx = (l + r) * 0.5f
                            val cy = (t + b) * 0.5f
                            drawLine(Color.White, Offset(cx - edgeLen * 0.5f, t), Offset(cx + edgeLen * 0.5f, t), bracketStroke, cap)
                            drawLine(Color.White, Offset(cx - edgeLen * 0.5f, b), Offset(cx + edgeLen * 0.5f, b), bracketStroke, cap)
                            drawLine(Color.White, Offset(l, cy - edgeLen * 0.5f), Offset(l, cy + edgeLen * 0.5f), bracketStroke, cap)
                            drawLine(Color.White, Offset(r, cy - edgeLen * 0.5f), Offset(r, cy + edgeLen * 0.5f), bracketStroke, cap)
                        }

                        // Pixel size pill indicator
                        val outW = ((cropNorm.right - cropNorm.left) * bmp.width).roundToInt().coerceAtLeast(1)
                        val outH = ((cropNorm.bottom - cropNorm.top) * bmp.height).roundToInt().coerceAtLeast(1)
                        Surface(
                            color = Color(0x99000000),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "$outW × $outH",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                } else {
                    // Cropped region preview for Filters / Tone / Draw (matches saved output 1:1)
                    val sx = (cropNorm.left * bmp.width).roundToInt().coerceIn(0, bmp.width - 1)
                    val sy = (cropNorm.top * bmp.height).roundToInt().coerceIn(0, bmp.height - 1)
                    val cw = ((cropNorm.right - cropNorm.left) * bmp.width).roundToInt().coerceIn(1, bmp.width - sx)
                    val ch = ((cropNorm.bottom - cropNorm.top) * bmp.height).roundToInt().coerceIn(1, bmp.height - sy)
                    val croppedRatio = cw.toFloat() / ch.toFloat()

                    val boxRatio = maxW / maxH
                    val fitW: Float
                    val fitH: Float
                    if (croppedRatio > boxRatio) {
                        fitW = maxW
                        fitH = (maxW / croppedRatio).coerceAtMost(maxH)
                    } else {
                        fitH = maxH
                        fitW = (maxH * croppedRatio).coerceAtMost(maxW)
                    }
                    val fitWdp = with(density) { fitW.toDp() }
                    val fitHdp = with(density) { fitH.toDp() }

                    Box(
                        modifier = Modifier
                            .size(width = fitWdp, height = fitHdp)
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
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageBmp = remember(bmp) { bmp.asImageBitmap() }
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationZ = fineRotationDegrees
                                }
                        ) {
                            drawImage(
                                image = imageBmp,
                                srcOffset = IntOffset(sx, sy),
                                srcSize = IntSize(cw, ch),
                                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                colorFilter = ColorFilter.colorMatrix(liveColorMatrix)
                            )
                        }

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
            }
        }

        // Editor Controls Panel (One UI Rounded Sheet)
        Surface(
            color = Color(0xFF151518),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        fadeIn(tween(200, easing = FastOutSlowInEasing)) togetherWith fadeOut(tween(140))
                    },
                    label = "editor_tab_switch"
                ) { tab ->
                    when (tab) {
                        EditorTab.TRANSFORM -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                // Fine Rotation Slider (-45° .. +45°)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${fineRotationDegrees.roundToInt()}°",
                                        color = if (fineRotationDegrees != 0f) colors.primaryBlue else Color(0xCCFFFFFF),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .width(42.dp)
                                            .clickable { fineRotationDegrees = 0f }
                                    )
                                    Slider(
                                        value = fineRotationDegrees,
                                        onValueChange = { fineRotationDegrees = it },
                                        valueRange = -45f..45f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = colors.primaryBlue,
                                            activeTrackColor = colors.primaryBlue,
                                            inactiveTrackColor = Color(0xFF34343B)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    TransformButton(
                                        icon = Icons.AutoMirrored.Rounded.RotateLeft,
                                        label = "-90°",
                                        onClick = { baseRotationDegrees = (baseRotationDegrees - 90f) % 360f }
                                    )
                                    TransformButton(
                                        icon = Icons.AutoMirrored.Rounded.RotateRight,
                                        label = "+90°",
                                        onClick = { baseRotationDegrees = (baseRotationDegrees + 90f) % 360f }
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
                                    TransformButton(
                                        icon = Icons.Rounded.Refresh,
                                        label = "Redefinir",
                                        onClick = { resetTransform() }
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val imgRatio = transformedBitmap?.let { it.width.toFloat() / it.height.toFloat() } ?: 1f
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(CropAspect.entries) { aspect ->
                                        val selected = cropAspect == aspect
                                        FilterChip(
                                            selected = selected,
                                            onClick = {
                                                cropAspect = aspect
                                                val targetR = resolveTargetRatio(aspect, imgRatio)
                                                if (targetR != null) {
                                                    cropNorm = fitCropToAspect(cropNorm, targetR, imgRatio)
                                                }
                                            },
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
                }

                // Bottom Editor Mode Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EditorTab.entries.forEach { tab ->
                        val selected = tab == activeTab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { activeTab = tab }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
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
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) colors.primaryBlue else Color.White,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = if (active) colors.primaryBlue else Color(0xCCFFFFFF),
            fontSize = 10.sp
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
