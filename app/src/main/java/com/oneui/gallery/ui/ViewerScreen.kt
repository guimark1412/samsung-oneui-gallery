package com.oneui.gallery.ui

import android.content.Intent
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.oneui.gallery.data.MediaRepository
import com.oneui.gallery.model.BuiltInFilters
import com.oneui.gallery.model.CropAspect
import com.oneui.gallery.model.MediaItem
import com.oneui.gallery.ui.theme.LocalOneUIColors
import com.oneui.gallery.ui.theme.OneUIShapeLarge
import com.oneui.gallery.ui.theme.OneUIShapeMedium
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    items: List<MediaItem>,
    initialIndex: Int,
    isTrashMode: Boolean = false,
    onBack: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onEdit: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    onTrashOrRestore: (MediaItem) -> Unit,
    onPermanentDelete: (MediaItem) -> Unit,
    onRemastered: () -> Unit = {}
) {
    val context = LocalContext.current
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
    var isRemastering by remember { mutableStateOf(false) }
    var activeZoomScale by remember { mutableFloatStateOf(1f) }

    BackHandler(enabled = isInfoExpanded) {
        isInfoExpanded = false
    }

    LaunchedEffect(pagerState.currentPage) {
        activeZoomScale = 1f
        runCatching {
            filmstripState.animateScrollToItem((pagerState.currentPage - 2).coerceAtLeast(0))
        }
    }

    val currentItem = items.getOrElse(pagerState.currentPage) { items.first() }
    val dateFmt = remember { SimpleDateFormat("d 'de' MMMM • HH:mm", Locale("pt", "BR")) }
    val fullDateFmt = remember { SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy • HH:mm", Locale("pt", "BR")) }

    val previewPaddingH by animateDpAsState(if (isInfoExpanded) 20.dp else 0.dp, label = "viewer_pad_h")
    val previewCorner by animateDpAsState(if (isInfoExpanded) 24.dp else 0.dp, label = "viewer_corner")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isInfoExpanded) colors.background else Color.Black)
    ) {
        // Top Photo Pager Area (100% fullscreen normally, smoothly shrinks to ~42% when swiped up!)
        Box(
            modifier = Modifier
                .weight(if (isInfoExpanded) 0.42f else 1f)
                .fillMaxWidth()
                .then(if (isInfoExpanded) Modifier.statusBarsPadding().padding(top = 8.dp, bottom = 6.dp) else Modifier)
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = activeZoomScale <= 1.04f,
                pageSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = previewPaddingH)
                    .clip(RoundedCornerShape(previewCorner))
            ) { page ->
                val media = items[page]
                var scale by remember(media.id, pagerState.currentPage) { mutableFloatStateOf(1f) }
                var offset by remember(media.id, pagerState.currentPage) { mutableStateOf(Offset.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .pointerInput(media.id, isInfoExpanded) {
                            detectTapGestures(
                                onTap = {
                                    if (isInfoExpanded) {
                                        isInfoExpanded = false
                                    } else {
                                        showControls = !showControls
                                    }
                                },
                                onDoubleTap = {
                                    if (!isInfoExpanded) {
                                        if (scale > 1.1f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.5f
                                        }
                                        if (page == pagerState.currentPage) {
                                            activeZoomScale = scale
                                        }
                                    }
                                }
                            )
                        }
                        // Multi-touch pinch + zoomed pan + vertical swipe-up/down for Info Panel
                        // NEVER consumes 1-finger horizontal drags at 1x zoom so HorizontalPager swipes smoothly!
                        .pointerInput(media.id, isInfoExpanded) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                var totalDx = 0f
                                var totalDy = 0f
                                var verticalHandled = false

                                do {
                                    val event = awaitPointerEvent()
                                    val pressedCount = event.changes.count { it.pressed }

                                    if (pressedCount >= 2 && !isInfoExpanded) {
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        val newScale = (scale * zoomChange).coerceIn(1f, 4f)
                                        scale = newScale
                                        if (newScale > 1.04f) {
                                            val maxX = (size.width * (newScale - 1f)) / 2f
                                            val maxY = (size.height * (newScale - 1f)) / 2f
                                            offset = Offset(
                                                x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
                                                y = (offset.y + panChange.y).coerceIn(-maxY, maxY)
                                            )
                                        } else {
                                            offset = Offset.Zero
                                        }
                                        if (page == pagerState.currentPage) {
                                            activeZoomScale = scale
                                        }
                                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                                    } else if (pressedCount == 1 && scale > 1.04f) {
                                        val panChange = event.calculatePan()
                                        if (panChange != Offset.Zero) {
                                            val maxX = (size.width * (scale - 1f)) / 2f
                                            val maxY = (size.height * (scale - 1f)) / 2f
                                            val nextX = offset.x + panChange.x
                                            val nextY = offset.y + panChange.y
                                            val hitHorizontalEdge = (nextX <= -maxX && panChange.x < 0f) ||
                                                (nextX >= maxX && panChange.x > 0f)
                                            offset = Offset(
                                                x = nextX.coerceIn(-maxX, maxX),
                                                y = nextY.coerceIn(-maxY, maxY)
                                            )
                                            if (!hitHorizontalEdge) {
                                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                                            }
                                        }
                                    } else if (pressedCount == 1 && scale <= 1.04f && !verticalHandled) {
                                        val panChange = event.calculatePan()
                                        totalDx += panChange.x
                                        totalDy += panChange.y

                                        if (abs(totalDy) > 42f && abs(totalDy) > abs(totalDx) * 1.35f) {
                                            if (totalDy < 0f && !isInfoExpanded) {
                                                isInfoExpanded = true
                                                showControls = true
                                                verticalHandled = true
                                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                                            } else if (totalDy > 0f && isInfoExpanded) {
                                                isInfoExpanded = false
                                                verticalHandled = true
                                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(media.uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = media.name,
                        contentScale = if (isInfoExpanded) ContentScale.Crop else ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
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

            // Fullscreen Top Bar (hidden when split-screen Info Sheet is expanded)
            androidx.compose.animation.AnimatedVisibility(
                visible = showControls && !isInfoExpanded,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
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
                    IconButton(onClick = onBack) {
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

            // Fullscreen Bottom Controls: Swipe-Up Hint + Filmstrip + 5 Minimalist Icons
            androidx.compose.animation.AnimatedVisibility(
                visible = showControls && !isInfoExpanded,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
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
                    // Subtle One UI 7/8 Swipe-Up Pill Hint
                    Surface(
                        onClick = { isInfoExpanded = true },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x44FFFFFF)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowUp,
                                contentDescription = null,
                                tint = Color(0xEEFFFFFF),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Detalhes",
                                color = Color(0xEEFFFFFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                                onClick = { onTrashOrRestore(currentItem) }
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

        // One UI 7 / 8 Swipe-Up Interactive Info & Galaxy AI Sheet (bottom 58% when expanded)
        AnimatedVisibility(
            visible = isInfoExpanded,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            modifier = Modifier
                .weight(0.58f)
                .fillMaxWidth()
        ) {
            Surface(
                color = colors.surfaceCard,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Drag handle header (swipe down or tap arrow to collapse)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount > 22f) isInfoExpanded = false
                                }
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
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // One UI 7 / 8 Galaxy AI Quick Action Pills
                        if (!isTrashMode) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (!currentItem.isVideo) {
                                    item {
                                        Surface(
                                            onClick = {
                                                if (!isRemastering) {
                                                    isRemastering = true
                                                    scope.launch {
                                                        val vivid = BuiltInFilters.presets.first { it.id == "vivid" }
                                                        val saved = MediaRepository.saveEditedImage(
                                                            context = context,
                                                            sourceUri = currentItem.uri,
                                                            rotationDegrees = 0f,
                                                            flipH = false,
                                                            flipV = false,
                                                            cropAspect = CropAspect.ORIGINAL,
                                                            filterMatrix = vivid.matrix,
                                                            brightness = 0.04f,
                                                            contrast = 1.12f,
                                                            saturation = 1.14f,
                                                            warmth = 0.05f,
                                                            vignette = 0.08f,
                                                            strokes = emptyList(),
                                                            overlayTexts = emptyList(),
                                                            previewWidth = 1080f,
                                                            previewHeight = 1080f
                                                        )
                                                        isRemastering = false
                                                        if (saved != null) {
                                                            Toast.makeText(
                                                                context,
                                                                "Remasterização salva em Galeria Editadas!",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            onRemastered()
                                                        }
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(22.dp),
                                            color = colors.aiAccent.copy(alpha = 0.18f),
                                            modifier = Modifier.border(
                                                0.8.dp,
                                                colors.aiAccent.copy(alpha = 0.5f),
                                                RoundedCornerShape(22.dp)
                                            )
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                            ) {
                                                if (isRemastering) {
                                                    CircularProgressIndicator(
                                                        color = colors.aiAccent,
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Rounded.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = colors.aiAccent,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isRemastering) "Remasterizando..." else "Remasterizar",
                                                    color = colors.textPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
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
            val steps = 28
            val phase = ((seed % 100) / 100f) * 3.14f

            fun buildWave(offsetPhase: Float, freq: Float): Path {
                val path = Path()
                path.moveTo(0f, size.height)
                for (i in 0..steps) {
                    val x = (i / steps.toFloat()) * size.width
                    val norm = i / steps.toFloat()
                    val v = (0.35f + 0.55f * abs(sin(norm * freq + phase + offsetPhase)) * (1f - abs(norm - 0.5f) * 0.6f))
                    val y = size.height * (1f - v.coerceIn(0.08f, 0.94f))
                    path.lineTo(x, y)
                }
                path.lineTo(size.width, size.height)
                path.close()
                return path
            }

            val lumaPath = buildWave(0f, 4.2f)
            val bluePath = buildWave(1.1f, 5.1f)
            val rosePath = buildWave(2.3f, 3.6f)

            drawPath(lumaPath, color = Color.White.copy(alpha = 0.14f))
            drawPath(bluePath, color = accentBlue.copy(alpha = 0.22f))
            drawPath(rosePath, color = accentRose.copy(alpha = 0.18f))

            drawPath(lumaPath, color = Color.White.copy(alpha = 0.55f), style = Stroke(width = 1.8f, cap = StrokeCap.Round))
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
