package com.oneui.gallery.ui

import android.content.Intent
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.oneui.gallery.model.MediaItem
import com.oneui.gallery.ui.theme.LocalOneUIColors
import com.oneui.gallery.ui.theme.OneUIShapeLarge
import com.oneui.gallery.ui.theme.OneUIShapeMedium
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    onPermanentDelete: (MediaItem) -> Unit
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
    var showInfoSheet by remember { mutableStateOf(false) }
    var showPermanentDeleteDialog by remember { mutableStateOf(false) }
    var activeZoomScale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(pagerState.currentPage) {
        activeZoomScale = 1f
        runCatching {
            filmstripState.animateScrollToItem((pagerState.currentPage - 2).coerceAtLeast(0))
        }
    }

    val currentItem = items.getOrElse(pagerState.currentPage) { items.first() }
    val dateFmt = remember { SimpleDateFormat("d 'de' MMMM • HH:mm", Locale("pt", "BR")) }
    val fullDateFmt = remember { SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy • HH:mm", Locale("pt", "BR")) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = activeZoomScale <= 1.04f,
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val media = items[page]
            var scale by remember(media.id, pagerState.currentPage) { mutableFloatStateOf(1f) }
            var offset by remember(media.id, pagerState.currentPage) { mutableStateOf(Offset.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(media.id) {
                        detectTapGestures(
                            onTap = { showControls = !showControls },
                            onDoubleTap = {
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
                        )
                    }
                    // Custom multi-touch + zoomed-pan gesture that NEVER steals 1-finger horizontal swipes at 1x zoom
                    .pointerInput(media.id) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }

                                if (pressedCount >= 2) {
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
                                }
                                // When pressedCount == 1 && scale <= 1.04f, do NOT consume so HorizontalPager swipes smoothly!
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
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )

                if (media.isVideo) {
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

        // One UI Translucent Top Bar
        AnimatedVisibility(
            visible = showControls,
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
                IconButton(onClick = { showInfoSheet = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Detalhes",
                        tint = Color.White
                    )
                }
            }
        }

        // One UI Bottom Filmstrip + Minimalist 5-Icon Action Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0x99000000), Color(0xEB000000))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(top = 14.dp, bottom = 10.dp)
            ) {
                // Samsung Gallery Interactive Mini-Thumbnail Filmstrip
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
                            onClick = { showInfoSheet = true }
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
                            onClick = { showInfoSheet = true }
                        )
                    }
                }
            }
        }
    }

    if (showInfoSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            sheetState = sheetState,
            containerColor = colors.surfaceCard,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 10.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Detalhes",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = OneUIShapeLarge,
                    color = colors.surfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        DetailRow("Data e hora", if (currentItem.dateTaken > 0L) fullDateFmt.format(Date(currentItem.dateTaken)) else "Desconhecida")
                        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 10.dp))
                        DetailRow("Arquivo", currentItem.name)
                        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 10.dp))
                        DetailRow("Álbum", currentItem.bucketName)
                        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 10.dp))
                        val mp = if (currentItem.width > 0 && currentItem.height > 0) {
                            val m = (currentItem.width * currentItem.height) / 1_000_000f
                            String.format(Locale.US, "%.1f MP", m)
                        } else ""
                        DetailRow(
                            "Resolução e Tamanho",
                            (if (currentItem.width > 0) "${currentItem.width} × ${currentItem.height} ($mp) • " else "") +
                                Formatter.formatFileSize(context, currentItem.size)
                        )
                        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 10.dp))
                        DetailRow("Formato", currentItem.mimeType)
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
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
