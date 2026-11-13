package com.oneui.gallery.ui

import android.content.Intent
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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

    if (items.isEmpty()) {
        onBack()
        return
    }

    val safeInitial = initialIndex.coerceIn(0, items.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeInitial) { items.size }
    var showControls by remember { mutableStateOf(true) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showPermanentDeleteDialog by remember { mutableStateOf(false) }
    var currentScale by remember { mutableFloatStateOf(1f) }

    val currentItem = items.getOrElse(pagerState.currentPage) { items.first() }
    val dateFmt = remember { SimpleDateFormat("dd 'de' MMM, HH:mm", Locale("pt", "BR")) }
    val fullDateFmt = remember { SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy • HH:mm", Locale("pt", "BR")) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = currentScale <= 1.05f,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val media = items[page]
            var scale by remember(media.id) { mutableFloatStateOf(1f) }
            var offset by remember(media.id) { mutableStateOf(Offset.Zero) }

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
                                if (page == pagerState.currentPage) currentScale = scale
                            }
                        )
                    }
                    .pointerInput(media.id) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            scale = newScale
                            offset = if (newScale > 1.02f) offset + pan else Offset.Zero
                            if (page == pagerState.currentPage) currentScale = newScale
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
                            listOf(Color(0xCC000000), Color(0x66000000), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                    Text(
                        text = if (currentItem.dateTaken > 0L) dateFmt.format(Date(currentItem.dateTaken)) else currentItem.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${currentItem.bucketName} • ${currentItem.name}",
                        color = Color(0xCCFFFFFF),
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

        // One UI Translucent Bottom Action Bar (5 Samsung Icons)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0x88000000), Color(0xDD000000))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isTrashMode) {
                    ViewerBottomAction(
                        icon = Icons.Rounded.RestoreFromTrash,
                        label = "Restaurar",
                        tint = colors.primaryBlue,
                        onClick = { onTrashOrRestore(currentItem) }
                    )
                    ViewerBottomAction(
                        icon = Icons.Outlined.DeleteForever,
                        label = "Excluir",
                        tint = colors.galleryRose,
                        onClick = { showPermanentDeleteDialog = true }
                    )
                    ViewerBottomAction(
                        icon = Icons.Outlined.Info,
                        label = "Detalhes",
                        tint = Color.White,
                        onClick = { showInfoSheet = true }
                    )
                } else {
                    ViewerBottomAction(
                        icon = if (currentItem.isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        label = "Favorito",
                        tint = if (currentItem.isFavorite) colors.galleryRose else Color.White,
                        onClick = { onToggleFavorite(currentItem) }
                    )
                    if (!currentItem.isVideo) {
                        ViewerBottomAction(
                            icon = Icons.Outlined.Edit,
                            label = "Editar",
                            tint = Color.White,
                            onClick = { onEdit(currentItem) }
                        )
                    }
                    ViewerBottomAction(
                        icon = Icons.Outlined.Share,
                        label = "Enviar",
                        tint = Color.White,
                        onClick = { onShare(currentItem) }
                    )
                    ViewerBottomAction(
                        icon = Icons.Outlined.Delete,
                        label = "Lixeira",
                        tint = Color.White,
                        onClick = { onTrashOrRestore(currentItem) }
                    )
                    ViewerBottomAction(
                        icon = Icons.Outlined.Info,
                        label = "Detalhes",
                        tint = Color.White,
                        onClick = { showInfoSheet = true }
                    )
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
                    .padding(horizontal = 24.dp, vertical = 12.dp)
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
                        DetailRow("Data", if (currentItem.dateTaken > 0L) fullDateFmt.format(Date(currentItem.dateTaken)) else "Desconhecida")
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
                            "Resolução",
                            if (currentItem.width > 0) "${currentItem.width} × ${currentItem.height} ($mp)" else "Desconhecida"
                        )
                        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 10.dp))
                        DetailRow("Tamanho", Formatter.formatFileSize(context, currentItem.size))
                        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 10.dp))
                        DetailRow("Tipo", currentItem.mimeType)
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
private fun ViewerBottomAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .pointerInput(label) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color(0xE6FFFFFF),
            fontSize = 11.sp,
            maxLines = 1
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
