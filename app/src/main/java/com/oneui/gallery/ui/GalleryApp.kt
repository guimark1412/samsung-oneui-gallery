package com.oneui.gallery.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.animate
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.oneui.gallery.data.MediaRepository
import com.oneui.gallery.model.AlbumItem
import com.oneui.gallery.model.MediaItem
import com.oneui.gallery.model.StoryItem
import com.oneui.gallery.ui.theme.LocalOneUIColors
import com.oneui.gallery.ui.theme.OneUICollapsingScaffold
import com.oneui.gallery.ui.theme.OneUISectionCard
import com.oneui.gallery.ui.theme.OneUIShapeLarge
import com.oneui.gallery.ui.theme.OneUIShapeMedium
import com.oneui.gallery.ui.theme.OneUIShapeMenu
import com.oneui.gallery.ui.theme.OneUITheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class GalleryTab(val label: String) {
    PICTURES("Fotos"),
    ALBUMS("Álbuns"),
    STORIES("Histórias"),
    MENU("Menu")
}

private enum class TimelineScale(
    val label: String,
    val columns: Int,
    val cornerDp: Int,
    val spacingDp: Float
) {
    DAY("Por Dia", 3, 0, 2f),
    WEEK("Por Semana", 4, 0, 2f),
    MONTH("Por Mês", 5, 0, 1.5f),
    YEAR("Por Ano", 6, 0, 1.5f)
}

private fun groupMediaByTimelineScale(
    items: List<MediaItem>,
    scale: TimelineScale
): List<Pair<String, List<MediaItem>>> {
    if (items.isEmpty()) return emptyList()
    val ptBr = Locale("pt", "BR")
    val now = Calendar.getInstance()
    val curYear = now.get(Calendar.YEAR)
    val curDayOfYear = now.get(Calendar.DAY_OF_YEAR)
    val curWeek = now.get(Calendar.WEEK_OF_YEAR)

    val daySameYearFmt = SimpleDateFormat("d 'de' MMMM", ptBr)
    val dayOtherYearFmt = SimpleDateFormat("d 'de' MMM 'de' yyyy", ptBr)
    val weekBoundFmt = SimpleDateFormat("d 'de' MMM", ptBr)
    val monthFmt = SimpleDateFormat("MMMM 'de' yyyy", ptBr)

    val groups = LinkedHashMap<String, MutableList<MediaItem>>()
    // Cache de rótulos por "balde" de data: formata cada dia/semana/mês uma única vez
    val labelCache = HashMap<Int, String>()
    val cal = Calendar.getInstance()
    val weekStartCal = Calendar.getInstance()
    val weekEndCal = Calendar.getInstance()
    val nowMs = System.currentTimeMillis()

    for (item in items) {
        val ts = if (item.dateTaken > 0L) item.dateTaken else nowMs
        cal.timeInMillis = ts
        val year = cal.get(Calendar.YEAR)
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        val bucket = when (scale) {
            TimelineScale.DAY -> year * 1000 + dayOfYear
            TimelineScale.WEEK -> year * 100 + week
            TimelineScale.MONTH -> year * 100 + cal.get(Calendar.MONTH)
            TimelineScale.YEAR -> year
        }

        val key = labelCache.getOrPut(bucket) {
            when (scale) {
                TimelineScale.DAY -> when {
                    year == curYear && dayOfYear == curDayOfYear -> "Hoje"
                    year == curYear && dayOfYear == curDayOfYear - 1 -> "Ontem"
                    year == curYear -> daySameYearFmt.format(Date(ts))
                    else -> dayOtherYearFmt.format(Date(ts))
                }
                TimelineScale.WEEK -> {
                    if (year == curYear && week == curWeek) {
                        "Esta semana"
                    } else if (year == curYear && week == curWeek - 1) {
                        "Semana passada"
                    } else {
                        weekStartCal.timeInMillis = ts
                        weekStartCal.set(Calendar.DAY_OF_WEEK, weekStartCal.firstDayOfWeek)
                        weekEndCal.timeInMillis = weekStartCal.timeInMillis
                        weekEndCal.add(Calendar.DAY_OF_YEAR, 6)
                        val s = weekBoundFmt.format(weekStartCal.time)
                        val e = weekBoundFmt.format(weekEndCal.time)
                        "Semana de $s – $e"
                    }
                }
                TimelineScale.MONTH -> monthFmt.format(Date(ts)).replaceFirstChar { it.titlecase(ptBr) }
                TimelineScale.YEAR -> year.toString()
            }
        }
        groups.getOrPut(key) { mutableListOf() }.add(item)
    }
    return groups.entries.map { it.key to it.value }
}

private sealed interface CollectionRoute {
    data class Album(val album: AlbumItem) : CollectionRoute
    data object Favorites : CollectionRoute
    data object Videos : CollectionRoute
    data object Recent : CollectionRoute
    data object Trash : CollectionRoute
    data object Private : CollectionRoute
}

@Composable
fun GalleryApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDarkMode by remember {
        mutableStateOf(MediaRepository.isDarkMode(context) ?: true)
    }

    val requiredPermissions = remember {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun checkHasPermission(): Boolean {
        return requiredPermissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    var hasPermission by remember { mutableStateOf(checkHasPermission()) }
    var allMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isGeneratingSamples by remember { mutableStateOf(false) }

    var currentTab by remember { mutableStateOf(GalleryTab.PICTURES) }
    var timelineScale by remember { mutableStateOf(TimelineScale.WEEK) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showOverflowMenu by remember { mutableStateOf(false) }

    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    var collectionRoute by remember { mutableStateOf<CollectionRoute?>(null) }
    var viewerRequest by remember { mutableStateOf<Triple<List<MediaItem>, Int, Boolean>?>(null) }
    val thumbnailBounds = remember { HashMap<Long, Rect>() }
    var editingItem by remember { mutableStateOf<MediaItem?>(null) }
    var editingCollageItems by remember { mutableStateOf<List<MediaItem>?>(null) }
    var activeStory by remember { mutableStateOf<StoryItem?>(null) }

    var showPinDialogMode by remember { mutableStateOf<String?>(null) } // "unlock_private" or "change_pin"
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    fun refreshMedia() {
        if (!hasPermission) return
        scope.launch {
            isLoading = true
            allMedia = MediaRepository.loadMedia(context)
            isLoading = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasPermission = grants.values.any { it } || checkHasPermission()
        if (hasPermission) {
            refreshMedia()
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            refreshMedia()
        }
    }

    fun shareItems(items: List<MediaItem>) {
        if (items.isEmpty()) return
        runCatching {
            val intent = if (items.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = items.first().mimeType
                    putExtra(Intent.EXTRA_STREAM, items.first().uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(items.map { it.uri }))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar mídia"))
        }
    }

    fun triggerSampleGeneration() {
        if (isGeneratingSamples) return
        scope.launch {
            isGeneratingSamples = true
            val count = MediaRepository.generateSamplePhotos(context)
            allMedia = MediaRepository.loadMedia(context)
            isGeneratingSamples = false
            Toast.makeText(
                context,
                if (count > 0) "$count fotos de demonstração criadas!" else "Permissão necessária para criar fotos.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    OneUITheme(darkTheme = isDarkMode) {
        val colors = LocalOneUIColors.current

        val activeItems = remember(allMedia) { allMedia.filter { !it.isTrashed && !it.isPrivate } }
        val privateItems = remember(allMedia) { allMedia.filter { !it.isTrashed && it.isPrivate } }
        val trashedItems = remember(allMedia) { allMedia.filter { it.isTrashed } }
        val albums = remember(activeItems) { MediaRepository.buildAlbums(activeItems) }
        val stories = remember(activeItems) { MediaRepository.buildStories(activeItems) }

        fun openPrivateAlbum() {
            pinInput = ""
            pinError = null
            showPinDialogMode = "unlock_private"
        }

        fun openChangePrivatePin() {
            pinInput = ""
            pinError = null
            showPinDialogMode = "change_pin"
        }

        val displayedPictures = remember(activeItems, searchQuery) {
            if (searchQuery.isBlank()) activeItems
            else activeItems.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.bucketName.contains(searchQuery, ignoreCase = true) ||
                    (searchQuery.equals("Favoritos", ignoreCase = true) && it.isFavorite)
            }
        }

        BackHandler(
            enabled = editingItem != null || editingCollageItems != null ||
                activeStory != null || collectionRoute != null || selectionMode || showSearch ||
                showPinDialogMode != null
        ) {
            when {
                showPinDialogMode != null -> showPinDialogMode = null
                editingItem != null -> editingItem = null
                editingCollageItems != null -> editingCollageItems = null
                activeStory != null -> activeStory = null
                selectionMode -> {
                    selectionMode = false
                    selectedIds.clear()
                }
                collectionRoute != null -> collectionRoute = null
                showSearch -> {
                    showSearch = false
                    searchQuery = ""
                }
            }
        }

        if (!hasPermission) {
            PermissionOnboardingScreen(
                onRequest = { permissionLauncher.launch(requiredPermissions) }
            )
            return@OneUITheme
        }

        val currentEditing = editingItem
        if (currentEditing != null) {
            EditorScreen(
                item = currentEditing,
                onBack = { editingItem = null },
                onSaved = {
                    editingItem = null
                    viewerRequest = null
                    refreshMedia()
                }
            )
            return@OneUITheme
        }

        val currentCollage = editingCollageItems
        if (currentCollage != null) {
            CollageScreen(
                items = currentCollage,
                onBack = { editingCollageItems = null },
                onSaved = {
                    editingCollageItems = null
                    selectionMode = false
                    selectedIds.clear()
                    refreshMedia()
                }
            )
            return@OneUITheme
        }

        val currentStory = activeStory
        if (currentStory != null) {
            StoryPlayerOverlay(
                story = currentStory,
                onClose = { activeStory = null }
            )
            return@OneUITheme
        }

        val currentViewer = viewerRequest

        Box(modifier = Modifier.fillMaxSize()) {
            val route = collectionRoute
            if (route != null) {
                val isPrivateRoute = route is CollectionRoute.Private
                val (routeTitle, routeItems, isTrashRoute) = when (route) {
                is CollectionRoute.Album -> Triple(
                    route.album.name,
                    activeItems.filter { it.bucketId == route.album.id },
                    false
                )
                CollectionRoute.Favorites -> Triple(
                    "Favoritos",
                    activeItems.filter { it.isFavorite },
                    false
                )
                CollectionRoute.Videos -> Triple(
                    "Vídeos",
                    activeItems.filter { it.isVideo },
                    false
                )
                CollectionRoute.Recent -> Triple(
                    "Recentes",
                    activeItems.take(30),
                    false
                )
                CollectionRoute.Trash -> Triple(
                    "Lixeira",
                    trashedItems,
                    true
                )
                CollectionRoute.Private -> Triple(
                    "Álbum Privado",
                    privateItems,
                    false
                )
            }

            OneUICollapsingScaffold(
                title = routeTitle,
                subtitle = "${routeItems.size} ${if (routeItems.size == 1) "item" else "itens"}",
                navigationIcon = {
                    IconButton(onClick = {
                        selectionMode = false
                        selectedIds.clear()
                        collectionRoute = null
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Voltar",
                            tint = colors.textPrimary
                        )
                    }
                },
                actions = {
                    if (isTrashRoute && routeItems.isNotEmpty()) {
                        TextButton(onClick = {
                            MediaRepository.restoreFromTrash(context, routeItems.map { it.id })
                            refreshMedia()
                            Toast.makeText(context, "Todos os itens restaurados", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Restaurar", color = colors.primaryBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        TextButton(onClick = {
                            scope.launch {
                                MediaRepository.deletePermanently(context, routeItems)
                                refreshMedia()
                                Toast.makeText(context, "Lixeira esvaziada", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Esvaziar", color = colors.galleryRose, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    } else if (isPrivateRoute) {
                        TextButton(onClick = { openChangePrivatePin() }) {
                            Text("PIN", color = colors.primaryBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        if (routeItems.isNotEmpty()) {
                            TextButton(onClick = {
                                MediaRepository.removeFromPrivate(context, routeItems.map { it.id })
                                refreshMedia()
                                Toast.makeText(context, "Itens desbloqueados para a galeria", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Desbloquear", color = colors.primaryBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            ) {
                if (routeItems.isEmpty()) {
                    EmptyStateCard(
                        icon = when {
                            isTrashRoute -> Icons.Outlined.DeleteSweep
                            isPrivateRoute -> Icons.Outlined.Lock
                            else -> Icons.Outlined.PhotoLibrary
                        },
                        title = when {
                            isTrashRoute -> "Lixeira vazia"
                            isPrivateRoute -> "Álbum Privado vazio"
                            else -> "Nenhum item em $routeTitle"
                        },
                        subtitle = when {
                            isTrashRoute -> "Itens excluídos ficam aqui antes da remoção permanente."
                            isPrivateRoute -> "Selecione fotos ou toque em Privado no visualizador para ocultar itens com PIN."
                            else -> "As mídias correspondentes aparecerão aqui."
                        },
                        actionLabel = null,
                        onAction = null
                    )
                } else {
                    MediaGridContent(
                        items = routeItems,
                        timelineScale = timelineScale,
                        onTimelineScaleChange = { timelineScale = it },
                        selectionMode = selectionMode,
                        selectedIds = selectedIds,
                        thumbnailBounds = thumbnailBounds,
                        onToggleSelect = { id ->
                            if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
                            if (selectedIds.isEmpty()) selectionMode = false
                        },
                        onEnterSelection = { id ->
                            selectionMode = true
                            if (!selectedIds.contains(id)) selectedIds.add(id)
                        },
                        onItemClick = { idx ->
                            viewerRequest = Triple(routeItems, idx, isTrashRoute)
                        }
                    )
                }
            }
            } else {
        // Main Samsung One UI 7 / 8 Gallery 4-Tab Scaffold
        val tabTitle = when {
            selectionMode -> "${selectedIds.size} selecionado(s)"
            currentTab == GalleryTab.PICTURES -> "Galeria"
            else -> currentTab.label
        }
        val tabSubtitle = when (currentTab) {
            GalleryTab.PICTURES -> "${displayedPictures.size} ${if (displayedPictures.size == 1) "item" else "itens"} • ${timelineScale.label}"
            GalleryTab.ALBUMS -> "${albums.size} ${if (albums.size == 1) "álbum" else "álbuns"}"
            GalleryTab.STORIES -> "${stories.size} ${if (stories.size == 1) "história" else "histórias"}"
            GalleryTab.MENU -> "One UI • Estúdio & Privacidade"
        }

        OneUICollapsingScaffold(
            title = tabTitle,
            subtitle = if (selectionMode) "Toque para marcar ou desmarcar" else tabSubtitle,
            navigationIcon = if (selectionMode) {
                {
                    IconButton(onClick = {
                        selectionMode = false
                        selectedIds.clear()
                    }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cancelar seleção", tint = colors.textPrimary)
                    }
                }
            } else null,
            actions = {
                if (selectionMode) {
                    IconButton(onClick = {
                        if (selectedIds.size == displayedPictures.size) {
                            selectedIds.clear()
                        } else {
                            selectedIds.clear()
                            selectedIds.addAll(displayedPictures.map { it.id })
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.SelectAll,
                            contentDescription = "Selecionar tudo",
                            tint = colors.primaryBlue
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        if (currentTab == GalleryTab.PICTURES) {
                            OneUICircularGlassAction(
                                icon = Icons.Outlined.Search,
                                description = "Pesquisar",
                                tint = if (showSearch) colors.primaryBlue else colors.textPrimary,
                                onClick = {
                                    showSearch = !showSearch
                                    if (!showSearch) searchQuery = ""
                                }
                            )
                        }

                        // Overflow Menu (⋮)
                        Box {
                            OneUICircularGlassAction(
                                icon = Icons.Rounded.MoreVert,
                                description = "Mais opções",
                                tint = colors.textPrimary,
                                onClick = { showOverflowMenu = true }
                            )
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                                modifier = Modifier
                                    .background(colors.popupMenu, OneUIShapeMenu)
                                    .clip(OneUIShapeMenu)
                            ) {
                                if (currentTab == GalleryTab.PICTURES && displayedPictures.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Editar / Selecionar", color = colors.textPrimary, fontSize = 15.sp) },
                                        leadingIcon = { Icon(Icons.Outlined.CheckCircle, null, tint = colors.textPrimary) },
                                        onClick = {
                                            showOverflowMenu = false
                                            selectionMode = true
                                            selectedIds.clear()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            val nextScale = TimelineScale.entries[(timelineScale.ordinal + 1) % TimelineScale.entries.size]
                                            Text("Visualização: ${nextScale.label}", color = colors.textPrimary, fontSize = 15.sp)
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.GridView, null, tint = colors.textPrimary) },
                                        onClick = {
                                            showOverflowMenu = false
                                            timelineScale = TimelineScale.entries[(timelineScale.ordinal + 1) % TimelineScale.entries.size]
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Álbum Privado (PIN)", color = colors.textPrimary, fontSize = 15.sp) },
                                    leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = colors.primaryBlue) },
                                    onClick = {
                                        showOverflowMenu = false
                                        openPrivateAlbum()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isDarkMode) "Mudar para Tema Claro" else "Mudar para Tema Escuro",
                                            color = colors.textPrimary,
                                            fontSize = 15.sp
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.DarkMode, null, tint = colors.textPrimary) },
                                    onClick = {
                                        showOverflowMenu = false
                                        isDarkMode = !isDarkMode
                                        MediaRepository.setDarkMode(context, isDarkMode)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Gerar 12 fotos de exemplo", color = colors.primaryBlue, fontSize = 15.sp) },
                                    leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null, tint = colors.primaryBlue) },
                                    onClick = {
                                        showOverflowMenu = false
                                        triggerSampleGeneration()
                                    }
                                )
                            }
                        }
                    }
                }
            },
            headerExtra = {
                AnimatedVisibility(visible = showSearch && currentTab == GalleryTab.PICTURES) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar por nome, álbum ou categoria...", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.primaryBlue) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Limpar")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(26.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.surfaceCard,
                                unfocusedContainerColor = colors.surfaceCard,
                                focusedBorderColor = colors.primaryBlue,
                                unfocusedBorderColor = colors.divider,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        // One UI 8 Smart Search Chips
                        val smartQueries = listOf("Câmera", "Viagem", "Screenshots", "Downloads", "Favoritos")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(smartQueries) { chip ->
                                val selected = searchQuery.equals(chip, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { searchQuery = if (selected) "" else chip },
                                    label = { Text(chip, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.primaryBlue,
                                        selectedLabelColor = Color.White,
                                        containerColor = colors.surfaceCard,
                                        labelColor = colors.textSecondary
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (selectionMode) {
                    val selectedPhotos = remember(displayedPictures, selectedIds.size) {
                        displayedPictures.filter { selectedIds.contains(it.id) && !it.isVideo }
                    }
                    SelectionBottomBar(
                        selectedCount = selectedIds.size,
                        canCollage = selectedPhotos.size in 2..6,
                        onShare = {
                            val itemsToShare = displayedPictures.filter { selectedIds.contains(it.id) }
                            shareItems(itemsToShare)
                        },
                        onFavorite = {
                            MediaRepository.setFavorites(context, selectedIds.toList(), true)
                            selectionMode = false
                            selectedIds.clear()
                            refreshMedia()
                            Toast.makeText(context, "Adicionado aos Favoritos", Toast.LENGTH_SHORT).show()
                        },
                        onCollage = {
                            editingCollageItems = selectedPhotos
                        },
                        onPrivate = {
                            MediaRepository.moveToPrivate(context, selectedIds.toList())
                            selectionMode = false
                            selectedIds.clear()
                            refreshMedia()
                            Toast.makeText(context, "Movido para o Álbum Privado", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = {
                            MediaRepository.moveToTrash(context, selectedIds.toList())
                            selectionMode = false
                            selectedIds.clear()
                            refreshMedia()
                            Toast.makeText(context, "Movido para a Lixeira", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    OneUIFloatingCapsuleBottomBar(
                        currentTab = currentTab,
                        isSearchOpen = showSearch,
                        onSelectTab = { tab ->
                            currentTab = tab
                            if (tab != GalleryTab.PICTURES) showSearch = false
                        },
                        onToggleSearch = {
                            currentTab = GalleryTab.PICTURES
                            showSearch = !showSearch
                            if (!showSearch) searchQuery = ""
                        }
                    )
                }
            }
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(tween(220, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(tween(150))
                },
                label = "main_tab_transition"
            ) { activeTab ->
            when (activeTab) {
                GalleryTab.PICTURES -> {
                    when {
                        isLoading && activeItems.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = colors.primaryBlue)
                            }
                        }
                        displayedPictures.isEmpty() -> {
                            EmptyStateCard(
                                icon = Icons.Outlined.PhotoLibrary,
                                title = if (searchQuery.isNotBlank()) "Nenhum resultado" else "Sua Galeria está vazia",
                                subtitle = if (searchQuery.isNotBlank()) {
                                    "Tente outro termo de pesquisa."
                                } else {
                                    "O emulador ou dispositivo ainda não possui fotos. Gere 12 fotos artísticas de exemplo para testar todos os recursos agora!"
                                },
                                actionLabel = if (searchQuery.isBlank()) {
                                    if (isGeneratingSamples) "Gerando fotos..." else "Gerar 12 Fotos de Demonstração"
                                } else null,
                                onAction = if (searchQuery.isBlank()) ({ triggerSampleGeneration() }) else null
                            )
                        }
                        else -> {
                            MediaGridContent(
                                items = displayedPictures,
                                timelineScale = timelineScale,
                                onTimelineScaleChange = { newScale -> timelineScale = newScale },
                                selectionMode = selectionMode,
                                selectedIds = selectedIds,
                                thumbnailBounds = thumbnailBounds,
                                onToggleSelect = { id ->
                                    if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
                                    if (selectedIds.isEmpty()) selectionMode = false
                                },
                                onEnterSelection = { id ->
                                    selectionMode = true
                                    if (!selectedIds.contains(id)) selectedIds.add(id)
                                },
                                onItemClick = { idx ->
                                    viewerRequest = Triple(displayedPictures, idx, false)
                                }
                            )
                        }
                    }
                }

                GalleryTab.ALBUMS -> {
                    AlbumsTabContent(
                        albums = albums,
                        activeItems = activeItems,
                        privateCount = privateItems.size,
                        trashedCount = trashedItems.size,
                        onAlbumClick = { album -> collectionRoute = CollectionRoute.Album(album) },
                        onOpenFavorites = { collectionRoute = CollectionRoute.Favorites },
                        onOpenVideos = { collectionRoute = CollectionRoute.Videos },
                        onOpenRecent = { collectionRoute = CollectionRoute.Recent },
                        onOpenPrivate = { openPrivateAlbum() },
                        onOpenTrash = { collectionRoute = CollectionRoute.Trash },
                        onGenerateSamples = { triggerSampleGeneration() },
                        isGenerating = isGeneratingSamples
                    )
                }

                GalleryTab.STORIES -> {
                    StoriesTabContent(
                        stories = stories,
                        onPlayStory = { story -> activeStory = story },
                        onGenerateSamples = { triggerSampleGeneration() },
                        isGenerating = isGeneratingSamples
                    )
                }

                GalleryTab.MENU -> {
                    MenuTabContent(
                        favoritesCount = activeItems.count { it.isFavorite },
                        videosCount = activeItems.count { it.isVideo },
                        recentCount = activeItems.take(30).size,
                        privateCount = privateItems.size,
                        trashCount = trashedItems.size,
                        isDarkMode = isDarkMode,
                        isGenerating = isGeneratingSamples,
                        onOpenFavorites = { collectionRoute = CollectionRoute.Favorites },
                        onOpenVideos = { collectionRoute = CollectionRoute.Videos },
                        onOpenRecent = { collectionRoute = CollectionRoute.Recent },
                        onOpenPrivate = { openPrivateAlbum() },
                        onChangePrivatePin = { openChangePrivatePin() },
                        onOpenTrash = { collectionRoute = CollectionRoute.Trash },
                        onGenerateSamples = { triggerSampleGeneration() },
                        onToggleDarkMode = { dark ->
                            isDarkMode = dark
                            MediaRepository.setDarkMode(context, dark)
                        }
                    )
                }
            }
            }
        }

        if (showPinDialogMode != null) {
            val isChanging = showPinDialogMode == "change_pin"
            AlertDialog(
                onDismissRequest = { showPinDialogMode = null },
                containerColor = colors.surfaceCard,
                shape = OneUIShapeMedium,
                title = {
                    Text(
                        text = if (isChanging) "Alterar PIN do Álbum Privado" else "Álbum Privado",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (isChanging) {
                                "Digite um novo PIN de 4 dígitos para proteger suas fotos privadas."
                            } else {
                                "Digite o PIN de 4 dígitos (padrão: 0000)."
                            },
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { v ->
                                val digits = v.filter { it.isDigit() }.take(4)
                                pinInput = digits
                                pinError = null
                            },
                            singleLine = true,
                            label = { Text("PIN (4 dígitos)") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = pinError != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (pinError != null) {
                            Text(
                                text = pinError!!,
                                color = colors.galleryRose,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (isChanging) {
                                if (pinInput.length == 4) {
                                    MediaRepository.setPrivatePin(context, pinInput)
                                    showPinDialogMode = null
                                    Toast.makeText(context, "PIN atualizado!", Toast.LENGTH_SHORT).show()
                                } else {
                                    pinError = "O PIN deve ter exatamente 4 números."
                                }
                            } else {
                                val expected = MediaRepository.getPrivatePin(context)
                                if (pinInput == expected) {
                                    showPinDialogMode = null
                                    collectionRoute = CollectionRoute.Private
                                } else {
                                    pinError = "PIN incorreto. Tente novamente."
                                }
                            }
                        }
                    ) {
                        Text(
                            text = if (isChanging) "Salvar" else "Desbloquear",
                            color = colors.primaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPinDialogMode = null }) {
                        Text("Cancelar", color = colors.textSecondary)
                    }
                }
            )
        }
            }

            if (currentViewer != null) {
                val (viewerList, initialIdx, isTrash) = currentViewer
                val isPrivateRouteViewer = collectionRoute is CollectionRoute.Private && !isTrash
                val syncedList = remember(viewerList, allMedia, isPrivateRouteViewer) {
                    val map = allMedia.associateBy { it.id }
                    viewerList.mapNotNull { map[it.id] }.filter {
                        when {
                            isTrash -> it.isTrashed
                            isPrivateRouteViewer -> !it.isTrashed && it.isPrivate
                            else -> !it.isTrashed && !it.isPrivate
                        }
                    }
                }
                if (syncedList.isEmpty()) {
                    viewerRequest = null
                } else {
                    ViewerScreen(
                        items = syncedList,
                        initialIndex = initialIdx.coerceIn(0, syncedList.lastIndex),
                        thumbnailBounds = thumbnailBounds,
                        thumbnailCornerDp = timelineScale.cornerDp,
                        isTrashMode = isTrash,
                        isPrivateMode = isPrivateRouteViewer,
                        onBack = { viewerRequest = null },
                        onToggleFavorite = { item ->
                            MediaRepository.toggleFavorite(context, item.id)
                            refreshMedia()
                        },
                        onTogglePrivate = { item ->
                            if (item.isPrivate) {
                                MediaRepository.removeFromPrivate(context, listOf(item.id))
                                Toast.makeText(context, "Removido do Álbum Privado", Toast.LENGTH_SHORT).show()
                            } else {
                                MediaRepository.moveToPrivate(context, listOf(item.id))
                                Toast.makeText(context, "Movido para o Álbum Privado", Toast.LENGTH_SHORT).show()
                            }
                            refreshMedia()
                        },
                        onEdit = { item -> editingItem = item },
                        onShare = { item -> shareItems(listOf(item)) },
                        onTrashOrRestore = { item ->
                            if (isTrash) {
                                MediaRepository.restoreFromTrash(context, listOf(item.id))
                                Toast.makeText(context, "Restaurado para a galeria", Toast.LENGTH_SHORT).show()
                            } else {
                                MediaRepository.moveToTrash(context, listOf(item.id))
                                Toast.makeText(context, "Movido para a Lixeira", Toast.LENGTH_SHORT).show()
                            }
                            refreshMedia()
                        },
                        onPermanentDelete = { item ->
                            scope.launch {
                                MediaRepository.deletePermanently(context, listOf(item))
                                refreshMedia()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OneUICircularGlassAction(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    val colors = LocalOneUIColors.current
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colors.floatingPill,
        modifier = Modifier
            .size(38.dp)
            .border(0.6.dp, colors.floatingBorder, CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private class SlotRectHolder(var rect: Rect? = null)

private class GridFlipTracker {
    val currentBounds = HashMap<Long, Rect>()
    val visualBounds = HashMap<Long, Rect>()
    val previousBounds = HashMap<Long, Rect>()
    var activeScale: TimelineScale? = null
    var isZoomingOut: Boolean = false
    var generation: Int = 0

    fun onScaleSync(newScale: TimelineScale) {
        val prev = activeScale
        if (prev != null && prev != newScale) {
            isZoomingOut = newScale.ordinal > prev.ordinal
            previousBounds.clear()
            if (visualBounds.isNotEmpty()) {
                previousBounds.putAll(visualBounds)
            } else {
                previousBounds.putAll(currentBounds)
            }
            generation++
        }
        activeScale = newScale
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGridContent(
    items: List<MediaItem>,
    timelineScale: TimelineScale,
    onTimelineScaleChange: (TimelineScale) -> Unit,
    selectionMode: Boolean,
    selectedIds: List<Long>,
    thumbnailBounds: MutableMap<Long, Rect>,
    onToggleSelect: (Long) -> Unit,
    onEnterSelection: (Long) -> Unit,
    onItemClick: (Int) -> Unit
) {
    val colors = LocalOneUIColors.current
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val indexById = remember(items) {
        items.mapIndexed { index, item -> item.id to index }.toMap()
    }
    // O(1) por item em vez de List.contains (O(n)) em cada célula
    val selectedSet by remember(selectedIds) { derivedStateOf { selectedIds.toHashSet() } }

    // ---- Agrupamentos: escala atual síncrona, demais pré-calculadas em background ----
    val groupingCache = remember(items) { HashMap<TimelineScale, List<Pair<String, List<MediaItem>>>>() }
    val grouped = groupingCache.getOrPut(timelineScale) { groupMediaByTimelineScale(items, timelineScale) }
    LaunchedEffect(items) {
        val missing = TimelineScale.entries.filter { !groupingCache.containsKey(it) }
        if (missing.isNotEmpty()) {
            val computed = withContext(Dispatchers.Default) {
                missing.associateWith { groupMediaByTimelineScale(items, it) }
            }
            computed.forEach { (k, v) -> if (!groupingCache.containsKey(k)) groupingCache[k] = v }
        }
    }

    // ---- Transição FLIP lida apenas na fase de desenho (zero recomposições por frame) ----
    val flipTracker = remember { GridFlipTracker() }
    flipTracker.onScaleSync(timelineScale)
    val generation = flipTracker.generation
    val flipAnim = remember(generation) { Animatable(if (generation == 0) 1f else 0f) }
    LaunchedEffect(generation) {
        if (generation > 0) {
            flipAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = 300f
                )
            )
        }
    }
    val flipProgressProvider: () -> Float = remember(flipAnim) {
        { flipAnim.value.coerceIn(0f, 1f) }
    }

    // ---- Pinça: valor visual lido apenas na fase de desenho ----
    val pinchVisual = remember { mutableFloatStateOf(1f) }
    val pinchScaleProvider: () -> Float = remember(pinchVisual) {
        { pinchVisual.floatValue }
    }
    val releaseJob = remember { arrayOfNulls<Job>(1) }
    val currentScale by rememberUpdatedState(timelineScale)
    val currentOnScaleChange by rememberUpdatedState(onTimelineScaleChange)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var accum = 1f
                    try {
                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.count { it.pressed } >= 2) {
                                val zoom = event.calculateZoom()
                                if (zoom != 1f) {
                                    releaseJob[0]?.cancel()
                                    accum = (accum * zoom).coerceIn(0.65f, 1.45f)
                                    val s = currentScale
                                    if (accum > 1.20f && s.ordinal > 0) {
                                        currentOnScaleChange(TimelineScale.entries[s.ordinal - 1])
                                        accum = 1f
                                    } else if (accum < 0.82f && s.ordinal < TimelineScale.entries.lastIndex) {
                                        currentOnScaleChange(TimelineScale.entries[s.ordinal + 1])
                                        accum = 1f
                                    }
                                    pinchVisual.floatValue = (1f + (accum - 1f) * 0.14f).coerceIn(0.90f, 1.10f)
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    } finally {
                        val from = pinchVisual.floatValue
                        if (from != 1f) {
                            releaseJob[0] = scope.launch {
                                animate(
                                    initialValue = from,
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = 0.8f,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) { v, _ -> pinchVisual.floatValue = v }
                            }
                        }
                    }
                }
            }
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(timelineScale.columns),
            contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 4.dp, bottom = 112.dp),
            horizontalArrangement = Arrangement.spacedBy(timelineScale.spacingDp.dp),
            verticalArrangement = Arrangement.spacedBy(timelineScale.spacingDp.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            grouped.forEach { (dateLabel, groupItems) ->
                item(
                    span = { GridItemSpan(timelineScale.columns) },
                    key = "header_${timelineScale.name}_$dateLabel",
                    contentType = "header"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(0f)
                            .graphicsLayer {
                                val p = flipProgressProvider()
                                alpha = p
                                translationY = (1f - p) * (if (flipTracker.isZoomingOut) 18f else -18f)
                            }
                            .padding(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dateLabel,
                            color = colors.textPrimary,
                            fontSize = if (timelineScale == TimelineScale.YEAR || timelineScale == TimelineScale.MONTH) 18.sp else 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (selectionMode) {
                            val allGroupSelected = groupItems.all { selectedSet.contains(it.id) }
                            Text(
                                text = if (allGroupSelected) "Desmarcar" else "Selecionar",
                                color = colors.primaryBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    if (allGroupSelected) {
                                        groupItems.forEach { if (selectedSet.contains(it.id)) onToggleSelect(it.id) }
                                    } else {
                                        groupItems.forEach { if (!selectedSet.contains(it.id)) onToggleSelect(it.id) }
                                    }
                                }
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.surfaceCard
                            ) {
                                Text(
                                    text = "${groupItems.size}",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                items(
                    items = groupItems,
                    key = { it.id },
                    contentType = { "photo" }
                ) { media ->
                    OneUIGridThumbnail(
                        media = media,
                        cornerDp = timelineScale.cornerDp,
                        isSelected = selectedSet.contains(media.id),
                        selectionMode = selectionMode,
                        flipTracker = flipTracker,
                        flipProgress = flipProgressProvider,
                        pinchScale = pinchScaleProvider,
                        thumbnailBounds = thumbnailBounds,
                        onClick = {
                            if (selectionMode) onToggleSelect(media.id)
                            else onItemClick(indexById[media.id] ?: 0)
                        },
                        onLongClick = { onEnterSelection(media.id) },
                        modifier = Modifier.zIndex(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OneUIGridThumbnail(
    media: MediaItem,
    cornerDp: Int,
    isSelected: Boolean,
    selectionMode: Boolean,
    flipTracker: GridFlipTracker,
    flipProgress: () -> Float,
    pinchScale: () -> Float,
    thumbnailBounds: MutableMap<Long, Rect>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalOneUIColors.current
    val slotRectHolder = remember(media.id) { SlotRectHolder() }
    DisposableEffect(media.id, thumbnailBounds) {
        onDispose {
            thumbnailBounds.remove(media.id)
        }
    }
    // Request estável: evita realocar/reiniciar o carregamento a cada recomposição
    val request = remember(media.uri) {
        ImageRequest.Builder(context)
            .data(media.uri)
            .size(320)
            .crossfade(true)
            .build()
    }

    val shrinkScale by animateFloatAsState(
        targetValue = if (selectionMode && isSelected) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "oneui_select_scale"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (selectionMode && isSelected) 8.dp else cornerDp.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "oneui_select_corner"
    )
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    val rect = Rect(coords.positionInRoot(), coords.size.toSize())
                    slotRectHolder.rect = rect
                    thumbnailBounds[media.id] = rect
                    flipTracker.currentBounds[media.id] = rect
                    if (flipProgress() >= 0.999f) {
                        flipTracker.visualBounds[media.id] = rect
                    }
                }
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = flipProgress()
                    val inv = 1f - p
                    val livePinch = pinchScale()
                    val newRect = slotRectHolder.rect
                    val oldRect = flipTracker.previousBounds[media.id]

                    if (newRect != null && newRect.width > 0f && newRect.height > 0f && inv > 0.001f) {
                        if (oldRect != null) {
                            val dx = (oldRect.center.x - newRect.center.x) * inv
                            val dy = (oldRect.center.y - newRect.center.y) * inv
                            val sx = 1f + ((oldRect.width / newRect.width) - 1f) * inv
                            val sy = 1f + ((oldRect.height / newRect.height) - 1f) * inv
                            translationX = dx
                            translationY = dy
                            scaleX = shrinkScale * livePinch * sx
                            scaleY = shrinkScale * livePinch * sy
                            alpha = 1f

                            val curW = newRect.width * sx
                            val curH = newRect.height * sy
                            val curCx = newRect.center.x + dx
                            val curCy = newRect.center.y + dy
                            flipTracker.visualBounds[media.id] = Rect(
                                offset = Offset(curCx - curW / 2f, curCy - curH / 2f),
                                size = Size(curW, curH)
                            )
                        } else {
                            val dy = (if (flipTracker.isZoomingOut) 160f else -100f) * inv
                            val s = 1f + ((if (flipTracker.isZoomingOut) 1.22f else 0.82f) - 1f) * inv
                            translationX = 0f
                            translationY = dy
                            scaleX = shrinkScale * livePinch * s
                            scaleY = shrinkScale * livePinch * s
                            alpha = (0.2f + 0.8f * p).coerceIn(0f, 1f)
                        }
                    } else {
                        translationX = 0f
                        translationY = 0f
                        scaleX = shrinkScale * livePinch
                        scaleY = shrinkScale * livePinch
                        alpha = 1f
                        if (newRect != null) {
                            flipTracker.visualBounds[media.id] = newRect
                        }
                    }
                }
                .clip(shape)
                .background(colors.surfaceCard)
                .then(
                    if (cornerDp > 0 || (selectionMode && isSelected)) {
                        Modifier.border(0.4.dp, colors.floatingBorder.copy(alpha = 0.35f), shape)
                    } else Modifier
                )
        ) {
            AsyncImage(
                model = request,
                contentDescription = media.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (media.isFavorite) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(5.dp)
                        .size(13.dp)
                )
            }

            if (media.isVideo) {
                Surface(
                    color = Color(0x99000000),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(2.dp).size(12.dp)
                    )
                }
            }

            if (selectionMode && isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.selectionScrim)
                )
            }

            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(21.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.primaryBlue else Color(0x55000000))
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumsTabContent(
    albums: List<AlbumItem>,
    activeItems: List<MediaItem>,
    privateCount: Int,
    trashedCount: Int,
    onAlbumClick: (AlbumItem) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenVideos: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenPrivate: () -> Unit,
    onOpenTrash: () -> Unit,
    onGenerateSamples: () -> Unit,
    isGenerating: Boolean
) {
    val context = LocalContext.current
    val colors = LocalOneUIColors.current

    val recentItems = remember(activeItems) { activeItems.take(30) }
    val favItems = remember(activeItems) { activeItems.filter { it.isFavorite } }
    val videoItems = remember(activeItems) { activeItems.filter { it.isVideo } }

    if (albums.isEmpty() && activeItems.isEmpty()) {
        EmptyStateCard(
            icon = Icons.Outlined.Folder,
            title = "Nenhum álbum encontrado",
            subtitle = "Gere fotos de demonstração para visualizar álbuns organizados como Câmera, Viagem e Screenshots.",
            actionLabel = if (isGenerating) "Gerando..." else "Gerar 12 Fotos de Demonstração",
            onAction = onGenerateSamples
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (activeItems.isNotEmpty()) {
                item(
                    span = { GridItemSpan(3) },
                    key = "albums_top_cards"
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            OneUI8StackedCard(
                                title = "Recentes",
                                count = recentItems.size,
                                coverUri = recentItems.firstOrNull()?.uri,
                                icon = Icons.Outlined.History,
                                accent = colors.primaryBlue,
                                onClick = onOpenRecent
                            )
                        }
                        item {
                            OneUI8StackedCard(
                                title = "Favoritos",
                                count = favItems.size,
                                coverUri = favItems.firstOrNull()?.uri,
                                icon = Icons.Outlined.FavoriteBorder,
                                accent = colors.galleryRose,
                                onClick = onOpenFavorites
                            )
                        }
                        item {
                            OneUI8StackedCard(
                                title = "Vídeos",
                                count = videoItems.size,
                                coverUri = videoItems.firstOrNull()?.uri,
                                icon = Icons.Outlined.VideoLibrary,
                                accent = Color(0xFF34C759),
                                onClick = onOpenVideos
                            )
                        }
                        item {
                            OneUI8StackedCard(
                                title = "Privado",
                                count = privateCount,
                                coverUri = null,
                                icon = Icons.Rounded.Lock,
                                accent = colors.primaryBlue,
                                onClick = onOpenPrivate
                            )
                        }
                        item {
                            OneUI8StackedCard(
                                title = "Lixeira",
                                count = trashedCount,
                                coverUri = null,
                                icon = Icons.Outlined.Delete,
                                accent = colors.textSecondary,
                                onClick = onOpenTrash
                            )
                        }
                    }
                }
            }

            items(albums, key = { it.id }) { album ->
                    val isCamera = album.name.contains("Camera", ignoreCase = true) ||
                        album.name.contains("Câmera", ignoreCase = true)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAlbumClick(album) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(OneUIShapeMedium)
                                .background(colors.surfaceCard)
                                .border(0.5.dp, colors.divider, OneUIShapeMedium)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(album.coverUri)
                                    .size(380)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = album.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isCamera) {
                                Surface(
                                    color = Color(0x77000000),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                        .size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.CameraAlt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = album.name,
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                        Text(
                            text = "${album.items.size}",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
}

/**
 * One UI 8 Stacked Card: subtle stacked depth layers behind a rounded squircle cover.
 */
@Composable
private fun OneUI8StackedCard(
    title: String,
    count: Int,
    coverUri: Any?,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOneUIColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(86.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .width(86.dp)
                .height(86.dp)
        ) {
            // Stacked back layer
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(68.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceElevated)
            )
            // Front squircle card
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxSize()
                    .clip(OneUIShapeMedium)
                    .background(colors.surfaceCard)
                    .border(0.6.dp, colors.divider, OneUIShapeMedium),
                contentAlignment = Alignment.Center
            ) {
                if (coverUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(coverUri)
                            .size(220)
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x55000000))
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (coverUri != null) Color.White else accent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "$count",
            color = colors.textSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun StoriesTabContent(
    stories: List<StoryItem>,
    onPlayStory: (StoryItem) -> Unit,
    onGenerateSamples: () -> Unit,
    isGenerating: Boolean
) {
    val context = LocalContext.current
    val colors = LocalOneUIColors.current

    if (stories.isEmpty()) {
        EmptyStateCard(
            icon = Icons.Outlined.AutoStories,
            title = "Sem histórias ainda",
            subtitle = "As histórias são geradas automaticamente a partir das suas fotos e favoritos.",
            actionLabel = if (isGenerating) "Gerando..." else "Gerar 12 Fotos de Demonstração",
            onAction = onGenerateSamples
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(stories, key = { it.id }) { story ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(OneUIShapeLarge)
                    .background(colors.surfaceCard)
                    .clickable { onPlayStory(story) }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(story.coverUri)
                        .size(720)
                        .crossfade(true)
                        .build(),
                    contentDescription = story.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0x44000000), Color(0xD9000000))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = story.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = story.subtitle,
                            color = Color(0xCCFFFFFF),
                            fontSize = 13.sp
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color(0x44FFFFFF),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Reproduzir história",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryPlayerOverlay(
    story: StoryItem,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var currentIndex by remember(story.id) { mutableIntStateOf(0) }
    var progress by remember(story.id, currentIndex) { mutableFloatStateOf(0f) }
    val kenBurnsScale = remember(story.id, currentIndex) { Animatable(1.0f) }

    LaunchedEffect(story.id, currentIndex) {
        kenBurnsScale.snapTo(1.0f)
        launch {
            kenBurnsScale.animateTo(
                targetValue = 1.10f,
                animationSpec = tween(durationMillis = 3500, easing = LinearEasing)
            )
        }
        progress = 0f
        val steps = 70
        for (i in 1..steps) {
            delay(50)
            progress = i / steps.toFloat()
        }
        if (currentIndex < story.items.lastIndex) {
            currentIndex++
        } else {
            onClose()
        }
    }

    val currentMedia = story.items[currentIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(currentIndex) {
                detectTapGestures { offset ->
                    if (offset.x < size.width * 0.35f) {
                        if (currentIndex > 0) currentIndex--
                    } else {
                        if (currentIndex < story.items.lastIndex) currentIndex++ else onClose()
                    }
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(currentMedia.uri)
                .crossfade(true)
                .build(),
            contentDescription = currentMedia.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = kenBurnsScale.value
                    scaleY = kenBurnsScale.value
                }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color(0xBB000000), Color.Transparent))
                )
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                story.items.forEachIndexed { idx, _ ->
                    val barProgress = when {
                        idx < currentIndex -> 1f
                        idx == currentIndex -> progress
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { barProgress },
                        color = Color.White,
                        trackColor = Color(0x55FFFFFF),
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = story.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${currentIndex + 1} de ${story.items.size}",
                        color = Color(0xCCFFFFFF),
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = "Fechar", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun MenuTabContent(
    favoritesCount: Int,
    videosCount: Int,
    recentCount: Int,
    privateCount: Int,
    trashCount: Int,
    isDarkMode: Boolean,
    isGenerating: Boolean,
    onOpenFavorites: () -> Unit,
    onOpenVideos: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenPrivate: () -> Unit,
    onChangePrivatePin: () -> Unit,
    onOpenTrash: () -> Unit,
    onGenerateSamples: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit
) {
    val colors = LocalOneUIColors.current

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            OneUISectionCard {
                Column {
                    OneUIMenuRow(
                        icon = Icons.Outlined.FavoriteBorder,
                        iconTint = colors.galleryRose,
                        title = "Favoritos",
                        trailingValue = "$favoritesCount",
                        onClick = onOpenFavorites
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 20.dp))
                    OneUIMenuRow(
                        icon = Icons.Outlined.VideoLibrary,
                        iconTint = colors.primaryBlue,
                        title = "Vídeos",
                        trailingValue = "$videosCount",
                        onClick = onOpenVideos
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 20.dp))
                    OneUIMenuRow(
                        icon = Icons.Outlined.History,
                        iconTint = Color(0xFF34C759),
                        title = "Recentes",
                        trailingValue = "$recentCount",
                        onClick = onOpenRecent
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 20.dp))
                    OneUIMenuRow(
                        icon = Icons.Rounded.Lock,
                        iconTint = colors.primaryBlue,
                        title = "Álbum Privado (PIN)",
                        trailingValue = "$privateCount",
                        onClick = onOpenPrivate
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 20.dp))
                    OneUIMenuRow(
                        icon = Icons.Outlined.Lock,
                        iconTint = colors.textSecondary,
                        title = "Alterar PIN do Álbum Privado",
                        trailingValue = "••••",
                        onClick = onChangePrivatePin
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 20.dp))
                    OneUIMenuRow(
                        icon = Icons.Outlined.Delete,
                        iconTint = colors.textSecondary,
                        title = "Lixeira",
                        trailingValue = "$trashCount",
                        onClick = onOpenTrash
                    )
                }
            }
        }

        item {
            OneUISectionCard {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleDarkMode(!isDarkMode) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DarkMode,
                            contentDescription = null,
                            tint = colors.primaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Modo Escuro One UI",
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isDarkMode) "AMOLED Preto ativado" else "Tema Claro ativado",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = onToggleDarkMode,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.primaryBlue
                            )
                        )
                    }
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 20.dp))
                    OneUIMenuRow(
                        icon = Icons.Rounded.AutoAwesome,
                        iconTint = Color(0xFFFF9F0A),
                        title = if (isGenerating) "Gerando fotos de exemplo..." else "Gerar Fotos de Exemplo (Emulador)",
                        trailingValue = "+12",
                        onClick = onGenerateSamples
                    )
                }
            }
        }

        item {
            OneUISectionCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Galeria One UI",
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Design moderno inspirado na Samsung One UI com pinça suave (Dia, Semana, Mês, Ano), Álbum Privado protegido por PIN, Estúdio de Colagens e Editor completo.",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OneUIMenuRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    trailingValue: String,
    onClick: () -> Unit
) {
    val colors = LocalOneUIColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = trailingValue,
            color = colors.textSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Samsung One UI 7 / 8 Floating Capsule Bottom Navigation Bar:
 * Rounded floating pill container with [Fotos | Álbuns | Histórias] + [🔍] + [☰]
 */
@Composable
private fun OneUIFloatingCapsuleBottomBar(
    currentTab: GalleryTab,
    isSearchOpen: Boolean,
    onSelectTab: (GalleryTab) -> Unit,
    onToggleSearch: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val mainTabs = listOf(GalleryTab.PICTURES, GalleryTab.ALBUMS, GalleryTab.STORIES)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(34.dp),
            color = colors.floatingPill,
            shadowElevation = 10.dp,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(0.7.dp, colors.floatingBorder, RoundedCornerShape(34.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    mainTabs.forEach { tab ->
                        val selected = tab == currentTab
                        val pillBg by animateColorAsState(
                            targetValue = if (selected) colors.surfaceElevated else Color.Transparent,
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                            label = "tab_pill_bg"
                        )
                        val tabTextColor by animateColorAsState(
                            targetValue = if (selected) colors.textPrimary else colors.textSecondary,
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                            label = "tab_text_color"
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(pillBg)
                                .clickable { onSelectTab(tab) }
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = tab.label,
                                color = tabTextColor,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSearchOpen) colors.surfaceElevated else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Pesquisar",
                        tint = if (isSearchOpen) colors.primaryBlue else colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { onSelectTab(GalleryTab.MENU) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (currentTab == GalleryTab.MENU) colors.surfaceElevated else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "Menu",
                        tint = if (currentTab == GalleryTab.MENU) colors.primaryBlue else colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionBottomBar(
    selectedCount: Int,
    canCollage: Boolean,
    onShare: () -> Unit,
    onFavorite: () -> Unit,
    onCollage: () -> Unit,
    onPrivate: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalOneUIColors.current
    Surface(
        color = colors.bottomBar,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(62.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionActionButton(
                    icon = Icons.Outlined.Share,
                    label = "Enviar",
                    enabled = selectedCount > 0,
                    tint = colors.textPrimary,
                    onClick = onShare
                )
                SelectionActionButton(
                    icon = Icons.Outlined.FavoriteBorder,
                    label = "Favoritar",
                    enabled = selectedCount > 0,
                    tint = colors.galleryRose,
                    onClick = onFavorite
                )
                SelectionActionButton(
                    icon = Icons.Rounded.Dashboard,
                    label = "Colagem",
                    enabled = canCollage,
                    tint = colors.primaryBlue,
                    onClick = onCollage
                )
                SelectionActionButton(
                    icon = Icons.Rounded.Lock,
                    label = "Privado",
                    enabled = selectedCount > 0,
                    tint = colors.primaryBlue,
                    onClick = onPrivate
                )
                SelectionActionButton(
                    icon = Icons.Outlined.Delete,
                    label = "Excluir",
                    enabled = selectedCount > 0,
                    tint = colors.textPrimary,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun SelectionActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    tint: Color,
    onClick: () -> Unit
) {
    val colors = LocalOneUIColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) tint else colors.textSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (enabled) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    val colors = LocalOneUIColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        OneUISectionCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(28.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.primaryBlue.copy(alpha = 0.14f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = colors.primaryBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                if (actionLabel != null && onAction != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onAction,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlue),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = actionLabel,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionOnboardingScreen(
    onRequest: () -> Unit
) {
    val colors = LocalOneUIColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        OneUISectionCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(28.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = colors.galleryRose,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Collections,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Galeria One UI",
                    color = colors.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Para exibir suas fotos, vídeos e álbuns no estilo Samsung One UI, permita o acesso à mídia do dispositivo.",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRequest,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlue),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = "Permitir acesso às fotos",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
