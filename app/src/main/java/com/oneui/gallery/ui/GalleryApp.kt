package com.oneui.gallery.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.oneui.gallery.ui.theme.OneUIShapeGridDense
import com.oneui.gallery.ui.theme.OneUIShapeLarge
import com.oneui.gallery.ui.theme.OneUIShapeMedium
import com.oneui.gallery.ui.theme.OneUIShapeSmall
import com.oneui.gallery.ui.theme.OneUITheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class GalleryTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    PICTURES("Imagens", Icons.Rounded.Collections, Icons.Outlined.Collections),
    ALBUMS("Álbuns", Icons.Rounded.Folder, Icons.Outlined.Folder),
    STORIES("Histórias", Icons.Rounded.AutoStories, Icons.Outlined.AutoStories),
    MENU("Menu", Icons.Rounded.Menu, Icons.Outlined.Menu)
}

private enum class AlbumFilter(val label: String) {
    ALL("Todos"),
    CAMERA("Câmera"),
    FAVORITES("Favoritos"),
    VIDEOS("Vídeos")
}

private sealed interface CollectionRoute {
    data class Album(val album: AlbumItem) : CollectionRoute
    data object Favorites : CollectionRoute
    data object Videos : CollectionRoute
    data object Recent : CollectionRoute
    data object Trash : CollectionRoute
}

@Composable
fun GalleryApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val systemDark = isSystemInDarkTheme()
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
    var columnCount by remember { mutableIntStateOf(4) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var albumFilter by remember { mutableStateOf(AlbumFilter.ALL) }

    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    var collectionRoute by remember { mutableStateOf<CollectionRoute?>(null) }
    var viewerRequest by remember { mutableStateOf<Triple<List<MediaItem>, Int, Boolean>?>(null) }
    var editingItem by remember { mutableStateOf<MediaItem?>(null) }
    var activeStory by remember { mutableStateOf<StoryItem?>(null) }

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

        val activeItems = remember(allMedia) { allMedia.filter { !it.isTrashed } }
        val trashedItems = remember(allMedia) { allMedia.filter { it.isTrashed } }
        val albums = remember(activeItems) { MediaRepository.buildAlbums(activeItems) }
        val stories = remember(activeItems) { MediaRepository.buildStories(activeItems) }

        val displayedPictures = remember(activeItems, searchQuery) {
            if (searchQuery.isBlank()) activeItems
            else activeItems.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.bucketName.contains(searchQuery, ignoreCase = true)
            }
        }

        // Back button hierarchy
        BackHandler(enabled = editingItem != null || viewerRequest != null || activeStory != null || collectionRoute != null || selectionMode || showSearch) {
            when {
                editingItem != null -> editingItem = null
                viewerRequest != null -> viewerRequest = null
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

        // Full-Screen Editor
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

        // Full-Screen Story Player
        val currentStory = activeStory
        if (currentStory != null) {
            StoryPlayerOverlay(
                story = currentStory,
                onClose = { activeStory = null }
            )
            return@OneUITheme
        }

        // Full-Screen Viewer
        val currentViewer = viewerRequest
        if (currentViewer != null) {
            val (viewerList, initialIdx, isTrash) = currentViewer
            // Keep viewer items synced with allMedia state (e.g. favorite status)
            val syncedList = remember(viewerList, allMedia) {
                val map = allMedia.associateBy { it.id }
                viewerList.mapNotNull { map[it.id] }.filter { it.isTrashed == isTrash }
            }
            if (syncedList.isEmpty()) {
                viewerRequest = null
            } else {
                ViewerScreen(
                    items = syncedList,
                    initialIndex = initialIdx.coerceIn(0, syncedList.lastIndex),
                    isTrashMode = isTrash,
                    onBack = { viewerRequest = null },
                    onToggleFavorite = { item ->
                        MediaRepository.toggleFavorite(context, item.id)
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
            return@OneUITheme
        }

        // Sub-collection screen (Album detail, Favorites, Videos, Recent, Trash)
        val route = collectionRoute
        if (route != null) {
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
                            Text("Restaurar", color = colors.primaryBlue, fontSize = 13.sp)
                        }
                        TextButton(onClick = {
                            scope.launch {
                                MediaRepository.deletePermanently(context, routeItems)
                                refreshMedia()
                                Toast.makeText(context, "Lixeira esvaziada", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Esvaziar", color = colors.galleryRose, fontSize = 13.sp)
                        }
                    } else if (routeItems.isNotEmpty()) {
                        ColumnZoomButtons(
                            columnCount = columnCount,
                            onChange = { columnCount = it }
                        )
                    }
                }
            ) {
                if (routeItems.isEmpty()) {
                    EmptyStateCard(
                        icon = if (isTrashRoute) Icons.Outlined.DeleteSweep else Icons.Outlined.PhotoLibrary,
                        title = if (isTrashRoute) "Lixeira vazia" else "Nenhum item em $routeTitle",
                        subtitle = if (isTrashRoute) "Itens excluídos ficam aqui antes da remoção permanente." else "As mídias correspondentes aparecerão aqui.",
                        actionLabel = null,
                        onAction = null
                    )
                } else {
                    MediaGridContent(
                        items = routeItems,
                        columnCount = columnCount,
                        onColumnCountChange = { columnCount = it },
                        selectionMode = selectionMode,
                        selectedIds = selectedIds,
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
            return@OneUITheme
        }

        // Main 4-Tab One UI Gallery Scaffold
        val tabTitle = when {
            selectionMode -> "${selectedIds.size} selecionado(s)"
            else -> currentTab.label
        }
        val tabSubtitle = when (currentTab) {
            GalleryTab.PICTURES -> "${displayedPictures.size} ${if (displayedPictures.size == 1) "item" else "itens"}"
            GalleryTab.ALBUMS -> "${albums.size} ${if (albums.size == 1) "álbum" else "álbuns"}"
            GalleryTab.STORIES -> "${stories.size} ${if (stories.size == 1) "história" else "histórias"}"
            GalleryTab.MENU -> "Samsung One UI Design"
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
                        Icon(Icons.Outlined.Close, contentDescription = "Sair da seleção", tint = colors.textPrimary)
                    }
                }
            } else null,
            actions = {
                if (currentTab == GalleryTab.PICTURES && !selectionMode) {
                    IconButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) searchQuery = ""
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Pesquisar",
                            tint = if (showSearch) colors.primaryBlue else colors.textPrimary
                        )
                    }
                    ColumnZoomButtons(
                        columnCount = columnCount,
                        onChange = { columnCount = it }
                    )
                    if (displayedPictures.isNotEmpty()) {
                        IconButton(onClick = {
                            selectionMode = true
                            selectedIds.clear()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Selecionar",
                                tint = colors.textPrimary
                            )
                        }
                    }
                } else if (selectionMode) {
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
                }
            },
            headerExtra = {
                AnimatedVisibility(visible = showSearch && currentTab == GalleryTab.PICTURES) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar por nome ou álbum...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Limpar")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
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
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            },
            bottomBar = {
                if (selectionMode) {
                    SelectionBottomBar(
                        selectedCount = selectedIds.size,
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
                        onDelete = {
                            MediaRepository.moveToTrash(context, selectedIds.toList())
                            selectionMode = false
                            selectedIds.clear()
                            refreshMedia()
                            Toast.makeText(context, "Movido para a Lixeira", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    OneUIBottomNavigation(
                        currentTab = currentTab,
                        onSelectTab = { currentTab = it }
                    )
                }
            }
        ) {
            when (currentTab) {
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
                                columnCount = columnCount,
                                onColumnCountChange = { columnCount = it },
                                selectionMode = selectionMode,
                                selectedIds = selectedIds,
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
                        selectedFilter = albumFilter,
                        onSelectFilter = { albumFilter = it },
                        onAlbumClick = { album -> collectionRoute = CollectionRoute.Album(album) },
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
                        trashCount = trashedItems.size,
                        isDarkMode = isDarkMode,
                        isGenerating = isGeneratingSamples,
                        onOpenFavorites = { collectionRoute = CollectionRoute.Favorites },
                        onOpenVideos = { collectionRoute = CollectionRoute.Videos },
                        onOpenRecent = { collectionRoute = CollectionRoute.Recent },
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
}

@Composable
private fun ColumnZoomButtons(
    columnCount: Int,
    onChange: (Int) -> Unit
) {
    val colors = LocalOneUIColors.current
    IconButton(
        onClick = { if (columnCount > 2) onChange(columnCount - 1) },
        enabled = columnCount > 2
    ) {
        Icon(
            imageVector = Icons.Outlined.ZoomIn,
            contentDescription = "Aumentar miniaturas",
            tint = if (columnCount > 2) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f)
        )
    }
    IconButton(
        onClick = { if (columnCount < 6) onChange(columnCount + 1) },
        enabled = columnCount < 6
    ) {
        Icon(
            imageVector = Icons.Outlined.ZoomOut,
            contentDescription = "Diminuir miniaturas",
            tint = if (columnCount < 6) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGridContent(
    items: List<MediaItem>,
    columnCount: Int,
    onColumnCountChange: (Int) -> Unit,
    selectionMode: Boolean,
    selectedIds: List<Long>,
    onToggleSelect: (Long) -> Unit,
    onEnterSelection: (Long) -> Unit,
    onItemClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOneUIColors.current
    val grouped = remember(items) { items.groupBy { it.dateGroupLabel } }
    val indexById = remember(items) {
        items.mapIndexed { index, item -> item.id to index }.toMap()
    }
    var pinchAccumulator by remember { mutableFloatStateOf(1f) }
    val thumbShape = if (columnCount <= 3) OneUIShapeSmall else OneUIShapeGridDense

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        contentPadding = PaddingValues(horizontal = 3.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalArrangement = Arrangement.spacedBy(2.5.dp),
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(columnCount) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f) {
                        pinchAccumulator *= zoom
                        if (pinchAccumulator > 1.24f && columnCount > 2) {
                            onColumnCountChange(columnCount - 1)
                            pinchAccumulator = 1f
                        } else if (pinchAccumulator < 0.78f && columnCount < 6) {
                            onColumnCountChange(columnCount + 1)
                            pinchAccumulator = 1f
                        }
                    }
                }
            }
    ) {
        grouped.forEach { (dateLabel, groupItems) ->
            item(span = { GridItemSpan(columnCount) }, key = "header_$dateLabel") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 13.dp, end = 13.dp, top = 14.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dateLabel,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (selectionMode) {
                        val allGroupSelected = groupItems.all { selectedIds.contains(it.id) }
                        Text(
                            text = if (allGroupSelected) "Desmarcar" else "Marcar dia",
                            color = colors.primaryBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                if (allGroupSelected) {
                                    groupItems.forEach { if (selectedIds.contains(it.id)) onToggleSelect(it.id) }
                                } else {
                                    groupItems.forEach { if (!selectedIds.contains(it.id)) onToggleSelect(it.id) }
                                }
                            }
                        )
                    }
                }
            }

            items(groupItems, key = { it.id }) { media ->
                val isSelected = selectedIds.contains(media.id)
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(thumbShape)
                        .background(colors.surfaceCard)
                        .combinedClickable(
                            onClick = {
                                if (selectionMode) {
                                    onToggleSelect(media.id)
                                } else {
                                    val idx = indexById[media.id] ?: 0
                                    onItemClick(idx)
                                }
                            },
                            onLongClick = {
                                onEnterSelection(media.id)
                            }
                        )
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(media.uri)
                            .size(320)
                            .crossfade(true)
                            .build(),
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
                                .size(14.dp)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    if (selectionMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isSelected) colors.selectionScrim else Color.Transparent)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.primaryBlue else Color(0x66000000))
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
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
    selectedFilter: AlbumFilter,
    onSelectFilter: (AlbumFilter) -> Unit,
    onAlbumClick: (AlbumItem) -> Unit,
    onGenerateSamples: () -> Unit,
    isGenerating: Boolean
) {
    val context = LocalContext.current
    val colors = LocalOneUIColors.current

    val filteredAlbums = remember(albums, activeItems, selectedFilter) {
        when (selectedFilter) {
            AlbumFilter.ALL -> albums
            AlbumFilter.CAMERA -> albums.filter {
                it.name.contains("Camera", ignoreCase = true) ||
                    it.name.contains("Câmera", ignoreCase = true)
            }
            AlbumFilter.FAVORITES -> {
                val favs = activeItems.filter { it.isFavorite }
                if (favs.isEmpty()) emptyList()
                else listOf(
                    AlbumItem(
                        id = "fav_virtual",
                        name = "Favoritos",
                        coverUri = favs.first().uri,
                        items = favs,
                        videoCount = favs.count { it.isVideo }
                    )
                )
            }
            AlbumFilter.VIDEOS -> albums.filter { it.videoCount > 0 }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AlbumFilter.entries) { filter ->
                val selected = filter == selectedFilter
                FilterChip(
                    selected = selected,
                    onClick = { onSelectFilter(filter) },
                    label = {
                        Text(
                            text = filter.label,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primaryBlue,
                        selectedLabelColor = Color.White,
                        containerColor = colors.surfaceCard,
                        labelColor = colors.textPrimary
                    ),
                    border = null
                )
            }
        }

        if (filteredAlbums.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Outlined.Folder,
                title = "Nenhum álbum encontrado",
                subtitle = if (albums.isEmpty()) {
                    "Gere fotos de demonstração para visualizar álbuns organizados como Câmera, Viagem e Screenshots."
                } else {
                    "Nenhum álbum corresponde ao filtro selecionado."
                },
                actionLabel = if (albums.isEmpty()) {
                    if (isGenerating) "Gerando..." else "Gerar 12 Fotos de Demonstração"
                } else null,
                onAction = if (albums.isEmpty()) onGenerateSamples else null
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredAlbums, key = { it.id }) { album ->
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
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(album.coverUri)
                                    .size(480)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = album.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = album.name,
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Text(
                            text = "${album.items.size} ${if (album.items.size == 1) "item" else "itens"}",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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

    LaunchedEffect(story.id, currentIndex) {
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
            modifier = Modifier.fillMaxSize()
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
    trashCount: Int,
    isDarkMode: Boolean,
    isGenerating: Boolean,
    onOpenFavorites: () -> Unit,
    onOpenVideos: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenTrash: () -> Unit,
    onGenerateSamples: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit
) {
    val colors = LocalOneUIColors.current

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Section 1: Collections (One UI Grouped Card)
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
                        icon = Icons.Outlined.Delete,
                        iconTint = colors.textSecondary,
                        title = "Lixeira",
                        trailingValue = "$trashCount",
                        onClick = onOpenTrash
                    )
                }
            }
        }

        // Section 2: Tools & Theme
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

        // Section 3: App Info Card
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
                        text = "Inspirada nas diretrizes visuais da Samsung One UI para Moto G15 e BlueStacks. Suporte a grid pinch-to-zoom, álbuns, histórias, lixeira e editor completo de fotos.",
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

@Composable
private fun OneUIBottomNavigation(
    currentTab: GalleryTab,
    onSelectTab: (GalleryTab) -> Unit
) {
    val colors = LocalOneUIColors.current
    Surface(
        color = colors.bottomBar,
        tonalElevation = 0.dp,
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
                GalleryTab.entries.forEach { tab ->
                    val selected = tab == currentTab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectTab(tab) }
                            .padding(vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) colors.primaryBlue.copy(alpha = 0.14f) else Color.Transparent
                                )
                                .padding(horizontal = 14.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                                tint = if (selected) colors.primaryBlue else colors.textSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            color = if (selected) colors.primaryBlue else colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionBottomBar(
    selectedCount: Int,
    onShare: () -> Unit,
    onFavorite: () -> Unit,
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
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
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
                    icon = Icons.Outlined.Delete,
                    label = "Lixeira",
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
