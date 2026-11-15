package com.example

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.local.MediaItemEntity
import com.example.data.repository.MediaRepository
import com.example.ui.components.AddToPlaylistSheet
import com.example.ui.components.FloatingMiniVideoOverlay
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.OpenStreamDialog
import com.example.ui.navigation.AudioPlayerRoute
import com.example.ui.navigation.AudioRoute
import com.example.ui.navigation.EqualizerRoute
import com.example.ui.navigation.GestureControlsRoute
import com.example.ui.navigation.PlaylistDetailRoute
import com.example.ui.navigation.PlaylistsRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.navigation.VideoPlayerRoute
import com.example.ui.navigation.VideosRoute
import com.example.ui.screens.AudioPlayerScreen
import com.example.ui.screens.AudioScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.GestureControlsScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.theme.AetherPlayTheme
import com.example.ui.viewmodel.AudioFilterTab
import com.example.ui.viewmodel.EqualizerViewModel
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.PlayerViewModel
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow

private data class TopLevelDestination(
    val label: String,
    val route: Any,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {

    private val incomingUriFlow = MutableStateFlow<Uri?>(null)
    private var isInPipState by mutableStateOf(false)
    private var canAutoEnterPip = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        val appContainer = (application as AetherPlayApp).container

        setContent {
            val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(appContainer))
            val playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory(appContainer))
            val equalizerViewModel: EqualizerViewModel = viewModel(factory = EqualizerViewModel.factory(appContainer))
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(appContainer))

            val preferences by settingsViewModel.preferences.collectAsStateWithLifecycle()
            val incomingUri by incomingUriFlow.collectAsStateWithLifecycle()

            AetherPlayTheme(
                themeMode = preferences.themeMode,
                useDynamicColor = preferences.useDynamicColor
            ) {
                AetherPlayRoot(
                    libraryViewModel = libraryViewModel,
                    playerViewModel = playerViewModel,
                    equalizerViewModel = equalizerViewModel,
                    settingsViewModel = settingsViewModel,
                    incomingUri = incomingUri,
                    onConsumeIncomingUri = { incomingUriFlow.value = null },
                    isInPipMode = isInPipState,
                    onUpdateAutoPipEligibility = { eligible -> canAutoEnterPip = eligible },
                    onRequestPip = { aspectWidth, aspectHeight -> enterPip(aspectWidth, aspectHeight) }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as AetherPlayApp).container.playerControllerHolder.connect()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = runCatching { intent.data }.getOrNull() ?: return
            if (com.example.player.MediaSafety.isSafeExternalMediaUri(this, uri)) {
                incomingUriFlow.value = uri
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (canAutoEnterPip) {
            enterPip(16, 9)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipState = isInPictureInPictureMode
    }

    private fun enterPip(width: Int, height: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            val w = if (width > 0) width else 16
            val h = if (height > 0) height else 9
            val rational = Rational(w.coerceIn(1, 239), h.coerceIn(1, 239))
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(rational)
                .build()
            runCatching { enterPictureInPictureMode(params) }
        }
    }
}

@Composable
private fun AetherPlayRoot(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    equalizerViewModel: EqualizerViewModel,
    settingsViewModel: SettingsViewModel,
    incomingUri: Uri?,
    onConsumeIncomingUri: () -> Unit,
    isInPipMode: Boolean,
    onUpdateAutoPipEligibility: (Boolean) -> Unit,
    onRequestPip: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val controller by playerViewModel.controller.collectAsStateWithLifecycle()
    val currentMedia by playerViewModel.currentMediaEntity.collectAsStateWithLifecycle()
    val eqState by equalizerViewModel.uiState.collectAsStateWithLifecycle()
    val preferences by settingsViewModel.preferences.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showStreamDialog by remember { mutableStateOf(false) }
    var playlistTargetMedia by remember { mutableStateOf<MediaItemEntity?>(null) }
    var isFloatingVideoMode by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var hasStoragePermission by remember { mutableStateOf(MediaRepository.hasAllFilesPermission(context)) }
    var hasAutoScannedThisSession by remember { mutableStateOf(false) }
    var showManageFilesPrompt by remember {
        mutableStateOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasStoragePermission)
    }

    val legacyStoragePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStoragePermission = MediaRepository.hasAllFilesPermission(context)
        if (granted) {
            hasAutoScannedThisSession = true
            libraryViewModel.scanDeviceMedia(showSummaryBanner = true)
        }
    }

    fun requestManageAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appIntent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            runCatching { context.startActivity(appIntent) }
                .onFailure { runCatching { context.startActivity(fallbackIntent) } }
        } else {
            legacyStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && !hasStoragePermission) {
            legacyStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowGranted = MediaRepository.hasAllFilesPermission(context)
                val becameGranted = !hasStoragePermission && nowGranted
                hasStoragePermission = nowGranted
                if (nowGranted) {
                    showManageFilesPrompt = false
                    if (!hasAutoScannedThisSession || becameGranted) {
                        hasAutoScannedThisSession = true
                        libraryViewModel.scanDeviceMedia(showSummaryBanner = becameGranted)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(playbackState.isVideo, playbackState.hasMedia) {
        if (!playbackState.isVideo || !playbackState.hasMedia) {
            isFloatingVideoMode = false
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            libraryViewModel.importFiles(uris)
        }
    }

    val documentFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            libraryViewModel.importFiles(uris)
        }
    }

    val folderTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            libraryViewModel.scanFolderTree(treeUri)
        }
    }

    // Handle external ACTION_VIEW URI launches
    LaunchedEffect(incomingUri) {
        val uri = incomingUri ?: return@LaunchedEffect
        ensureNotificationPermission()
        libraryViewModel.openExternalUri(
            uri = uri,
            onOpenVideo = { navController.navigate(VideoPlayerRoute) { launchSingleTop = true } },
            onOpenAudio = { navController.navigate(AudioPlayerRoute) { launchSingleTop = true } }
        )
        onConsumeIncomingUri()
    }

    // Surface status banners & playback errors cleanly
    LaunchedEffect(libraryState.statusBanner) {
        val msg = libraryState.statusBanner ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        libraryViewModel.clearStatusBanner()
    }

    LaunchedEffect(playbackState.errorMessage) {
        val err = playbackState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(err)
        playerViewModel.clearError()
    }

    val isFullScreenPlayer = currentDest?.hasRoute<VideoPlayerRoute>() == true ||
        currentDest?.hasRoute<AudioPlayerRoute>() == true
    val isTopLevelTab = currentDest?.hasRoute<VideosRoute>() == true ||
        currentDest?.hasRoute<AudioRoute>() == true ||
        currentDest?.hasRoute<PlaylistsRoute>() == true ||
        currentDest?.hasRoute<SettingsRoute>() == true

    LaunchedEffect(currentDest, playbackState.isPlaying, playbackState.isVideo, preferences.autoPip) {
        val onVideoScreen = currentDest?.hasRoute<VideoPlayerRoute>() == true
        onUpdateAutoPipEligibility(onVideoScreen && playbackState.isPlaying && playbackState.isVideo && preferences.autoPip)
    }

    val destinations = remember {
        listOf(
            TopLevelDestination("Videos", VideosRoute, Icons.Rounded.Movie, Icons.Outlined.Movie, "nav_videos"),
            TopLevelDestination("Audio", AudioRoute, Icons.Rounded.Audiotrack, Icons.Outlined.Audiotrack, "nav_audio"),
            TopLevelDestination(
                "Playlists",
                PlaylistsRoute,
                Icons.AutoMirrored.Rounded.QueueMusic,
                Icons.AutoMirrored.Outlined.QueueMusic,
                "nav_playlists"
            ),
            TopLevelDestination("Settings", SettingsRoute, Icons.Rounded.Tune, Icons.Outlined.Tune, "nav_settings")
        )
    }

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceContainerLowest
        )
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        val useRail = maxWidth >= 600.dp && !isFullScreenPlayer

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail && isTopLevelTab) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    destinations.forEach { dest ->
                        val selected = currentDest?.hasRoute(dest.route::class) == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTopLevel(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) },
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!isFullScreenPlayer && !isInPipMode) {
                        val showBottomNav = !useRail && isTopLevelTab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (showBottomNav) Modifier else Modifier.navigationBarsPadding())
                        ) {
                            MiniPlayerBar(
                                playbackState = playbackState,
                                controller = if (isFloatingVideoMode) null else controller,
                                isVisible = !isFloatingVideoMode,
                                onOpenPlayer = {
                                    isFloatingVideoMode = false
                                    if (playbackState.isVideo) {
                                        navController.navigate(VideoPlayerRoute) { launchSingleTop = true }
                                    } else {
                                        navController.navigate(AudioPlayerRoute) { launchSingleTop = true }
                                    }
                                },
                                onToggleFloatingVideo = { isFloatingVideoMode = true },
                                onTogglePlayPause = { playerViewModel.togglePlayPause() },
                                onSkipNext = { playerViewModel.skipNext() },
                                onDismiss = {
                                    isFloatingVideoMode = false
                                    playerViewModel.stopAndClear()
                                }
                            )
                            if (showBottomNav) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.96f),
                                    windowInsets = NavigationBarDefaults.windowInsets
                                ) {
                                    destinations.forEach { dest ->
                                        val selected = currentDest?.hasRoute(dest.route::class) == true
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateTopLevel(dest.route) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = dest.label
                                                )
                                            },
                                            label = { Text(dest.label) },
                                            modifier = Modifier.testTag(dest.testTag)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = VideosRoute
                    ) {
                        composable<VideosRoute> {
                            VideosScreen(
                                uiState = libraryState,
                                isGridMode = preferences.videoGridMode,
                                isFolderView = preferences.videoFolderView,
                                hasStoragePermission = hasStoragePermission,
                                currentPlayingUri = playbackState.currentUri,
                                onSearchChange = libraryViewModel::setSearchQuery,
                                onSelectFolderKey = libraryViewModel::setSelectedVideoFolderKey,
                                onSelectSort = libraryViewModel::setSortOrder,
                                onToggleGridMode = libraryViewModel::toggleVideoGridMode,
                                onToggleFolderView = libraryViewModel::toggleVideoFolderView,
                                onRescanDevice = { libraryViewModel.scanDeviceMedia(showSummaryBanner = true) },
                                onRequestStoragePermission = { requestManageAllFilesPermission() },
                                onPlayVideo = { items, idx ->
                                    ensureNotificationPermission()
                                    libraryViewModel.playMediaQueue(items, idx)
                                    navController.navigate(VideoPlayerRoute) { launchSingleTop = true }
                                },
                                onToggleLike = libraryViewModel::toggleLike,
                                onAddToPlaylist = { playlistTargetMedia = it },
                                onDeleteVideo = { libraryViewModel.deleteMediaItem(it.uri) },
                                onPickPhotoPickerVideos = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                onPickDocumentFiles = {
                                    documentFilesLauncher.launch(arrayOf("video/*", "audio/*", "*/*"))
                                },
                                onPickFolderTree = { folderTreeLauncher.launch(null) },
                                onOpenStreamDialog = { showStreamDialog = true }
                            )
                        }

                        composable<AudioRoute> {
                            AudioScreen(
                                uiState = libraryState,
                                isFolderView = preferences.audioFolderView,
                                hasStoragePermission = hasStoragePermission,
                                currentPlayingUri = playbackState.currentUri,
                                onSearchChange = libraryViewModel::setSearchQuery,
                                onSelectFilter = libraryViewModel::setAudioFilter,
                                onSelectFolderKey = libraryViewModel::setSelectedAudioFolderKey,
                                onToggleFolderView = libraryViewModel::toggleAudioFolderView,
                                onRescanDevice = { libraryViewModel.scanDeviceMedia(showSummaryBanner = true) },
                                onRequestStoragePermission = { requestManageAllFilesPermission() },
                                onPlayAudio = { items, idx ->
                                    ensureNotificationPermission()
                                    libraryViewModel.playMediaQueue(items, idx)
                                    navController.navigate(AudioPlayerRoute) { launchSingleTop = true }
                                },
                                onShufflePlay = { items ->
                                    if (items.isNotEmpty()) {
                                        ensureNotificationPermission()
                                        val shuffled = items.shuffled()
                                        libraryViewModel.playMediaQueue(shuffled, 0)
                                        navController.navigate(AudioPlayerRoute) { launchSingleTop = true }
                                    }
                                },
                                onToggleLike = libraryViewModel::toggleLike,
                                onAddToPlaylist = { playlistTargetMedia = it },
                                onDeleteAudio = { libraryViewModel.deleteMediaItem(it.uri) },
                                onPickAudioFiles = {
                                    documentFilesLauncher.launch(arrayOf("audio/*", "*/*"))
                                },
                                onPickFolderTree = { folderTreeLauncher.launch(null) },
                                onOpenEqualizer = {
                                    navController.navigate(EqualizerRoute) { launchSingleTop = true }
                                }
                            )
                        }

                        composable<PlaylistsRoute> {
                            PlaylistsScreen(
                                uiState = libraryState,
                                onOpenPlaylist = { id ->
                                    navController.navigate(PlaylistDetailRoute(id))
                                },
                                onPlayLiked = { items, shuffle ->
                                    if (items.isNotEmpty()) {
                                        ensureNotificationPermission()
                                        val queue = if (shuffle) items.shuffled() else items
                                        libraryViewModel.playMediaQueue(queue, 0)
                                        if (queue.first().isVideo) {
                                            navController.navigate(VideoPlayerRoute) { launchSingleTop = true }
                                        } else {
                                            navController.navigate(AudioPlayerRoute) { launchSingleTop = true }
                                        }
                                    }
                                },
                                onPlayPlaylist = { pw, shuffle ->
                                    if (pw.items.isNotEmpty()) {
                                        ensureNotificationPermission()
                                        val queue = if (shuffle) pw.items.shuffled() else pw.items
                                        libraryViewModel.playMediaQueue(queue, 0)
                                        if (queue.first().isVideo) {
                                            navController.navigate(VideoPlayerRoute) { launchSingleTop = true }
                                        } else {
                                            navController.navigate(AudioPlayerRoute) { launchSingleTop = true }
                                        }
                                    }
                                },
                                onCreatePlaylist = libraryViewModel::createPlaylist,
                                onRenamePlaylist = libraryViewModel::renamePlaylist,
                                onDeletePlaylist = libraryViewModel::deletePlaylist
                            )
                        }

                        composable<PlaylistDetailRoute> { entry ->
                            val route = entry.toRoute<PlaylistDetailRoute>()
                            val playlistDetail by libraryViewModel.observePlaylist(route.playlistId)
                                .collectAsStateWithLifecycle()

                            PlaylistDetailScreen(
                                playlistWithItems = playlistDetail,
                                onBack = { navController.popBackStack() },
                                onPlayQueue = { items, idx ->
                                    ensureNotificationPermission()
                                    libraryViewModel.playMediaQueue(items, idx)
                                    val target = items[idx]
                                    if (target.isVideo) {
                                        navController.navigate(VideoPlayerRoute) { launchSingleTop = true }
                                    } else {
                                        navController.navigate(AudioPlayerRoute) { launchSingleTop = true }
                                    }
                                },
                                onShuffleQueue = { items ->
                                    if (items.isNotEmpty()) {
                                        ensureNotificationPermission()
                                        val shuffled = items.shuffled()
                                        libraryViewModel.playMediaQueue(shuffled, 0)
                                        if (shuffled.first().isVideo) {
                                            navController.navigate(VideoPlayerRoute) { launchSingleTop = true }
                                        } else {
                                            navController.navigate(AudioPlayerRoute) { launchSingleTop = true }
                                        }
                                    }
                                },
                                onRemoveItem = { mediaUri ->
                                    libraryViewModel.removeFromPlaylist(route.playlistId, mediaUri)
                                }
                            )
                        }

                        composable<SettingsRoute> {
                            SettingsScreen(
                                preferences = preferences,
                                onSetThemeMode = settingsViewModel::setThemeMode,
                                onSetDynamicColor = settingsViewModel::setDynamicColor,
                                onSetDecoderMode = settingsViewModel::setDecoderMode,
                                onSetUseSurfaceView = settingsViewModel::setUseSurfaceView,
                                onSetResumePlayback = settingsViewModel::setResumePlayback,
                                onSetBackgroundVideoAudio = settingsViewModel::setBackgroundVideoAudio,
                                onSetAutoPip = settingsViewModel::setAutoPip,
                                onSetRotateAudioArtwork = settingsViewModel::setRotateAudioArtwork,
                                onSetAudioSwipeDownAction = settingsViewModel::setAudioSwipeDownAction,
                                onSetAudioSwipeUpAction = settingsViewModel::setAudioSwipeUpAction,
                                onSetAudioDoubleTapAction = settingsViewModel::setAudioDoubleTapAction,
                                onUpdateSubtitlePrefs = settingsViewModel::updateSubtitlePreferences,
                                onOpenEqualizer = {
                                    navController.navigate(EqualizerRoute) { launchSingleTop = true }
                                },
                                onOpenGestureControls = {
                                    navController.navigate(GestureControlsRoute) { launchSingleTop = true }
                                }
                            )
                        }

                        composable<GestureControlsRoute> {
                            GestureControlsScreen(
                                preferences = preferences,
                                onBack = { navController.popBackStack() },
                                onSetBrightnessEnabled = settingsViewModel::setGestureBrightnessEnabled,
                                onSetVolumeEnabled = settingsViewModel::setGestureVolumeEnabled,
                                onSetBrightnessSensitivity = settingsViewModel::setBrightnessSensitivity,
                                onSetVolumeSensitivity = settingsViewModel::setVolumeSensitivity,
                                onSetSeekSensitivity = settingsViewModel::setSeekSensitivity,
                                onSetHorizontalSeek = settingsViewModel::setGestureHorizontalSeek,
                                onSetDoubleTapSeek = settingsViewModel::setDoubleTapSeek,
                                onSetDefaultSeekSeconds = settingsViewModel::setDefaultSeekSeconds,
                                onSetLongPress2x = settingsViewModel::setLongPress2x,
                                onResetDefaults = settingsViewModel::resetGestureDefaults
                            )
                        }

                        composable<VideoPlayerRoute> {
                            val subText = playerViewModel.resolveVisibleSubtitles(playbackState, preferences.subtitle)
                            VideoPlayerScreen(
                                controller = controller,
                                playbackState = playbackState,
                                preferences = preferences,
                                subtitleText = subText,
                                isInPipMode = isInPipMode,
                                onBack = { navController.popBackStack() },
                                onEnterPip = {
                                    onRequestPip(playbackState.videoWidth, playbackState.videoHeight)
                                },
                                onSwitchToAudioOnly = {
                                    navController.navigate(AudioPlayerRoute) {
                                        popUpTo<VideoPlayerRoute> { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onOpenEqualizer = {
                                    navController.navigate(EqualizerRoute) { launchSingleTop = true }
                                },
                                onOpenGestureControls = {
                                    navController.navigate(GestureControlsRoute) { launchSingleTop = true }
                                },
                                onTogglePlayPause = playerViewModel::togglePlayPause,
                                onSeekTo = playerViewModel::seekTo,
                                onSeekBy = playerViewModel::seekBy,
                                onSkipNext = playerViewModel::skipNext,
                                onSkipPrevious = playerViewModel::skipPrevious,
                                onSetSpeed = playerViewModel::setSpeed,
                                onSelectAudioTrack = playerViewModel::selectAudioTrack,
                                onSelectSubtitleTrack = playerViewModel::selectSubtitleTrack,
                                onAttachExternalSubtitle = playerViewModel::attachExternalSubtitle,
                                onUpdateSubtitlePrefs = playerViewModel::updateSubtitlePreferences,
                                onSetSleepTimer = playerViewModel::setSleepTimer,
                                onToggleAbLoop = playerViewModel::toggleAbLoop
                            )
                        }

                        composable<AudioPlayerRoute> {
                            val subText = playerViewModel.resolveVisibleSubtitles(playbackState, preferences.subtitle)
                            val likedAudioSongs = remember(libraryState.likedItems) {
                                libraryState.likedItems.filter { !it.isVideo }
                            }
                            AudioPlayerScreen(
                                playbackState = playbackState,
                                currentMedia = currentMedia,
                                preferences = preferences,
                                subtitleText = subText,
                                likedSongs = likedAudioSongs,
                                onBack = { navController.popBackStack() },
                                onOpenEqualizer = {
                                    navController.navigate(EqualizerRoute) { launchSingleTop = true }
                                },
                                onOpenLikedSongsTab = {
                                    libraryViewModel.setAudioFilter(AudioFilterTab.LIKED)
                                    navController.navigate(AudioRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onPlayLikedSong = { items, idx ->
                                    ensureNotificationPermission()
                                    libraryViewModel.playMediaQueue(items, idx)
                                },
                                onAddToPlaylist = { playlistTargetMedia = it },
                                onTogglePlayPause = playerViewModel::togglePlayPause,
                                onSeekTo = playerViewModel::seekTo,
                                onSkipNext = playerViewModel::skipNext,
                                onSkipPrevious = playerViewModel::skipPrevious,
                                onSkipToQueueIndex = playerViewModel::skipToQueueIndex,
                                onToggleShuffle = playerViewModel::toggleShuffle,
                                onCycleRepeat = playerViewModel::cycleRepeat,
                                onToggleLike = playerViewModel::toggleCurrentLike,
                                onSetSpeed = playerViewModel::setSpeed,
                                onSetSleepTimer = playerViewModel::setSleepTimer,
                                onSelectSubtitleTrack = playerViewModel::selectSubtitleTrack,
                                onAttachExternalSubtitle = playerViewModel::attachExternalSubtitle,
                                onUpdateSubtitlePrefs = playerViewModel::updateSubtitlePreferences
                            )
                        }

                        composable<EqualizerRoute> {
                            val customProfiles by equalizerViewModel.customProfiles.collectAsStateWithLifecycle()
                            EqualizerScreen(
                                uiState = eqState,
                                customProfiles = customProfiles,
                                isAudioPlaying = playbackState.isPlaying,
                                activeMediaTitle = playbackState.currentTitle.takeIf {
                                    playbackState.hasMedia && it.isNotBlank()
                                },
                                onBack = { navController.popBackStack() },
                                onSetEnabled = equalizerViewModel::setEnabled,
                                onSelectPreset = equalizerViewModel::applyPreset,
                                onSelectCustomProfile = equalizerViewModel::applyCustomProfile,
                                onBandLevelChange = equalizerViewModel::setBandLevel,
                                onResetBand = equalizerViewModel::resetBand,
                                onBassBoostChange = equalizerViewModel::setBassBoost,
                                onVirtualizerChange = equalizerViewModel::setVirtualizer,
                                onLoudnessChange = equalizerViewModel::setLoudnessGain,
                                onSaveCustomProfile = equalizerViewModel::saveCustomProfile,
                                onDeleteCustomProfile = equalizerViewModel::deleteCustomProfile,
                                onReset = equalizerViewModel::reset
                            )
                        }
                    }
                }
            }
        }

        FloatingMiniVideoOverlay(
            playbackState = playbackState,
            controller = controller,
            isVisible = isFloatingVideoMode && !isFullScreenPlayer && !isInPipMode,
            onExpand = {
                isFloatingVideoMode = false
                navController.navigate(VideoPlayerRoute) { launchSingleTop = true }
            },
            onDockBack = { isFloatingVideoMode = false },
            onTogglePlayPause = { playerViewModel.togglePlayPause() },
            onDismiss = {
                isFloatingVideoMode = false
                playerViewModel.stopAndClear()
            }
        )
    }

    if (showManageFilesPrompt && !hasStoragePermission) {
        AlertDialog(
            onDismissRequest = { showManageFilesPrompt = false },
            title = { Text("Allow All Files Access") },
            text = {
                Text(
                    "Grant permission to manage all files on your device so AetherPlay can automatically scan and organize all your video and audio folders."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showManageFilesPrompt = false
                        requestManageAllFilesPermission()
                    },
                    modifier = Modifier.testTag("manage_files_allow_button")
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showManageFilesPrompt = false },
                    modifier = Modifier.testTag("manage_files_later_button")
                ) {
                    Text("Not Now")
                }
            }
        )
    }

    if (showStreamDialog) {
        OpenStreamDialog(
            onDismiss = { showStreamDialog = false },
            onConfirmStream = { url, title, isVideo ->
                ensureNotificationPermission()
                libraryViewModel.addAndPlayStream(
                    url = url,
                    title = title,
                    isVideo = isVideo,
                    onOpenVideo = { navController.navigate(VideoPlayerRoute) { launchSingleTop = true } },
                    onOpenAudio = { navController.navigate(AudioPlayerRoute) { launchSingleTop = true } }
                )
            }
        )
    }

    playlistTargetMedia?.let { media ->
        AddToPlaylistSheet(
            mediaItem = media,
            playlists = libraryState.playlists,
            onDismiss = { playlistTargetMedia = null },
            onSelectPlaylist = { pw ->
                libraryViewModel.addToPlaylist(pw.playlist.id, media.uri, pw.playlist.name)
            },
            onCreatePlaylist = { name ->
                libraryViewModel.createPlaylist(name)
            }
        )
    }
}
