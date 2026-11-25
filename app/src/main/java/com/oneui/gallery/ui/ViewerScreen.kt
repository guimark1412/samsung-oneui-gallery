package com.oneui.gallery.ui

import android.content.Intent
import android.graphics.Bitmap
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.oneui.gallery.data.ImageDiagnostics
import com.oneui.gallery.data.MediaRepository
import com.oneui.gallery.data.PixelAiEngine
import com.oneui.gallery.model.BuiltInFilters
import com.oneui.gallery.model.CropAspect
import com.oneui.gallery.model.MediaItem
import com.oneui.gallery.ui.theme.LocalOneUIColors
import com.oneui.gallery.ui.theme.OneUIShapeLarge
import com.oneui.gallery.ui.theme.OneUIShapeMedium
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    items: List<MediaItem>,
    initialIndex: Int,
    thumbnailBounds: Map<Long, Rect> = emptyMap(),
    thumbnailCornerDp: Int = 0,
    isTrashMode: Boolean = false,
    isPrivateMode: Boolean = false,
    onBack: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onTogglePrivate: (MediaItem) -> Unit = {},
    onEdit: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    onTrashOrRestore: (MediaItem) -> Unit,
    onPermanentDelete: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val colors = LocalOneUIColors.current
    val scope = rememberCoroutineScope()

    if (items.isEmpty()) {
        onBack()
        return
    }

    val safeInitial = initialIndex.coerceIn(0, items.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeInitial) { items.size }
    val filmstripState = rememberLazyListState(initialFirstVisibleItemIndex = safeInitial)

    var showControls by remember { mutableStateOf(true) }
    var isInfoExpanded by remember { mutableStateOf(false) }
    var showPermanentDeleteDialog by remember { mutableStateOf(false) }
    var showMoveToTrashDialog by remember { mutableStateOf(false) }
    var activeZoomScale by remember { mutableFloatStateOf(1f) }
    var isPinching by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        activeZoomScale = 1f
        isPinching = false
        runCatching {
            filmstripState.animateScrollToItem((pagerState.currentPage - 2).coerceAtLeast(0))
        }
    }

    val currentItem = items.getOrElse(pagerState.currentPage) { items.first() }
    var realDiagnostics by remember(currentItem.id) { mutableStateOf<ImageDiagnostics?>(null) }
    LaunchedEffect(currentItem.id, isInfoExpanded) {
        if (!currentItem.isVideo && isInfoExpanded && realDiagnostics == null) {
            realDiagnostics = withContext(Dispatchers.Default) {
                val sample = MediaRepository.decodeBitmapForEditing(context, currentItem.uri, 512)
                if (sample != null) {
                    val diag = PixelAiEngine.analyzeBitmap(sample)
                    sample.recycle()
                    diag
                } else null
            }
        }
    }
    val dateFmt = remember { SimpleDateFormat("d 'de' MMMM • HH:mm", Locale("pt", "BR")) }
    val fullDateFmt = remember { SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy • HH:mm", Locale("pt", "BR")) }

    val previewPaddingH by animateDpAsState(
        targetValue = if (isInfoExpanded) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.90f, stiffness = 280f),
        label = "viewer_pad_h"
    )
    val previewCorner by animateDpAsState(
        targetValue = if (isInfoExpanded) 24.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.90f, stiffness = 280f),
        label = "viewer_corner"
    )

    val infoAnim = remember { Animatable(0f) }
    LaunchedEffect(isInfoExpanded) {
        infoAnim.animateTo(
            targetValue = if (isInfoExpanded) 1f else 0f,
            animationSpec = spring(
                dampingRatio = 0.90f,
                stiffness = 280f
            )
        )
    }

    // Hero open/dismiss & interactive drag-down to dismiss state
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingToDismiss by remember { mutableStateOf(false) }

    val initialTargetRaw = remember(safeInitial) { thumbnailBounds[items[safeInitial].id] }
    val openAnim = remember { Animatable(if (initialTargetRaw != null) 0f else 1f) }
    var isOpening by remember { mutableStateOf(initialTargetRaw != null) }
    LaunchedEffect(Unit) {
        if (initialTargetRaw != null) {
            openAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.90f,
                    stiffness = 310f
                )
            )
            isOpening = false
        }
    }

    var isDismissing by remember { mutableStateOf(false) }
    var dismissStartRect by remember { mutableStateOf<Rect?>(null) }
    var dismissTargetRect by remember { mutableStateOf<Rect?>(null) }
    var dismissStartCornerDp by remember { mutableFloatStateOf(0f) }
    var dismissStartBackdropAlpha by remember { mutableFloatStateOf(1f) }
    var dismissMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    val dismissAnim = remember { Animatable(0f) }

    fun computeFullImageRect(media: MediaItem, screenW: Float, screenH: Float): Rect {
        val ratio = if (media.width > 0 && media.height > 0) {
            media.width.toFloat() / media.height.toFloat()
        } else {
            3f / 4f
        }
        val screenRatio = screenW / screenH
        val w: Float
        val h: Float
        if (ratio > screenRatio) {
            w = screenW
            h = (screenW / ratio).coerceAtMost(screenH)
        } else {
            h = screenH
            w = (screenH * ratio).coerceAtMost(screenW)
        }
        val left = (screenW - w) / 2f
        val top = (screenH - h) / 2f
        return Rect(left, top, left + w, top + h)
    }

    fun triggerAnimatedDismiss(fromDragged: Boolean, screenW: Float, screenH: Float) {
        if (isDismissing) return
        isOpening = false
        val item = currentItem
        val fullRect = computeFullImageRect(item, screenW, screenH)
        val rawTarget = thumbnailBounds[item.id]
        val localTarget = rawTarget?.let {
            Rect(
                it.left - rootOffset.x,
                it.top - rootOffset.y,
                it.right - rootOffset.x,
                it.bottom - rootOffset.y
            )
        }

        val df = if (fromDragged) (dragOffsetY / (screenH * 0.42f)).coerceIn(0f, 1f) else 0f
        val ds = 1f - df * 0.44f
        val startW = fullRect.width * ds
        val startH = fullRect.height * ds
        val startCx = fullRect.center.x + (if (fromDragged) dragOffsetX else 0f)
        val startCy = fullRect.center.y + (if (fromDragged) dragOffsetY * 0.9f else 0f)

        dismissStartRect = Rect(
            startCx - startW / 2f,
            startCy - startH / 2f,
            startCx + startW / 2f,
            startCy + startH / 2f
        )
        dismissTargetRect = localTarget
        dismissStartCornerDp = if (fromDragged) df * 22f else 0f
        dismissStartBackdropAlpha = if (fromDragged) (1f - df * 0.82f).coerceIn(0.15f, 1f) else 1f
        dismissMediaItem = item
        isDismissing = true

        scope.launch {
            dismissAnim.snapTo(0f)
            dismissAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.90f,
                    stiffness = 310f
                )
            )
            onBack()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootOffset = coords.positionInRoot()
            }
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val totalHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val sheetHeightDp = maxHeight * 0.56f
        val sheetHeightPx = totalHeightPx * 0.56f

        BackHandler(enabled = !isDismissing) {
            if (isInfoExpanded) {
                isInfoExpanded = false
            } else {
                triggerAnimatedDismiss(fromDragged = false, screenW = totalWidthPx, screenH = totalHeightPx)
            }
        }

        val dragFraction = (dragOffsetY / (totalHeightPx * 0.42f)).coerceIn(0f, 1f)
        val dragScale = 1f - (dragFraction * 0.44f)
        val currentBackdropAlpha = when {
            isDismissing -> dismissStartBackdropAlpha * (1f - dismissAnim.value.coerceIn(0f, 1f))
            isOpening -> openAnim.value.coerceIn(0f, 1f)
            isDraggingToDismiss || dragOffsetY > 0.5f -> (1f - dragFraction * 0.82f).coerceIn(0.15f, 1f)
            else -> 1f
        }

        // Dynamic black scrim (fades as user drags photo down, revealing the grid underneath)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = currentBackdropAlpha }
                .background(Color.Black)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = infoAnim.value.coerceIn(0f, 1f) * currentBackdropAlpha
                }
                .background(colors.background)
        )

        // Photo Pager Area (hidden only while hero open/dismiss transition runs so there's no double photo)
        val showPager = !isOpening && !isDismissing
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (showPager) 1f else 0f }
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isPinching && activeZoomScale <= 1.02f && !isDraggingToDismiss && !isDismissing,
                pageSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = infoAnim.value.coerceIn(0f, 1f)
                        translationY = -sheetHeightPx * 0.44f * p
                        val s = 1f - 0.14f * p
                        scaleX = s
                        scaleY = s
                    }
                    .padding(horizontal = previewPaddingH)
                    .clip(RoundedCornerShape(previewCorner))
            ) { page ->
                val media = items[page]
                val isCurrentPage = page == pagerState.currentPage
                val scaleAnim = remember(media.id) { Animatable(1f) }
                val offsetXAnim = remember(media.id) { Animatable(0f) }
                val offsetYAnim = remember(media.id) { Animatable(0f) }
                val currentIsInfoExpanded by rememberUpdatedState(isInfoExpanded)
                val currentIsDismissing by rememberUpdatedState(isDismissing)
                val currentIsDraggingToDismiss by rememberUpdatedState(isDraggingToDismiss)

                LaunchedEffect(isCurrentPage) {
                    if (!isCurrentPage) {
                        scaleAnim.snapTo(1f)
                        offsetXAnim.snapTo(0f)
                        offsetYAnim.snapTo(0f)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(media.id) {
                            detectTapGestures(
                                onTap = {
                                    if (currentIsInfoExpanded) {
                                        isInfoExpanded = false
                                    } else if (!currentIsDraggingToDismiss && !currentIsDismissing) {
                                        showControls = !showControls
                                    }
                                },
                                onDoubleTap = { tapOffset ->
                                    if (!currentIsInfoExpanded && !currentIsDraggingToDismiss && !currentIsDismissing) {
                                        scope.launch {
                                            val zoomSpring = spring<Float>(
                                                dampingRatio = 0.80f,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                            if (scaleAnim.value > 1.1f) {
                                                if (page == pagerState.currentPage) activeZoomScale = 1f
                                                launch { scaleAnim.animateTo(1f, zoomSpring) }
                                                launch { offsetXAnim.animateTo(0f, zoomSpring) }
                                                launch { offsetYAnim.animateTo(0f, zoomSpring) }
                                            } else {
                                                val targetScale = 2.6f
                                                val centerX = totalWidthPx / 2f
                                                val centerY = totalHeightPx / 2f
                                                val maxX = (totalWidthPx * (targetScale - 1f)) / 2f
                                                val maxY = (totalHeightPx * (targetScale - 1f)) / 2f
                                                val targetX = ((centerX - tapOffset.x) * (targetScale - 1f)).coerceIn(-maxX, maxX)
                                                val targetY = ((centerY - tapOffset.y) * (targetScale - 1f)).coerceIn(-maxY, maxY)
                                                if (page == pagerState.currentPage) activeZoomScale = targetScale
                                                launch { scaleAnim.animateTo(targetScale, zoomSpring) }
                                                launch { offsetXAnim.animateTo(targetX, zoomSpring) }
                                                launch { offsetYAnim.animateTo(targetY, zoomSpring) }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        // Multi-touch pinch + iOS rubber-banded zoom/pan + vertical swipe-up (Info) & drag-down (Dismiss)
                        // PointerEventPass.Initial guarantees HorizontalPager never steals 2-finger pinch!
                        .pointerInput(media.id, totalWidthPx, totalHeightPx) {
                            fun iosSoftBound(value: Float, maxLimit: Float, dimension: Float): Float {
                                val a = abs(value)
                                if (a <= maxLimit) return value
                                val excess = a - maxLimit
                                val dim = dimension.coerceAtLeast(120f)
                                val result = (1f - (1f / ((excess * 0.55f / dim) + 1f))) * dim
                                return sign(value) * (maxLimit + result)
                            }

                            fun invertIosSoftBound(boundedValue: Float, maxLimit: Float, dimension: Float): Float {
                                val a = abs(boundedValue)
                                if (a <= maxLimit) return boundedValue
                                val dim = dimension.coerceAtLeast(120f)
                                val y = (a - maxLimit).coerceAtMost(dim * 0.94f)
                                val excess = dim * y / (0.55f * (dim - y))
                                return sign(boundedValue) * (maxLimit + excess)
                            }

                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                var curScale = scaleAnim.value
                                var rawScale = curScale
                                var curX = offsetXAnim.value
                                var curY = offsetYAnim.value
                                var rawPanX = curX
                                var rawPanY = curY
                                var totalDx = 0f
                                var totalDy = 0f
                                var zoomedPanDx = 0f
                                var zoomedPanDy = 0f
                                var zoomedPanEngaged = false
                                var verticalHandled = false
                                var draggingDownThisGesture = false
                                var didPinchInThisGesture = false

                                try {
                                    do {
                                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                        val pressedCount = event.changes.count { it.pressed }

                                        if (pressedCount >= 2 && !currentIsInfoExpanded && !currentIsDismissing) {
                                            isPinching = true
                                            didPinchInThisGesture = true
                                            if (draggingDownThisGesture) {
                                                draggingDownThisGesture = false
                                                isDraggingToDismiss = false
                                                dragOffsetX = 0f
                                                dragOffsetY = 0f
                                            }
                                            val zoomChange = event.calculateZoom()
                                            val panChange = event.calculatePan()
                                            val centroid = event.calculateCentroid(useCurrent = true)
                                            if (!zoomChange.isNaN() && zoomChange > 0f) {
                                                rawScale = (rawScale * zoomChange).coerceIn(0.45f, 6.2f)
                                                val newScale = when {
                                                    rawScale < 1f -> {
                                                        val ex = 1f - rawScale
                                                        val d = 0.34f
                                                        1f - (1f - (1f / ((ex * 0.55f / d) + 1f))) * d
                                                    }
                                                    rawScale > 4f -> {
                                                        val ex = rawScale - 4f
                                                        val d = 1.25f
                                                        4f + (1f - (1f / ((ex * 0.55f / d) + 1f))) * d
                                                    }
                                                    else -> rawScale
                                                }
                                                val ratio = if (curScale > 0.001f) newScale / curScale else 1f
                                                val candX: Float
                                                val candY: Float
                                                if (centroid != Offset.Unspecified) {
                                                    val cRelX = centroid.x - size.width / 2f
                                                    val cRelY = centroid.y - size.height / 2f
                                                    candX = cRelX - (cRelX - curX) * ratio + panChange.x
                                                    candY = cRelY - (cRelY - curY) * ratio + panChange.y
                                                } else {
                                                    candX = curX + panChange.x
                                                    candY = curY + panChange.y
                                                }
                                                val maxX = if (newScale > 1f) (size.width * (newScale - 1f)) / 2f else 0f
                                                val maxY = if (newScale > 1f) (size.height * (newScale - 1f)) / 2f else 0f
                                                val dimX = if (newScale > 1f) size.width * 0.30f else size.width * 0.16f
                                                val dimY = if (newScale > 1f) size.height * 0.28f else size.height * 0.16f
                                                curScale = newScale
                                                curX = iosSoftBound(candX, maxX, dimX)
                                                curY = iosSoftBound(candY, maxY, dimY)
                                                rawPanX = invertIosSoftBound(curX, maxX, dimX)
                                                rawPanY = invertIosSoftBound(curY, maxY, dimY)
                                                if (page == pagerState.currentPage) {
                                                    activeZoomScale = curScale
                                                }
                                                scope.launch {
                                                    scaleAnim.snapTo(curScale)
                                                    offsetXAnim.snapTo(curX)
                                                    offsetYAnim.snapTo(curY)
                                                }
                                            }
                                            event.changes.forEach { it.consume() }
                                        } else if (pressedCount == 1 && (curScale > 1.03f || didPinchInThisGesture)) {
                                            if (curScale > 1.03f) {
                                                val panChange = event.calculatePan()
                                                zoomedPanDx += panChange.x
                                                zoomedPanDy += panChange.y
                                                if (!zoomedPanEngaged && (abs(zoomedPanDx) > 8f || abs(zoomedPanDy) > 8f)) {
                                                    val maxX = (size.width * (curScale - 1f)) / 2f
                                                    val maxY = (size.height * (curScale - 1f)) / 2f
                                                    rawPanX = invertIosSoftBound(curX, maxX, size.width * 0.30f)
                                                    rawPanY = invertIosSoftBound(curY, maxY, size.height * 0.28f)
                                                    zoomedPanEngaged = true
                                                }
                                                if (zoomedPanEngaged && panChange != Offset.Zero) {
                                                    val maxX = (size.width * (curScale - 1f)) / 2f
                                                    val maxY = (size.height * (curScale - 1f)) / 2f
                                                    val dimX = size.width * 0.30f
                                                    val dimY = size.height * 0.28f
                                                    rawPanX = (rawPanX + panChange.x).coerceIn(-maxX - dimX * 3f, maxX + dimX * 3f)
                                                    rawPanY = (rawPanY + panChange.y).coerceIn(-maxY - dimY * 3f, maxY + dimY * 3f)
                                                    curX = iosSoftBound(rawPanX, maxX, dimX)
                                                    curY = iosSoftBound(rawPanY, maxY, dimY)
                                                    scope.launch {
                                                        offsetXAnim.snapTo(curX)
                                                        offsetYAnim.snapTo(curY)
                                                    }
                                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                                }
                                            } else {
                                                // If user already pinched in this gesture and lifted to 1 finger below 1x, avoid accidental pager swipe
                                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                                            }
                                        } else if (pressedCount == 1 && curScale <= 1.03f && !didPinchInThisGesture && !verticalHandled) {
                                            val panChange = event.calculatePan()
                                            totalDx += panChange.x
                                            totalDy += panChange.y

                                            val minSwipeUpPx = (totalHeightPx * 0.12f).coerceAtLeast(140f)
                                            if (draggingDownThisGesture) {
                                                dragOffsetX += panChange.x
                                                dragOffsetY = (dragOffsetY + panChange.y).coerceAtLeast(0f)
                                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                                            } else if (totalDy < -minSwipeUpPx && abs(totalDy) > abs(totalDx) * 1.65f && !currentIsInfoExpanded) {
                                                isInfoExpanded = true
                                                showControls = true
                                                verticalHandled = true
                                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                                            } else if (totalDy > 65f && abs(totalDy) > abs(totalDx) * 1.35f && currentIsInfoExpanded) {
                                                isInfoExpanded = false
                                                verticalHandled = true
                                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                                            } else if (totalDy > 36f && abs(totalDy) > abs(totalDx) * 1.25f && !currentIsInfoExpanded && !currentIsDismissing) {
                                                draggingDownThisGesture = true
                                                isDraggingToDismiss = true
                                                dragOffsetX = totalDx
                                                dragOffsetY = totalDy.coerceAtLeast(0f)
                                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                } finally {
                                    isPinching = false
                                    if (didPinchInThisGesture || curScale != 1f || zoomedPanEngaged) {
                                        val finalScale = when {
                                            curScale < 1.03f -> 1f
                                            curScale > 4f -> 4f
                                            else -> curScale
                                        }
                                        val maxX = (size.width * (finalScale - 1f)) / 2f
                                        val maxY = (size.height * (finalScale - 1f)) / 2f
                                        val finalX = if (finalScale <= 1.01f) 0f else curX.coerceIn(-maxX, maxX)
                                        val finalY = if (finalScale <= 1.01f) 0f else curY.coerceIn(-maxY, maxY)
                                        if (page == pagerState.currentPage) {
                                            activeZoomScale = finalScale
                                        }
                                        if (finalScale != curScale || finalX != curX || finalY != curY) {
                                            scope.launch {
                                                val bounceSpring = spring<Float>(
                                                    dampingRatio = 0.90f,
                                                    stiffness = 250f
                                                )
                                                launch { scaleAnim.animateTo(finalScale, bounceSpring) }
                                                launch { offsetXAnim.animateTo(finalX, bounceSpring) }
                                                launch { offsetYAnim.animateTo(finalY, bounceSpring) }
                                            }
                                        }
                                    }
                                    if (draggingDownThisGesture) {
                                        isDraggingToDismiss = false
                                        if (dragOffsetY > totalHeightPx * 0.14f) {
                                            triggerAnimatedDismiss(
                                                fromDragged = true,
                                                screenW = totalWidthPx,
                                                screenH = totalHeightPx
                                            )
                                        } else {
                                            val startX = dragOffsetX
                                            val startY = dragOffsetY
                                            scope.launch {
                                                animate(
                                                    initialValue = 1f,
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.90f,
                                                        stiffness = 270f
                                                    )
                                                ) { v, _ ->
                                                    dragOffsetX = startX * v
                                                    dragOffsetY = startY * v
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val activeDragCorner = if (isCurrentPage && dragOffsetY > 0.5f) (dragFraction * 22f).dp else 0.dp
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(media.uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = media.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                if (isCurrentPage && (isDraggingToDismiss || dragOffsetY > 0.5f)) {
                                    translationX = dragOffsetX
                                    translationY = dragOffsetY * 0.9f
                                    scaleX = dragScale
                                    scaleY = dragScale
                                } else {
                                    scaleX = scaleAnim.value
                                    scaleY = scaleAnim.value
                                    translationX = offsetXAnim.value
                                    translationY = offsetYAnim.value
                                }
                            }
                            .then(if (activeDragCorner > 0.dp) Modifier.clip(RoundedCornerShape(activeDragCorner)) else Modifier)
                    )

                    if (media.isVideo && !isInfoExpanded) {
                        Surface(
                            onClick = {
                                runCatching {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(media.uri, media.mimeType)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            shape = CircleShape,
                            color = Color(0xAA000000),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Reproduzir vídeo",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Fullscreen Top Bar (hidden when split-screen Info Sheet is expanded or during drag-dismiss)
            androidx.compose.animation.AnimatedVisibility(
                visible = showControls && !isInfoExpanded && !isDraggingToDismiss && !isDismissing && !isOpening,
                enter = fadeIn(androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                    slideInVertically(androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { -it },
                exit = fadeOut(androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                    slideOutVertically(androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xD9000000), Color(0x66000000), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        triggerAnimatedDismiss(
                            fromDragged = false,
                            screenW = totalWidthPx,
                            screenH = totalHeightPx
                        )
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        Text(
                            text = if (currentItem.dateTaken > 0L) dateFmt.format(Date(currentItem.dateTaken)) else currentItem.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pagerState.currentPage + 1}/${items.size} • ${currentItem.bucketName}",
                            color = Color(0xB8FFFFFF),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { isInfoExpanded = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Detalhes",
                            tint = Color.White
                        )
                    }
                }
            }

            // Fullscreen Bottom Controls: Filmstrip + Minimalist Icons
            androidx.compose.animation.AnimatedVisibility(
                visible = showControls && !isInfoExpanded && !isDraggingToDismiss && !isDismissing && !isOpening,
                enter = fadeIn(androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                    slideInVertically(androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it },
                exit = fadeOut(androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                    slideOutVertically(androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0x99000000), Color(0xEB000000))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(top = 12.dp, bottom = 10.dp)
                ) {
                    if (items.size > 1) {
                        LazyRow(
                            state = filmstripState,
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            itemsIndexed(items, key = { _, it -> it.id }) { idx, thumb ->
                                val isActive = idx == pagerState.currentPage
                                val thumbWidth by animateDpAsState(
                                    targetValue = if (isActive) 46.dp else 30.dp,
                                    label = "filmstrip_width"
                                )
                                val thumbHeight by animateDpAsState(
                                    targetValue = if (isActive) 46.dp else 38.dp,
                                    label = "filmstrip_height"
                                )
                                Box(
                                    modifier = Modifier
                                        .width(thumbWidth)
                                        .height(thumbHeight)
                                        .clip(RoundedCornerShape(if (isActive) 8.dp else 4.dp))
                                        .then(
                                            if (isActive) {
                                                Modifier.border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable {
                                            scope.launch { pagerState.animateScrollToPage(idx) }
                                        }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(thumb.uri)
                                            .size(120)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        alpha = if (isActive) 1f else 0.65f,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isTrashMode) {
                            ViewerIconAction(
                                icon = Icons.Rounded.RestoreFromTrash,
                                description = "Restaurar",
                                tint = colors.primaryBlue,
                                onClick = { onTrashOrRestore(currentItem) }
                            )
                            ViewerIconAction(
                                icon = Icons.Outlined.DeleteForever,
                                description = "Excluir",
                                tint = colors.galleryRose,
                                onClick = { showPermanentDeleteDialog = true }
                            )
                            ViewerIconAction(
                                icon = Icons.Outlined.Info,
                                description = "Detalhes",
                                tint = Color.White,
                                onClick = { isInfoExpanded = true }
                            )
                        } else {
                            ViewerIconAction(
                                icon = if (currentItem.isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                                description = "Favorito",
                                tint = if (currentItem.isFavorite) colors.galleryRose else Color.White,
                                onClick = { onToggleFavorite(currentItem) }
                            )
                            if (!currentItem.isVideo) {
                                ViewerIconAction(
                                    icon = Icons.Outlined.Edit,
                                    description = "Editar",
                                    tint = Color.White,
                                    onClick = { onEdit(currentItem) }
                                )
                            }
                            ViewerIconAction(
                                icon = Icons.Outlined.Share,
                                description = "Compartilhar",
                                tint = Color.White,
                                onClick = { onShare(currentItem) }
                            )
                            ViewerIconAction(
                                icon = Icons.Outlined.Delete,
                                description = "Lixeira",
                                tint = Color.White,
                                onClick = { showMoveToTrashDialog = true }
                            )
                            ViewerIconAction(
                                icon = Icons.Outlined.Info,
                                description = "Detalhes",
                                tint = Color.White,
                                onClick = { isInfoExpanded = true }
                            )
                        }
                    }
                }
            }
        }

        // Hero Shared-Bounds Open / Dismiss Transition Overlay (morphs photo directly to/from its grid cell)
        if (isOpening || isDismissing) {
            val heroItem = if (isDismissing) (dismissMediaItem ?: currentItem) else items[safeInitial]
            val t = if (isDismissing) dismissAnim.value.coerceIn(0f, 1f) else openAnim.value.coerceIn(0f, 1f)
            val fullRect = computeFullImageRect(heroItem, totalWidthPx, totalHeightPx)
            val rawOpenStart = initialTargetRaw?.let {
                Rect(
                    it.left - rootOffset.x,
                    it.top - rootOffset.y,
                    it.right - rootOffset.x,
                    it.bottom - rootOffset.y
                )
            }
            val sRect: Rect?
            val eRect: Rect
            val startCorner: Float
            val endCorner: Float
            val heroAlpha: Float

            if (isDismissing) {
                sRect = dismissStartRect ?: fullRect
                val fallbackRect = Rect(
                    sRect.center.x - sRect.width * 0.22f,
                    totalHeightPx * 0.68f - sRect.height * 0.22f,
                    sRect.center.x + sRect.width * 0.22f,
                    totalHeightPx * 0.68f + sRect.height * 0.22f
                )
                eRect = dismissTargetRect ?: fallbackRect
                startCorner = dismissStartCornerDp
                endCorner = if (dismissTargetRect != null) thumbnailCornerDp.toFloat() else 28f
                heroAlpha = if (dismissTargetRect != null) 1f else (1f - t).coerceIn(0f, 1f)
            } else {
                sRect = rawOpenStart ?: fullRect
                eRect = fullRect
                startCorner = thumbnailCornerDp.toFloat()
                endCorner = 0f
                heroAlpha = 1f
            }

            if (sRect != null) {
                val curLeft = sRect.left + (eRect.left - sRect.left) * t
                val curTop = sRect.top + (eRect.top - sRect.top) * t
                val curW = (sRect.width + (eRect.width - sRect.width) * t).coerceAtLeast(1f)
                val curH = (sRect.height + (eRect.height - sRect.height) * t).coerceAtLeast(1f)
                val curCorner = (startCorner + (endCorner - startCorner) * t).coerceAtLeast(0f)

                val wDp = with(density) { curW.toDp() }
                val hDp = with(density) { curH.toDp() }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(curLeft.roundToInt(), curTop.roundToInt()) }
                        .size(width = wDp, height = hDp)
                        .graphicsLayer { alpha = heroAlpha }
                        .clip(RoundedCornerShape(curCorner.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(heroItem.uri)
                            .build(),
                        contentDescription = heroItem.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // One UI Swipe-Up Interactive Info Sheet (bottom 56%, continuous spring slide + drag)
        if (isInfoExpanded || infoAnim.value > 0.001f) {
            Surface(
                color = colors.surfaceCard,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(sheetHeightDp)
                    .graphicsLayer {
                        val p = infoAnim.value.coerceIn(0f, 1f)
                        translationY = sheetHeightPx * (1f - p)
                        alpha = (p * 1.25f).coerceIn(0f, 1f)
                    }
                    .navigationBarsPadding()
            ) {
                val detailsScrollState = rememberScrollState()
                LaunchedEffect(isInfoExpanded) {
                    if (!isInfoExpanded) {
                        detailsScrollState.scrollTo(0)
                    }
                }
                var detailsSheetDragActive by remember { mutableStateOf(false) }
                val detailsNestedScroll = remember(sheetHeightPx) {
                    object : NestedScrollConnection {
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            if (source == NestedScrollSource.Drag && available.y < 0f && (detailsSheetDragActive || infoAnim.value < 0.999f)) {
                                val next = (infoAnim.value - (available.y / sheetHeightPx)).coerceIn(0f, 1f)
                                if (next >= 0.999f) detailsSheetDragActive = false
                                scope.launch { infoAnim.snapTo(next) }
                                return Offset(0f, available.y)
                            }
                            return Offset.Zero
                        }

                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            if (source == NestedScrollSource.Drag && available.y > 0f && detailsScrollState.value == 0) {
                                detailsSheetDragActive = true
                                val next = (infoAnim.value - (available.y / sheetHeightPx)).coerceIn(0f, 1f)
                                scope.launch { infoAnim.snapTo(next) }
                                return Offset(0f, available.y)
                            }
                            return Offset.Zero
                        }

                        override suspend fun onPreFling(available: Velocity): Velocity {
                            if (detailsSheetDragActive || infoAnim.value < 0.995f) {
                                detailsSheetDragActive = false
                                if (infoAnim.value < 0.72f || available.y > 750f) {
                                    isInfoExpanded = false
                                } else {
                                    scope.launch {
                                        infoAnim.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = 0.90f,
                                                stiffness = 280f
                                            )
                                        )
                                    }
                                }
                                return available
                            }
                            return Velocity.Zero
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Drag handle header (interactive vertical drag down or tap arrow to collapse)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(sheetHeightPx) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (infoAnim.value < 0.82f) {
                                            isInfoExpanded = false
                                            scope.launch {
                                                infoAnim.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.90f,
                                                        stiffness = 280f
                                                    )
                                                )
                                            }
                                        } else {
                                            scope.launch {
                                                infoAnim.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.90f,
                                                        stiffness = 280f
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        scope.launch {
                                            infoAnim.animateTo(
                                                targetValue = if (isInfoExpanded) 1f else 0f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.90f,
                                                    stiffness = 280f
                                                )
                                            )
                                        }
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        val next = (infoAnim.value - (dragAmount / sheetHeightPx)).coerceIn(0f, 1f)
                                        scope.launch { infoAnim.snapTo(next) }
                                    }
                                )
                            }
                            .padding(top = 10.dp, bottom = 6.dp, start = 20.dp, end = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colors.textSecondary.copy(alpha = 0.45f))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Detalhes",
                                color = colors.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = { isInfoExpanded = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Fechar detalhes",
                                    tint = colors.textSecondary
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .nestedScroll(detailsNestedScroll)
                            .verticalScroll(detailsScrollState)
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Quick Action Pills
                        if (!isTrashMode) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (!currentItem.isVideo) {
                                    item {
                                        InfoActionPill(
                                            icon = Icons.Outlined.Edit,
                                            label = "Editar",
                                            tint = colors.primaryBlue,
                                            onClick = { onEdit(currentItem) }
                                        )
                                    }
                                }
                                item {
                                    InfoActionPill(
                                        icon = if (currentItem.isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                                        label = if (currentItem.isFavorite) "Favorito" else "Favoritar",
                                        tint = colors.galleryRose,
                                        onClick = { onToggleFavorite(currentItem) }
                                    )
                                }
                                item {
                                    InfoActionPill(
                                        icon = Icons.Outlined.Share,
                                        label = "Enviar",
                                        tint = colors.textPrimary,
                                        onClick = { onShare(currentItem) }
                                    )
                                }
                                if (!isTrashMode) {
                                    item {
                                        InfoActionPill(
                                            icon = if (isPrivateMode || currentItem.isPrivate) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                            label = if (isPrivateMode || currentItem.isPrivate) "Desbloquear" else "Privado",
                                            tint = colors.primaryBlue,
                                            onClick = { onTogglePrivate(currentItem) }
                                        )
                                    }
                                }
                            }
                        }

                        // Card 1: Date, Time & Filename (One UI 8 26.dp Squircle)
                        Surface(
                            shape = OneUIShapeLarge,
                            color = colors.surfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = if (currentItem.dateTaken > 0L) fullDateFmt.format(Date(currentItem.dateTaken)) else "Data desconhecida",
                                    color = colors.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentItem.name,
                                    color = colors.textSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "/Armazenamento interno/Pictures/${currentItem.bucketName}/${currentItem.name}",
                                    color = colors.textSecondary.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Card 2: Camera EXIF + Live Histogram + Dimensions (One UI 8 26.dp Squircle)
                        Surface(
                            shape = OneUIShapeLarge,
                            color = colors.surfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CameraAlt,
                                        contentDescription = null,
                                        tint = colors.primaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Moto G15 • Câmera Principal",
                                        color = colors.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ExifBadgeChip("f/1.8", Modifier.weight(1f))
                                    ExifBadgeChip("1/125s", Modifier.weight(1f))
                                    ExifBadgeChip("ISO 100", Modifier.weight(1f))
                                    ExifBadgeChip("24mm", Modifier.weight(1f))
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                OneUIHistogramChart(
                                    seed = currentItem.id,
                                    diagnostics = realDiagnostics,
                                    accentBlue = colors.primaryBlue,
                                    accentRose = colors.galleryRose
                                )

                                HorizontalDivider(
                                    color = colors.divider,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )

                                val mp = if (currentItem.width > 0 && currentItem.height > 0) {
                                    val m = (currentItem.width * currentItem.height) / 1_000_000f
                                    String.format(Locale.US, "%.1f MP", m)
                                } else ""
                                DetailRow(
                                    label = "Resolução e Tamanho",
                                    value = (if (currentItem.width > 0) "${currentItem.width} × ${currentItem.height} ($mp) • " else "") +
                                        Formatter.formatFileSize(context, currentItem.size)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                DetailRow(label = "Formato", value = currentItem.mimeType)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    if (showPermanentDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showPermanentDeleteDialog = false },
            containerColor = colors.surfaceCard,
            shape = OneUIShapeMedium,
            title = {
                Text("Excluir permanentemente?", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    "Esta mídia será removida definitivamente do armazenamento do dispositivo.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermanentDeleteDialog = false
                        onPermanentDelete(currentItem)
                    }
                ) {
                    Text("Excluir", color = colors.galleryRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermanentDeleteDialog = false }) {
                    Text("Cancelar", color = colors.primaryBlue)
                }
            }
        )
    }

    if (showMoveToTrashDialog) {
        AlertDialog(
            onDismissRequest = { showMoveToTrashDialog = false },
            containerColor = colors.surfaceCard,
            shape = OneUIShapeMedium,
            title = {
                Text("Mover para a Lixeira?", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    "Este item será movido para a Lixeira e poderá ser restaurado depois.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMoveToTrashDialog = false
                        onTrashOrRestore(currentItem)
                    }
                ) {
                    Text("Mover para a Lixeira", color = colors.galleryRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveToTrashDialog = false }) {
                    Text("Cancelar", color = colors.primaryBlue)
                }
            }
        )
    }
}

@Composable
private fun InfoActionPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    val colors = LocalOneUIColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = colors.surfaceElevated
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ExifBadgeChip(text: String, modifier: Modifier = Modifier) {
    val colors = LocalOneUIColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.surfaceCard,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Text(text = text, color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun OneUIHistogramChart(
    seed: Long,
    diagnostics: ImageDiagnostics?,
    accentBlue: Color,
    accentRose: Color
) {
    val colors = LocalOneUIColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            fun buildBinsPath(bins: FloatArray): Path {
                val path = Path()
                path.moveTo(0f, size.height)
                val last = (bins.size - 1).coerceAtLeast(1)
                for (i in bins.indices) {
                    val x = (i / last.toFloat()) * size.width
                    val v = bins[i].coerceIn(0.02f, 1f)
                    val y = size.height * (1f - v)
                    path.lineTo(x, y)
                }
                path.lineTo(size.width, size.height)
                path.close()
                return path
            }

            if (diagnostics != null) {
                val lumaPath = buildBinsPath(diagnostics.lumaBins)
                val bluePath = buildBinsPath(diagnostics.blueBins)
                val rosePath = buildBinsPath(diagnostics.redBins)

                drawPath(lumaPath, color = Color.White.copy(alpha = 0.16f))
                drawPath(bluePath, color = accentBlue.copy(alpha = 0.28f))
                drawPath(rosePath, color = accentRose.copy(alpha = 0.24f))
                drawPath(lumaPath, color = Color.White.copy(alpha = 0.65f), style = Stroke(width = 1.8f, cap = StrokeCap.Round))
            } else {
                val steps = 28
                val phase = ((seed % 100) / 100f) * 3.14f
                fun buildWave(offsetPhase: Float, freq: Float): Path {
                    val path = Path()
                    path.moveTo(0f, size.height)
                    for (i in 0..steps) {
                        val x = (i / steps.toFloat()) * size.width
                        val norm = i / steps.toFloat()
                        val v = (0.25f + 0.35f * abs(sin(norm * freq + phase + offsetPhase)) * (1f - abs(norm - 0.5f) * 0.6f))
                        val y = size.height * (1f - v.coerceIn(0.08f, 0.94f))
                        path.lineTo(x, y)
                    }
                    path.lineTo(size.width, size.height)
                    path.close()
                    return path
                }
                val lumaPath = buildWave(0f, 4.2f)
                drawPath(lumaPath, color = Color.White.copy(alpha = 0.10f))
            }
        }
    }
}

@Composable
private fun ViewerIconAction(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = LocalOneUIColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = colors.textSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
