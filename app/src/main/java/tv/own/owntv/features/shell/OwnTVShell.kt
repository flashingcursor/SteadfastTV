package tv.own.owntv.features.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import tv.own.owntv.R
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.launcher.LauncherDeepLink
import tv.own.owntv.core.launcher.LauncherIntegrationRepository
import tv.own.owntv.core.launcher.LauncherLaunch
import tv.own.owntv.core.update.UpdateManager
import tv.own.owntv.features.update.UpdateDialog
import tv.own.owntv.features.update.UpdateStatusToast
import tv.own.owntv.features.downloads.DownloadsScreen
import tv.own.owntv.features.epg.EpgScreen
import tv.own.owntv.features.home.HomeScreen
import tv.own.owntv.features.home.HomeViewModel
import tv.own.owntv.features.live.LiveScreen
import tv.own.owntv.features.live.LiveViewModel
import tv.own.owntv.features.movies.MoviesScreen
import tv.own.owntv.features.movies.MovieViewModel
import tv.own.owntv.features.search.SearchScreen
import tv.own.owntv.features.series.SeriesScreen
import tv.own.owntv.features.series.SeriesViewModel
import tv.own.owntv.player.MiniPlayer
import tv.own.owntv.player.MpvVideoSurface
import tv.own.owntv.player.OwnTVPlayer
import tv.own.owntv.player.PlayerHud
import tv.own.owntv.features.settings.data.RailPosition
import tv.own.owntv.features.shell.components.AvatarPickerDialog
import tv.own.owntv.features.shell.components.CategoryRail
import tv.own.owntv.features.shell.components.ContentPane
import tv.own.owntv.features.shell.components.ExitDialog
import tv.own.owntv.features.shell.components.FloatingRail
import tv.own.owntv.features.shell.components.IncompleteRestoreDialog
import tv.own.owntv.features.shell.components.PreviewPane
import tv.own.owntv.features.shell.components.RailCategory
import tv.own.owntv.features.shell.components.SettingsScreen
import tv.own.owntv.features.shell.components.ShellHeader
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.res.stringResource
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.theme.Dimens
import tv.own.owntv.ui.theme.GlassSurface
import tv.own.owntv.ui.theme.LocalGlass
import tv.own.owntv.ui.theme.OwnTVTheme
import tv.own.owntv.ui.theme.ThemeMode
import tv.own.owntv.ui.theme.ownTvTween

/** Which layer currently holds focus (drives Back navigation). */
private enum class ShellLayer { SIDEBAR, RAIL, CONTENT }

/** Player presentation: hidden, fullscreen, docked mini-player, or audio-only now-playing bar. */
private enum class PlayerMode { NONE, FULLSCREEN, MINI, AUDIO }

/**
 * The MD3 shell: a fixed navigation panel (Layer 1) plus the active destination. Settings is a
 * single-pane sectioned screen; browse sections keep the Folder Rail → Content → Preview layout.
 */
@Composable
fun OwnTVShell(
    selectedSection: MainSection,
    visibleSections: Set<MainSection>,
    onSelectSection: (MainSection) -> Unit,
    themeMode: ThemeMode,
    uiZoomPercent: Int,
    onSetZoom: (Int) -> Unit,
    avatarId: Int,
    onSetAvatar: (Int) -> Unit,
    profileName: String,
    // Home re-fetches when the active playlist changes (see the LaunchedEffect keyed on this below);
    // switching the active playlist itself lives in Settings now (final-review cleanup, M8) — this is
    // read-only here.
    activePlaylistId: Long = -1L,
    weatherInfo: tv.own.owntv.core.weather.WeatherInfo? = null, // Phase 7
    weatherFahrenheit: Boolean = false,
    activeProfileId: Long?,
    pendingDeepLink: LauncherDeepLink?,
    onDeepLinkConsumed: () -> Unit,
    isOffline: Boolean = false,
    onExitApp: () -> Unit,
    onSwitchProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors
    val subtitleLoadFailed = stringResource(R.string.content_subtitle_load_failed)
    val railSelection = remember { mutableStateMapOf<MainSection, Int>() }
    val selectedRail = railSelection[selectedSection] ?: 0
    val categories = railCategoriesFor(selectedSection)

    val scope = rememberCoroutineScope()
    val sidebarFocus = remember { FocusRequester() }
    var focusedLayer by remember { mutableStateOf(ShellLayer.SIDEBAR) }
    var showExit by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var playerMode by remember { mutableStateOf(PlayerMode.NONE) }
    // Floating shell (Task 4): rail docking edge, persisted in Settings (Task 1). Default LEFT.
    val railPositionValue by koinInject<tv.own.owntv.features.settings.data.SettingsRepository>()
        .railPosition.collectAsStateWithLifecycle(initialValue = RailPosition.DEFAULT)
    // True while D-pad focus sits inside the rail (fires from FloatingRail's onActiveChange).
    var railActive by remember { mutableStateOf(false) }
    // BACK-forced expansion (Task 5's unified BACK floor, §4): content BACK sets this true and focuses
    // the rail so it renders expanded immediately, before the focus-within transition itself lands
    // (see the LaunchedEffect below, which clears it once railActive catches up).
    var railForceActive by remember { mutableStateOf(false) }
    // Once real D-pad focus settles inside the rail, `railActive` already keeps the panel expanded
    // (`active = forceActive || focusWithin` in FloatingRail) — the forced flag has done its job and
    // stops being needed. Clearing it here (rather than leaving it stuck true) is what lets a later
    // BACK press correctly read "rail active" from railActive alone once focus later leaves the rail.
    LaunchedEffect(railActive) { if (railActive) railForceActive = false }
    // Avatar/Settings (and their focus anchors) only compose while the rail is active, so the
    // BACK-floor's synchronous sidebarFocus.requestFocus() can fire a frame before its target item
    // exists (sidebarFocus attaches to the Settings item when Settings is the selected section).
    // Retry after the forced expansion has actually recomposed the drawer contents.
    LaunchedEffect(railForceActive) {
        if (railForceActive) runCatching { sidebarFocus.requestFocus() }
    }
    // Hoisted above the header (final-review, I2): the LEFT edge drawer being active drives the
    // header title fade (ShellHeader's `titleVisible` below). The drawer's active geometry itself
    // now lives inside FloatingRail (keyed off its own `active`), so this boolean's remaining job
    // is the shell-level title coordination.
    val leftRailActive = railPositionValue == RailPosition.LEFT && (railActive || railForceActive)
    // Measured header/audio-row/rail dimensions (px), used to place the rail relative to whatever's
    // actually above it and to reserve exactly that much space for the content below/beside it —
    // computed once per layout pass rather than hard-coded, so it self-corrects for UI zoom,
    // locale-driven text size, and the Audio Mode row's presence instead of drifting out of sync
    // with a hand-tuned constant.
    // TOP mode: combined height of the header AND the Audio Mode now-playing row (when present) —
    // the rail must dock below whichever of those is currently on screen, not just the header alone.
    var topBlockHeightPx by remember { mutableIntStateOf(0) }
    // The rail's own measured cross-axis size (height in TOP mode, width in LEFT mode below) drives
    // the content reservation, frozen at the smallest ("truly idle") size ever observed — see the
    // FloatingRail onSizeChanged callback further down, which is where the freeze is actually
    // enforced. `remember(railPositionValue)` resets both to 0 (unmeasured) on every LEFT<->TOP
    // switch: LEFT's frozen width is meaningless as a TOP-mode height reservation and vice versa —
    // without this key, switching orientation at runtime would apply a stale reservation from the
    // OTHER orientation for a pass (content shoved off-screen) until the rail happened to re-measure.
    var railHeightPx by remember(railPositionValue) { mutableIntStateOf(0) }
    // LEFT mode: the rail's own measured width, so the content start-inset always reserves exactly
    // enough room regardless of rail padding/border/content changes.
    var railWidthPx by remember(railPositionValue) { mutableIntStateOf(0) }
    val density = LocalDensity.current
    // Deep-link: the Guide's "Add EPG" button switches to Settings and opens EPG Sources → add.
    var openEpgAdd by remember { mutableStateOf(false) }
    // One-shot: set when leaving the player so the returning browse screen re-focuses the item you played.
    // Also armed on rail SELECT (Task 4) so choosing a section jumps focus straight into its content —
    // but only for the sections that consume it; see the onSelect wiring at the FloatingRail call site.
    var restoreFocus by remember { mutableStateOf(false) }
    val player = koinInject<OwnTVPlayer>()
    // Docked mini-player size (% of screen width) + position, configurable in Settings and from the
    // mini-player's own controls. Read straight from settings so both entry points stay in sync.
    val settingsRepo = koinInject<tv.own.owntv.features.settings.data.SettingsRepository>()
    val miniSizePct by settingsRepo.miniPlayerSizePct.collectAsStateWithLifecycle(initialValue = tv.own.owntv.player.MiniPlayerSize.DEFAULT)
    val miniPosName by settingsRepo.miniPlayerPosition.collectAsStateWithLifecycle(initialValue = tv.own.owntv.player.MiniPlayerPosition.DEFAULT.name)
    val miniPos = tv.own.owntv.player.MiniPlayerPosition.fromName(miniPosName)
    val subtitleController = koinInject<tv.own.owntv.core.subtitles.SubtitleController>()
    val subtitleContext by subtitleController.current.collectAsStateWithLifecycle()
    var showSubtitleSearch by remember { mutableStateOf(false) }
    // Local subtitle-file picker (plan §7) — the same TV-safe in-app browser local M3U import uses.
    var showLocalSubPicker by remember { mutableStateOf(false) }
    val localSubToast = tv.own.owntv.ui.components.rememberInAppToast()
    val mpvEngine = remember(player) { tv.own.owntv.player.MpvPlaybackEngine(player) }
    // Audio focus + MediaSession (F27). This is the only place that knows which engine currently owns
    // the speaker, so it hands that engine over and takes it back when the player closes.
    val playbackSession = koinInject<tv.own.owntv.player.PlaybackSession>()
    val launcherIntegrationRepository = koinInject<LauncherIntegrationRepository>()
    val homeVm = org.koin.androidx.compose.koinViewModel<HomeViewModel>()
    val movieVm = org.koin.androidx.compose.koinViewModel<MovieViewModel>()
    val seriesVm = org.koin.androidx.compose.koinViewModel<SeriesViewModel>()
    // Same activity-scoped instances the Live/Guide screens use — lets the fullscreen HUD zap channels
    // up/down (CH+/CH-). Guide tunes start through LiveViewModel too (they set zapSource = LIVE_TV), so
    // there is exactly ONE zap path: liveVm's. The Guide keeps its own EpgViewModel only for the grid.
    val liveVm = org.koin.androidx.compose.koinViewModel<LiveViewModel>()
    val epgVm = org.koin.androidx.compose.koinViewModel<tv.own.owntv.features.epg.EpgViewModel>()
    val liveCanZap by liveVm.canZap.collectAsStateWithLifecycle()
    // Full-screen is running on the ExoPlayer engine (a promoted Live preview) rather than mpv.
    val liveOnExo by liveVm.liveOnExo.collectAsStateWithLifecycle()
    // A catch-up archive programme is playing (Guide "Watch from start" or the Live TV catch-up picker)
    // rather than the live stream — the HUD swaps live-only controls for the VOD ones.
    val catchupActive by liveVm.catchupActive.collectAsStateWithLifecycle()
    val vodExoActive by player.exoActiveState.collectAsStateWithLifecycle()
    // Publish the active engine to the system (audio focus + MediaSession), and detach when the player
    // is closed — an inactive session must not keep answering the TV's transport keys or the Assistant.
    LaunchedEffect(liveOnExo, playerMode) {
        playbackSession.attach(
            if (playerMode == PlayerMode.NONE) null else if (liveOnExo) liveVm.previewEngine else mpvEngine,
        )
    }
    // Auto frame rate: only ever applied to the FULL-SCREEN surface (never the mini-player or the
    // in-pane Live preview) — see FrameRateController.
    val autoFrameRate by settingsRepo.autoFrameRate.collectAsStateWithLifecycle(initialValue = false)
    // ...and the one-time suggestion to turn it on, for the 25-fps-on-60-Hz judder the direct render path
    // cannot fix by itself (F13). `true` until the flag is read, so it can never flash on first frame.
    val afrPrompted by settingsRepo.autoFrameRatePrompted.collectAsStateWithLifecycle(initialValue = true)
    // Direct tune (type a channel number on the remote). Settings → Video Player → Live TV; default on.
    val directTuneEnabled by settingsRepo.directTune.collectAsStateWithLifecycle(initialValue = true)
    // "Prefer EPG logos": start following the setting once, here rather than in Application.onCreate —
    // the store queries nothing at all while the toggle is off, so cold start stays free of EPG reads.
    val epgDaoForLogos = koinInject<tv.own.owntv.core.database.dao.EpgDao>()
    val logoScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        tv.own.owntv.core.epg.EpgLogoStore.start(logoScope, settingsRepo, epgDaoForLogos)
    }
    // Live rewind / timeshift: whether the live channel supports catch-up, and how far behind live we are.
    val canRewindLive by liveVm.canRewindLive.collectAsStateWithLifecycle()
    val timeshiftOffset by liveVm.timeshiftOffsetSec.collectAsStateWithLifecycle()
    // Which section armed the current fullscreen stream — picks whose channel list CH+/CH- step through.
    var zapSource by remember { mutableStateOf<MainSection?>(null) }
    // In-player channel-list overlay (Left while controls hidden, live only).
    var showChannelList by remember { mutableStateOf(false) }
    // In-player watch-history list (Right while controls hidden, live only).
    var showHistoryList by remember { mutableStateOf(false) }
    val zapChannels by liveVm.zapChannels.collectAsStateWithLifecycle()
    val zapListTitle by liveVm.zapListTitle.collectAsStateWithLifecycle()
    val zapOverlayTitle = zapListTitle ?: stringResource(R.string.content_category_all_channels)
    val showCategoryBrowser by liveVm.showCategoryBrowser.collectAsStateWithLifecycle()
    val browserCategories by liveVm.browserCategories.collectAsStateWithLifecycle()
    val previewChannel by liveVm.previewChannel.collectAsStateWithLifecycle()
    // Favorite state for the player HUD's in-stream favorite toggle (live channel / movie / series).
    val liveFavoriteIds by liveVm.favoriteIds.collectAsStateWithLifecycle()
    val playingMovie by movieVm.playingMovie.collectAsStateWithLifecycle()
    val movieFavoriteIds by movieVm.favoriteIds.collectAsStateWithLifecycle()
    val playingSeries by seriesVm.playingSeries.collectAsStateWithLifecycle()
    val seriesFavoriteIds by seriesVm.favoriteIds.collectAsStateWithLifecycle()
    // Current programme per channel for the in-player channel list overlay (small subtitle under each row).
    // Only resolved while the overlay is actually open. Keyed on the channel set so a zap-list change re-resolves.
    val overlayNowPlaying by produceState<Map<Long, String>>(emptyMap(), showChannelList, zapChannels) {
        if (!showChannelList || zapChannels.size <= 1) { value = emptyMap(); return@produceState }
        value = runCatching { liveVm.nowPlayingFor(zapChannels) }.getOrDefault(emptyMap())
    }
    // Recently-watched channels for the right-hand history overlay — re-read each time it opens (and
    // after a zap, since tuning writes a new history row) so the newest channel is always on top.
    val historyChannels by produceState(emptyList<ChannelEntity>(), showHistoryList, previewChannel?.id) {
        if (!showHistoryList) { value = emptyList(); return@produceState }
        value = runCatching { liveVm.historyChannels() }.getOrDefault(emptyList())
    }
    val historyNowPlaying by produceState<Map<Long, String>>(emptyMap(), historyChannels) {
        if (historyChannels.isEmpty()) { value = emptyMap(); return@produceState }
        value = runCatching { liveVm.nowPlayingFor(historyChannels) }.getOrDefault(emptyMap())
    }
    // "Resume last channel on startup" (opt-in, default off): once when the shell first appears, if enabled
    // and nothing is playing, jump straight back into the last live channel watched. Reads the setting once
    // (via first()) so toggling it later in Settings never yanks the user into a channel.
    val resumeSettings = koinInject<tv.own.owntv.features.settings.data.SettingsRepository>()
    LaunchedEffect(Unit) {
        if (playerMode != PlayerMode.NONE) return@LaunchedEffect
        val pid = resumeSettings.activeProfileId.first()
        when (resumeSettings.startupMode(pid).first()) {
            tv.own.owntv.features.settings.data.StartupMode.LAST_CHANNEL -> {
                val ch = liveVm.lastWatchedLiveChannel()
                if (ch != null && playerMode == PlayerMode.NONE) {
                    zapSource = MainSection.LIVE_TV
                    liveVm.watchFullscreen(ch, listOf(ch))
                    playerMode = PlayerMode.FULLSCREEN
                }
            }
            // Open straight to Live TV on the Favorites folder, with focus landing inside the channel list
            // (restoreFocus drives LiveScreen to focus the first/last channel, not the nav panel).
            tv.own.owntv.features.settings.data.StartupMode.FAVORITES -> {
                onSelectSection(MainSection.LIVE_TV)
                liveVm.select(tv.own.owntv.features.live.LiveKey.Favorites)
                restoreFocus = true
            }
            tv.own.owntv.features.settings.data.StartupMode.HOME -> Unit
        }
    }

    // Movies/Series/Live load on first open via their reactive Paging flows — their indexed first page is
    // cheap, so they need NO preloading (a Live-TV-only user pays nothing for them). The TV Guide is the ONE
    // exception: load() pulls every guide channel + a programme window, which is heavy enough that doing it on
    // open felt slow. So warm EPG in the background shortly after the shell renders — opening the Guide is then
    // instant, matching how it behaved before. (EpgScreen also calls load() on mount, so this is a pure pre-warm
    // and is skipped if the user is already on EPG.)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1_200)
        if (selectedSection != MainSection.EPG) { tv.own.owntv.Perf.stamp("epg-preload"); epgVm.load() }
    }

    // Opening content from a browse screen goes fullscreen — UNLESS the player is already docked as a
    // mini-player, in which case it stays docked and just swaps to the newly-selected stream (the VM
    // already started it), so picking a channel updates the PiP window in place (#6).
    fun openFullscreen(source: MainSection = selectedSection) {
        restoreFocus = false
        zapSource = source
        homeVm.stopPreview()
        // Only Live TV promotes a channel to the ExoPlayer engine. Movies/Series/Search/EPG/Downloads all
        // play on mpv — clear any stale live-on-ExoPlayer flag so the shell renders mpv, not the old channel.
        if (source != MainSection.LIVE_TV) liveVm.clearLiveOnExo()
        // Only Movies/Series/Downloads carry an external-subtitle item context (set by their play
        // paths). Anything else (Live/EPG/Search-channel) clears it so ADD SUBTITLES never shows stale.
        if (source != MainSection.MOVIES && source != MainSection.SERIES && source != MainSection.DOWNLOADS) {
            subtitleController.clear()
        }
        // A new stream is opening — make sure any Audio Mode video-off state is cleared, else mpv keeps
        // `vid=no` and the new item would play with no picture.
        player.exitAudioOnly(); runCatching { liveVm.previewEngine.exitAudioOnly() }
        if (playerMode != PlayerMode.MINI) playerMode = PlayerMode.FULLSCREEN
    }
    // Restore video output on both engines (no-op unless we were in Audio Mode). mpv `vid=auto` /
    // ExoPlayer surface is re-attached by the surface remount right after.
    val resumeVideo = {
        player.exitAudioOnly()
        runCatching { liveVm.previewEngine.exitAudioOnly() }
    }
    // The mini-player's own expand button always maximizes.
    val expandPlayer = { resumeVideo(); restoreFocus = false; playerMode = PlayerMode.FULLSCREEN }
    val exitPlayer = {
        resumeVideo() // restore mpv `vid=auto` before stop so the next played item isn't left video-less
        playerMode = PlayerMode.NONE
        showChannelList = false
        showHistoryList = false
        liveVm.hideCategoryBrowser()
        liveVm.onFullscreenExited() // no longer full-screen on ExoPlayer → let the preview re-take the engine
        player.stop()
        subtitleController.clear() // leaving the player drops the OpenSubtitles item context
        if (selectedSection != MainSection.LIVE_TV) liveVm.clearLiveOnExo()
        restoreFocus = true
        runCatching { sidebarFocus.requestFocus() }
        Unit
    }
    val dockPlayer = {
        resumeVideo()
        playerMode = PlayerMode.MINI
        restoreFocus = true
        runCatching { sidebarFocus.requestFocus() }
        Unit
    }
    // Switch the current stream to audio-only and surface the now-playing bar in the top bar. Stop the
    // video decoder FIRST (plan §5 ordering rule), then drop the video surface by leaving FULLSCREEN/MINI.
    val toAudioMode = {
        (if (liveOnExo) liveVm.previewEngine else mpvEngine).enterAudioOnly()
        playerMode = PlayerMode.AUDIO
        restoreFocus = true
        runCatching { sidebarFocus.requestFocus() }
        Unit
    }
    // Task 6 (finding #3): closing the exit dialog — via Cancel or BACK — must deterministically put
    // the user back on the expanded rail (the "menu" state BACK just led them to), rather than
    // trusting whatever railActive/focus happen to read once the dialog's own focus trap releases.
    // showExit only ever becomes true while the rail was already active/forced, so re-asserting both
    // here is what makes the restore land the same way every time.
    val dismissExit = {
        showExit = false
        railForceActive = true
        runCatching { sidebarFocus.requestFocus() }
        Unit
    }

    // Startup focus (user feedback): land in the CONTENT with the rail idle — not on the expanded
    // rail, which the old unconditional sidebarFocus request produced. Arms the same one-shot
    // restoreFocus contract the player-exit/rail-select paths use: the browse screen grabs focus
    // once its data is ready. Sections outside browseOrder never consume the flag (M6), so those
    // keep the old rail-focus default rather than leaving focus stranded nowhere.
    LaunchedEffect(Unit) {
        tv.own.owntv.Perf.stamp("shell-composed")
        if (selectedSection in MainSection.browseOrder) {
            restoreFocus = true
        } else {
            runCatching { sidebarFocus.requestFocus() }
        }
    }

    LaunchedEffect(pendingDeepLink, activeProfileId) {
        val deepLink = pendingDeepLink ?: return@LaunchedEffect
        val pid = activeProfileId ?: return@LaunchedEffect
        if (pid < 0) return@LaunchedEffect
        when (deepLink) {
            LauncherDeepLink.OpenLiveSection -> {
                onSelectSection(MainSection.LIVE_TV)
                onDeepLinkConsumed()
            }
            else -> when (val launch = launcherIntegrationRepository.resolveLaunch(pid, deepLink)) {
                is LauncherLaunch.Movie -> {
                    onSelectSection(MainSection.MOVIES)
                    movieVm.play(launch.movie, launch.startPositionMs)
                    openFullscreen(MainSection.MOVIES)
                    onDeepLinkConsumed()
                }
                is LauncherLaunch.Episode -> {
                    onSelectSection(MainSection.SERIES)
                    seriesVm.playEpisodeQueue(launch.show, launch.queue, launch.episode, launch.startPositionMs)
                    openFullscreen(MainSection.SERIES)
                    onDeepLinkConsumed()
                }
                is LauncherLaunch.Live -> {
                    onSelectSection(MainSection.LIVE_TV)
                    liveVm.ensurePlaying(launch.channel)
                    openFullscreen(MainSection.LIVE_TV)
                    onDeepLinkConsumed()
                }
                is LauncherLaunch.Series -> {
                    onSelectSection(MainSection.SERIES)
                    seriesVm.openSeries(launch.show)
                    onDeepLinkConsumed()
                }
                null -> {
                    onDeepLinkConsumed()
                }
            }
        }
    }

    // Stop a leftover live preview when you leave the Live section (but never while fullscreen/mini plays).
    LaunchedEffect(selectedSection, playerMode) {
        if (selectedSection != MainSection.LIVE_TV && playerMode == PlayerMode.NONE) player.stop()
        if (selectedSection != MainSection.HOME || playerMode != PlayerMode.NONE) homeVm.stopPreview()
    }

    LaunchedEffect(selectedSection, playerMode, activeProfileId, activePlaylistId) {
        if (selectedSection == MainSection.HOME && playerMode == PlayerMode.NONE && (activeProfileId?.let { it >= 0 } == true)) {
            homeVm.refresh()
        }
    }

    // Task 5 — the unified BACK floor (spec §4). Registered here, near the TOP of this composable's
    // body, so it's added to the dispatcher BEFORE any child screen's own BackHandler — Compose/
    // OnBackPressedDispatcher runs callbacks LIFO, so every page-local handler (drills, overlays,
    // dialogs, the player HUD) is added later and therefore wins first, exactly preserving their
    // existing unwind chains. This handler only ever fires once nothing below it consumed BACK, i.e.
    // once a section's own state is already at its top ("deep page state → page top" already happened
    // via those page-local handlers before this one is even reached). From there BACK always walks:
    // rail activates → rail active → exit confirmation.
    BackHandler {
        when {
            // The fullscreen player owns BACK entirely while open (unchanged) — leaving it takes
            // priority over any rail/exit bookkeeping below.
            playerMode == PlayerMode.FULLSCREEN -> exitPlayer()
            showAvatarPicker -> showAvatarPicker = false
            showExit -> dismissExit()
            // Rail already active (real D-pad focus inside it, or still BACK-forced expanded from the
            // previous press) → next BACK is the exit confirmation.
            railActive || railForceActive -> showExit = true
            // Content (or anything else) holds focus → activate the rail: force it to render expanded
            // immediately and move focus onto it. railForceActive clears itself once railActive catches
            // up (LaunchedEffect above) or once a section is picked (FloatingRail's onSelect below).
            else -> {
                railForceActive = true
                runCatching { sidebarFocus.requestFocus() }
            }
        }
    }

    // Liquid Glass: when a background image is active, the shell's own base paints must be transparent
    // so the full-bleed image (rendered in MainActivity behind this shell) shows through the gaps
    // between/around panels. Solid otherwise — the usual near-black base.
    val glass = LocalGlass.current
    val shellBase = if (glass.isGlassy(GlassSurface.PANELS) || glass.isGlassy(GlassSurface.SIDEBAR)) Color.Transparent else colors.background

    Box(modifier = modifier.fillMaxSize().background(shellBase)) {
      // Browse UI — hidden while the player is fullscreen (stays visible behind the docked mini-player).
      if (playerMode != PlayerMode.FULLSCREEN) {
        Column(modifier = Modifier.fillMaxSize()) {
          if (isOffline) OfflineBanner()
          // Floating shell: the header spans the full width and the rail floats independently over
          // this area (LEFT: centered on the edge; TOP: centered below the header) rather than
          // sitting in a fixed column like the deleted Sidebar did — both live in one Box below the
          // offline banner so FloatingRail's CenterStart/TopCenter alignment matches the same
          // vertical extent Sidebar used to fill.
          Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Phase 6 — unified panel surface: panels and content area share the theme base
                    // so the rounded borders define regions on one continuous dark surface.
                    // Liquid Glass: transparent here (shellBase) when a background image is active, so
                    // the image shows through the gaps between the content panels.
                    .background(shellBase),
            ) {
                // Floating shell header (Task 3) + Audio Mode now-playing row, measured TOGETHER: the
                // rail must dock below whichever of these is actually on screen, not just the header —
                // when Audio Mode is active the row adds real height below the header that the rail
                // would otherwise draw over.
                Column(modifier = Modifier.fillMaxWidth().onSizeChanged { topBlockHeightPx = it.height }) {
                    // Shown on EVERY section now, including Settings ("top bar same for all").
                    ShellHeader(
                        title = stringResource(selectedSection.labelRes),
                        onSearch = { onSelectSection(MainSection.SEARCH) },
                        weatherInfo = weatherInfo,
                        weatherFahrenheit = weatherFahrenheit,
                        // F6 (Task 5 review carry-over): only reachable by D-pad while the rail holds
                        // focus — matches the old TopBar SearchPill's `searchVisible` gate. Keeps the
                        // pill from stealing focus (and leaving focusedLayer stale) from any direction.
                        searchFocusable = focusedLayer == ShellLayer.SIDEBAR,
                        // Final-review (I2): the LEFT edge drawer is full-height (spec #1, user-confirmed
                        // top-to-bottom), which paints directly over the header's start-zone title behind
                        // it. Rather than shrink the drawer back down, fade the title out while the drawer
                        // is up (weather/clock stay — they sit at the far/end zone, never covered).
                        titleVisible = !leftRailActive,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // Audio Mode now-playing bar: the transparent header (Task 3) has exactly three fixed
                    // zones and no slot for it, so it floats as its own end-aligned row directly under the
                    // header — the same adjacency ("left of the weather chip") the old top bar gave it.
                    if (playerMode == PlayerMode.AUDIO) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            val isLiveStream = liveOnExo || player.isLiveContent
                            val zapFn: ((Int) -> Unit)? = when {
                                !isLiveStream -> null
                                zapSource == MainSection.LIVE_TV && liveCanZap -> liveVm::zap
                                else -> null
                            }
                            val audioEngine = if (liveOnExo) liveVm.previewEngine else mpvEngine
                            val vodNav by audioEngine.nav.collectAsStateWithLifecycle()
                            tv.own.owntv.player.AudioNowPlayingBar(
                                player = audioEngine,
                                isLive = isLiveStream,
                                canPrev = if (isLiveStream) zapFn != null else vodNav.hasPrev,
                                canNext = if (isLiveStream) zapFn != null else vodNav.hasNext,
                                onPrev = { if (isLiveStream) zapFn?.invoke(-1) else mpvEngine.previous() },
                                onNext = { if (isLiveStream) zapFn?.invoke(1) else mpvEngine.next() },
                                onExpand = expandPlayer,
                                onClose = exitPlayer,
                                // Always reachable in Audio Mode — its own D-pad trap keeps focus inside
                                // once entered and Back is the only way out (unchanged from the old
                                // top-bar slot).
                                focusable = true,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        // Task 6 (finding #1 + #4): the single source of truth for "content holds focus",
                        // covering EVERY section — single-pane (Settings/Home/Search/Guide) as well as the
                        // browse 3-pane path — since this Box is the common ancestor of the whole `when`
                        // block below. Per-screen onChildFocused/onFocusChanged wiring still fires and is
                        // harmless/redundant with this, but this ancestor is what guarantees focusedLayer
                        // is never left stale at SIDEBAR from an earlier rail visit (which was silently
                        // reopening the ShellHeader search pill's D-pad gate and skipping straight to the
                        // exit dialog on BACK). Any real focus landing here also means the rail is no
                        // longer holding it, so this is where a forced rail expansion that never actually
                        // got real focus (BACK pressed, sidebarFocus.requestFocus() silently failed) gets
                        // cleared too, instead of leaving the rail stuck expanded forever.
                        //
                        // Deliberately NOT paired with .focusGroup() here (Task 6 follow-up, post-fa73e2c):
                        // that would make this whole content area — which every section already wraps in
                        // its OWN focusGroup() a level in (Settings/EPG directly, Home/Search/Live/Movies/
                        // Series/Downloads internally) — into a SECOND, OUTER FocusTargetNode. That node's
                        // full-content-sized bounds then become a real candidate in Compose's default
                        // bounds-based two-dimensional search (TwoDimensionalFocusSearch.searchChildren),
                        // which is what DOWN-from-a-rail-item consults once the rail's own internal search
                        // bubbles up past FloatingRail with nothing found — and that broke TOP mode's
                        // rail-to-content exit (focus stayed trapped in the rail). onFocusChanged doesn't
                        // need a focusTarget on this same node to observe descendants' hasFocus (it isn't a
                        // participant in that bounds search at all), so dropping focusGroup() here removes
                        // the only structurally new element without giving up the hasFocus tracking.
                        .onFocusChanged {
                            if (it.hasFocus) {
                                focusedLayer = ShellLayer.CONTENT
                                railForceActive = false
                            }
                        }
                        // Final-review finding (I4): an explicit `Modifier.focusProperties { up =
                        // sidebarFocus }` override used to sit here for TOP mode, but it was INERT — every
                        // section below interposes its own FocusTarget (each wraps its content in
                        // `.focusGroup()`/a focusable descendant), so this ancestor Box's `up` override was
                        // never the one consulted; `fetchFocusProperties()` walks the CURRENTLY FOCUSED
                        // node's own ancestors; it never reached this far up. Deleted rather than kept as
                        // dead defense-in-depth.
                        //
                        // The real, verified-on-device mechanism, recorded here since it's easy to
                        // re-break by accident: UP reaches the rail via the DEFAULT spatial search wherever
                        // a section doesn't block it. Single-pane sections (Settings/Home/Search/EPG) and
                        // the browse 3-pane path's ContentPane/PreviewPane don't trap vertical exits, so UP
                        // from their top row resolves spatially straight to the rail (LEFT: it's beside
                        // content, so this needs no help at all; TOP: it's directly above, and reachable the
                        // same way once the header's search pill is out of the running — see ShellHeader's
                        // `searchFocusable` gate above). Live/Movies/Series/Downloads and CategoryRail itself
                        // deliberately call `trapVerticalFocusExit()` (held Up/Down can outrun a lazy list's
                        // composition and escape to the wrong place) — UP is intentionally swallowed there,
                        // so on those screens the rail is reached via BACK (the unified BACK floor, spec §4)
                        // or by first moving to the CategoryRail's top edge. This is the intended design, not
                        // a gap.
                        // Content reservation for the floating rail, implemented ONCE here rather than
                        // per-screen, both driven by the rail's OWN measured size (self-correcting — no
                        // magic constants that can drift out of sync with the rail's real geometry):
                        // LEFT reserves the flush rail's measured width + one gap, so content never
                        // sits under the floating pill; TOP reserves the rail's measured
                        // height + one gap (the header/audio block above it is already accounted for by
                        // column flow, so only the rail itself needs an explicit reservation here — the
                        // matching gap between the header block and the rail is added at the rail's own
                        // top padding below, so both gaps land at Dimens.RailTopGap — shell-refinements
                        // Task 3 tightened this from GapMedium/16dp to RailTopGap/8dp).
                        //
                        // railWidthPx/railHeightPx are 0 until the rail's onSizeChanged callback below
                        // reports its first idle measurement (cold start, or right after a LEFT<->TOP
                        // switch resets the capture — both keyed to railPositionValue) — fall back to
                        // Dimens.RailIdleNominal for that one frame instead of reserving zero space.
                        .padding(
                            start = if (railPositionValue == RailPosition.LEFT) {
                                val w = if (railWidthPx == 0) Dimens.RailIdleNominal else with(density) { railWidthPx.toDp() }
                                w + Dimens.GapMedium
                            } else {
                                0.dp
                            },
                            top = if (railPositionValue == RailPosition.TOP) {
                                val h = if (railHeightPx == 0) Dimens.RailIdleNominal else with(density) { railHeightPx.toDp() }
                                h + Dimens.RailTopGap * 2
                            } else {
                                0.dp
                            },
                            end = 6.dp,
                            bottom = 6.dp,
                        ),
                ) {
                    when {
                        selectedSection == MainSection.SETTINGS -> SettingsScreen(
                            themeMode = themeMode,
                            uiZoomPercent = uiZoomPercent,
                            onSetZoom = onSetZoom,
                            onOpenPlaylist = { /* Phase 6: open setup/playlist */ },
                            openEpgAdd = openEpgAdd,
                            onEpgAddConsumed = { openEpgAdd = false },
                            modifier = Modifier
                                .fillMaxSize()
                                .onFocusChanged { if (it.hasFocus) focusedLayer = ShellLayer.CONTENT }
                                .focusGroup(),
                        )

                        selectedSection == MainSection.HOME -> HomeScreen(
                            vm = homeVm,
                            // Skip the fullscreen player when the global external-player toggle is on
                            // (mounting it spins up mpv even though playback went to the external app).
                            onPlayMovie = { id, pos -> scope.launch { if (movieVm.playByIdAsync(id, pos) && !movieVm.externalPlayerOn.value) openFullscreen(MainSection.MOVIES) } },
                            onPlayEpisode = { seriesId, epId, pos -> scope.launch { if (seriesVm.playFromHomeAsync(seriesId, epId, pos) && !seriesVm.externalPlayerOn.value) openFullscreen(MainSection.SERIES) } },
                            onPlayChannel = { id, zap -> scope.launch { if (liveVm.ensurePlayingByIdAsync(id, zap)) openFullscreen(MainSection.LIVE_TV) } },
                            onOpenGuide = { onSelectSection(MainSection.EPG) },
                            onChildFocused = { focusedLayer = ShellLayer.CONTENT },
                            restoreFocus = restoreFocus,
                            onRestored = { restoreFocus = false },
                            previewEnabled = playerMode == PlayerMode.NONE,
                            modifier = Modifier.fillMaxSize(),
                        )

                        selectedSection == MainSection.SEARCH -> SearchScreen(
                            onFullscreen = { openFullscreen() },
                            // Open the actual series (its episode list), then switch to the Series section —
                            // the screen shares this SeriesViewModel, so it shows the opened show.
                            onOpenSeries = { series -> seriesVm.openSeries(series); onSelectSection(MainSection.SERIES) },
                            // A channel found in Search tunes through the same LiveViewModel path as one
                            // opened from Live TV or the Guide (F05) — Prefer HLS, the ExoPlayer→mpv
                            // ladder, compatibility-mode pins, the external-player toggle, and CH+/- zap.
                            onPlayChannel = { ch ->
                                restoreFocus = false
                                liveVm.watchFromGuide(ch)
                                zapSource = MainSection.LIVE_TV
                                homeVm.stopPreview()
                                if (playerMode != PlayerMode.MINI && !liveVm.externalPlayerOn.value) playerMode = PlayerMode.FULLSCREEN
                            },
                            onChildFocused = { focusedLayer = ShellLayer.CONTENT },
                            modifier = Modifier.fillMaxSize(),
                        )

                        selectedSection == MainSection.LIVE_TV -> LiveScreen(
                            onFullscreen = { openFullscreen() },
                            onChildFocused = { focusedLayer = ShellLayer.CONTENT },
                            previewEnabled = playerMode == PlayerMode.NONE,
                            restoreFocus = restoreFocus,
                            onRestored = { restoreFocus = false },
                            modifier = Modifier.fillMaxSize(),
                        )

                        selectedSection == MainSection.MOVIES -> MoviesScreen(
                            onFullscreen = { openFullscreen() },
                            onChildFocused = { focusedLayer = ShellLayer.CONTENT },
                            restoreFocus = restoreFocus,
                            onRestored = { restoreFocus = false },
                            modifier = Modifier.fillMaxSize(),
                        )

                        selectedSection == MainSection.SERIES -> SeriesScreen(
                            onFullscreen = { openFullscreen() },
                            onChildFocused = { focusedLayer = ShellLayer.CONTENT },
                            restoreFocus = restoreFocus,
                            onRestored = { restoreFocus = false },
                            modifier = Modifier.fillMaxSize(),
                        )

                        selectedSection == MainSection.DOWNLOADS -> DownloadsScreen(
                            onFullscreen = { openFullscreen() },
                            onChildFocused = { focusedLayer = ShellLayer.CONTENT },
                            restoreFocus = restoreFocus,
                            onRestored = { restoreFocus = false },
                            modifier = Modifier.fillMaxSize(),
                        )

                        selectedSection == MainSection.EPG -> EpgScreen(
                            onBack = { runCatching { sidebarFocus.requestFocus() } },
                            onFullscreen = { openFullscreen() },
                            onPlayChannel = { ch, _ ->
                                restoreFocus = false
                                liveVm.watchFromGuide(ch)
                                zapSource = MainSection.LIVE_TV
                                homeVm.stopPreview()
                                // Live TV set to play externally → the channel went to another app;
                                // don't mount the fullscreen player over it.
                                if (playerMode != PlayerMode.MINI && !liveVm.externalPlayerOn.value) playerMode = PlayerMode.FULLSCREEN
                            },
                            onPlayCatchup = { ch, prog ->
                                restoreFocus = false
                                liveVm.playCatchupProgramme(ch, prog)
                                zapSource = MainSection.LIVE_TV
                                homeVm.stopPreview()
                                if (playerMode != PlayerMode.MINI) playerMode = PlayerMode.FULLSCREEN
                            },
                            onAddEpg = { openEpgAdd = true; onSelectSection(MainSection.SETTINGS) },
                            restoreFocus = restoreFocus,
                            onRestored = { restoreFocus = false },
                            modifier = Modifier
                                .fillMaxSize()
                                .onFocusChanged { if (it.hasFocus) focusedLayer = ShellLayer.CONTENT }
                                .focusGroup(),
                        )

                        else -> Row(modifier = Modifier.fillMaxSize()) {
                            CategoryRail(
                                categories = categories,
                                selectedIndex = selectedRail,
                                onSelect = { railSelection[selectedSection] = it },
                                onFocused = { focusedLayer = ShellLayer.RAIL },
                            )

                            ContentPane(
                                sectionTitle = stringResource(selectedSection.labelRes),
                                categoryName = categories.getOrNull(selectedRail)?.let { category -> category.labelRes?.let { stringResource(it) } ?: category.fullName }
                                    ?: stringResource(R.string.content_category_all_channels),
                                countLabel = placeholderCount(selectedSection),
                                emptyIcon = selectedSection.emptyIcon,
                                emptyMessage = stringResource(R.string.content_empty_section, stringResource(selectedSection.labelRes)),
                                onAddSource = { onSelectSection(MainSection.SETTINGS) },
                                modifier = Modifier
                                    .weight(1.4f)
                                    .onFocusChanged { if (it.hasFocus) focusedLayer = ShellLayer.CONTENT }
                                    .focusGroup(),
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .padding(Dimens.GapLarge),
                            ) {
                                PreviewPane(hint = stringResource(R.string.content_preview_select_channel))
                            }
                        }
                    }
                }
            }
            // Active-rail scrim: dims the header AND content (user feedback — it used to sit inside
            // the content Box and left the header at full brightness) while the rail holds D-pad
            // focus or is BACK-forced active (Task 5's unified BACK floor sets railForceActive
            // true). Lives at this Box level so it covers the whole header+content column yet still
            // composes BEFORE FloatingRail below — the rail stays undimmed on top. Content stays
            // visible underneath — focus lives in the rail.
            val scrimAlpha by animateFloatAsState(
                targetValue = if (railActive || railForceActive) 0.45f else 0f,
                animationSpec = ownTvTween(),
                label = "railScrim",
            )
            if (scrimAlpha > 0f) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)))
            }
            // LEFT sits flush at the start edge in BOTH states (the 30dp floating inset — and its
            // active->0 animation — was removed along with the vestigial corner radius: expanding is
            // now purely the panel appearing + labels extending, with no positional shift). Active
            // still swaps the centered icon column for the edge-pinned, full-height drawer below.
            FloatingRail(
                position = railPositionValue,
                selected = selectedSection,
                visibleSections = visibleSections,
                // Rail activation clears on selection too (spec §4), not just on the focus-settles path
                // above — belt-and-suspenders for the same click-without-a-focus-transition edge case.
                //
                // Task 4 (shell-refinements): SELECT also jumps focus into the new section's content —
                // the same one-shot `restoreFocus` flag/`onRestored` contract already used when leaving
                // the player (see exitPlayer/dockPlayer/toAudioMode above, and LiveScreen's consumption
                // of it). Only armed for the sections that actually consume it (MainSection.browseOrder:
                // Home/Live/Movies/Series/Downloads/EPG) — final-review fix (M6): arming it unconditionally
                // for EVERY section let it latch true after selecting Settings or Search (neither reads nor
                // clears the flag), so a LATER unrelated focus-into-content entry — a deep link, or picking
                // a browse section from Settings — could see that stale `true` and yank focus somewhere the
                // user never asked for. Search isn't wired to this flag at all regardless — SearchScreen
                // already self-focuses its search field on every entry via its own LaunchedEffect(Unit).
                // Settings doesn't consume it either — its onEnter-based focus-restore only fires for
                // directional D-pad entry, not a programmatic flag — so selecting either leaves focus on
                // the rail, the documented safe fallback.
                //
                // Each browse screen guards its restoreFocus LaunchedEffect on its data actually being
                // loaded (itemCount/size == 0 -> early return without grabbing focus or calling
                // onRestored), so a still-loading section simply leaves focus on the rail item just pressed
                // until data arrives, then focuses automatically. (M7: this IS a late-yank window — if the
                // user navigates away again before that data arrives, the deferred focus grab can still
                // fire once it does, landing on a screen the user is no longer looking at. Not fixed here;
                // flagged for a follow-up that ties the effect to "still the selected section" too.)
                onSelect = { section ->
                    railForceActive = false
                    if (section in MainSection.browseOrder) restoreFocus = true
                    onSelectSection(section)
                },
                avatarId = avatarId,
                onPickAvatar = { showAvatarPicker = true },
                onSwitchProfile = onSwitchProfile,
                profileName = profileName,
                selectedItemFocusRequester = sidebarFocus,
                onActiveChange = { active ->
                    railActive = active
                    if (active) focusedLayer = ShellLayer.SIDEBAR
                },
                forceActive = railForceActive,
                modifier = Modifier
                    .align(if (railPositionValue == RailPosition.LEFT) Alignment.CenterStart else Alignment.TopCenter)
                    // The drawer's active geometry (fillMaxHeight + IntrinsicSize.Max width, C1
                    // bound included) moved INTO FloatingRail's own column chain so it can sit
                    // below the panel's shadow/clip/glass/border and be eased open by the
                    // animateContentSize there — the shell now only places the rail.
                    .padding(
                        // Docks below whichever of header/audio-row is actually on screen (topBlockHeightPx
                        // measures both together, F2) — one RailTopGap gap; the content Box below reserves
                        // a second RailTopGap past the rail's own measured height so both gaps match.
                        top = if (railPositionValue == RailPosition.TOP) {
                            with(density) { topBlockHeightPx.toDp() } + Dimens.RailTopGap
                        } else {
                            0.dp
                        },
                    )
                    .onSizeChanged {
                        // Reserve the IDLE rail size only. The approved mockup dims content IN PLACE
                        // under the expanded rail — an overlay — never pushes/reflows it (the old
                        // Sidebar's "fixed rail, layout never jumps" principle). Active (focused or
                        // BACK-forced) rendering balloons the rail with labels + the glass panel; skip
                        // capturing that while active, so the expanded rail floats over the (already
                        // dimmed, via the scrim above) content instead of shoving it aside.
                        //
                        // minOf (not a plain overwrite) is what makes the freeze hold during the ~220ms
                        // COLLAPSE animation too, not just while active: the moment focus leaves, this
                        // guard reopens, but the collapse animation's intermediate frames are all still
                        // LARGER than the true idle size (shrinking down toward it) — since the value
                        // already sitting in railWidthPx/railHeightPx from before the rail ever expanded
                        // IS that true idle size, minOf(idle, anyLargerTransientFrame) always stays at
                        // idle. Content genuinely never moves, not even eased, through a full
                        // expand-then-collapse cycle. The `== 0` branch is only the very first
                        // measurement after a cold start or a railPositionValue-keyed reset (see the
                        // state declarations above), where there is no prior floor to take a min against.
                        if (!(railActive || railForceActive)) {
                            railWidthPx = if (railWidthPx == 0) it.width else minOf(railWidthPx, it.width)
                            railHeightPx = if (railHeightPx == 0) it.height else minOf(railHeightPx, it.height)
                        }
                    },
            )
          }
        }
      }

      // Unobtrusive background-sync pill (bottom middle): visible while any catalog sync runs —
      // backgrounded first import, remainder worker, auto refresh — but never over fullscreen video.
      if (playerMode != PlayerMode.FULLSCREEN) {
          tv.own.owntv.features.shell.components.SyncStatusPill(modifier = Modifier.align(Alignment.BottomCenter))
      }

      // Brand watermark (Task 4): bottom-right, above content and below dialogs/player. Hidden the
      // moment ANY player mode is active (fullscreen, docked mini, or audio-only), not just fullscreen.
      if (playerMode == PlayerMode.NONE) {
          ShellWatermark(modifier = Modifier.align(Alignment.BottomEnd))
      }

      // Player surface — hoisted so it persists across fullscreen <-> mini (same call site = the
      // SurfaceView isn't recreated when docking/expanding, so playback never blips). NOT composed in
      // AUDIO mode: there's no video surface — audio plays and the top-bar now-playing bar drives it.
      if (playerMode == PlayerMode.FULLSCREEN || playerMode == PlayerMode.MINI) {
        val isFull = playerMode == PlayerMode.FULLSCREEN
        Box(
            modifier = if (isFull) {
                Modifier.fillMaxSize().background(Color.Black)
            } else {
                // Dynamic docked size/position: a screen-width fraction at the chosen corner/edge, so it
                // scales with the panel + UI zoom (unlike the old fixed 340×191 dp box).
                Modifier.align(miniPos.alignment).padding(24.dp)
                    .fillMaxWidth(tv.own.owntv.player.MiniPlayerSize.fraction(miniSizePct)).aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp)).background(Color.Black)
            },
        ) {
            // "Promote Preview": a Live channel playing on ExoPlayer renders the ExoPlayer surface — in BOTH
            // full-screen AND the docked mini-player (same call site = the surface persists across dock/
            // expand, so playback never blips). Everything else (mpv) renders mpv's surface.
            if (liveOnExo) {
                tv.own.owntv.player.ExoPreviewSurface(
                    engine = liveVm.previewEngine, modifier = Modifier.fillMaxSize(),
                    keepAwake = true, autoFrameRate = isFull && autoFrameRate,
                )
            } else {
                MpvVideoSurface(player = player, modifier = Modifier.fillMaxSize(), autoFrameRate = isFull && autoFrameRate)
            }
            // Direct render mode: mpv can't draw subtitles on the decoder-owned surface — the app does.
            // Also drawn docked (F19b): the mini-player is a real watching mode for a subtitled film, and
            // dropping the only line of dialogue there made subtitles look broken. Scaled to the box.
            if (!liveOnExo) {
                tv.own.owntv.player.SubtitleOverlay(
                    player = player, modifier = Modifier.fillMaxSize(),
                    // Tied to the chosen mini size, but nudged up and floored: a strictly proportional
                    // line would be unreadable in the smallest box.
                    sizeScale = if (isFull) 1f else {
                        (tv.own.owntv.player.MiniPlayerSize.fraction(miniSizePct) * 1.5f).coerceIn(0.35f, 0.7f)
                    },
                )
            }
            if (isFull && !autoFrameRate && !afrPrompted) {
                // Frame rate of whichever engine is on screen. On the mpv side this is what the direct
                // path judders on; on Exo it now survives "Measured stream stats" being off (F14).
                val activeFps by if (liveOnExo) {
                    liveVm.previewEngine.videoFps.collectAsStateWithLifecycle()
                } else {
                    player.videoFps.collectAsStateWithLifecycle()
                }
                tv.own.owntv.player.AutoFrameRatePrompt(
                    fps = activeFps,
                    afrEnabled = autoFrameRate,
                    alreadyPrompted = afrPrompted,
                    onEnable = { scope.launch { settingsRepo.setAutoFrameRate(true) } },
                    onDismiss = { scope.launch { settingsRepo.setAutoFrameRatePrompted() } },
                )
            }
            if (isFull) {
                // CH+/CH- zap through the channel list of whichever section opened the current stream
                // (Live TV or the Guide); never for VOD. When live plays on ExoPlayer (liveOnExo=true) the
                // mpv `player` is stopped so player.isLiveContent is false — the ExoPlayer engine is the one
                // playing live, so we must check liveOnExo too (otherwise zap breaks for the common case).
                val isLiveStream = liveOnExo || player.isLiveContent
                val zap: ((Int) -> Unit)? = when {
                    !isLiveStream -> null
                    zapSource == MainSection.LIVE_TV && liveCanZap -> liveVm::zap
                    else -> null
                }
                // Live rewind controls apply to a Live-TV channel (live OR its timeshift archive).
                val isLiveChannel = zapSource == MainSection.LIVE_TV
                // ...but NOT to a catch-up archive programme. That's VOD-style playback of a past
                // programme, so it gets the VOD engine toggle (reloads the same archive URL at the same
                // position on the other engine) rather than Live TV's compatibility toggle, which would
                // re-tune the live stream and jump the user to the current programme.
                val isTunedLive = isLiveChannel && !catchupActive
                // Favorite toggle for whatever is playing: the live channel, the movie, or the series
                // (episodes favorite their parent series). Picked by the section that armed the stream.
                val favToggle: (() -> Unit)? = when {
                    isLiveChannel -> previewChannel?.let { ch -> { liveVm.toggleFavorite(ch) } }
                    zapSource == MainSection.MOVIES -> playingMovie?.let { m -> { movieVm.toggleFavorite(m) } }
                    zapSource == MainSection.SERIES -> playingSeries?.let { s -> { seriesVm.toggleFavorite(s) } }
                    else -> null
                }
                val favActive = when {
                    isLiveChannel -> previewChannel?.let { liveFavoriteIds.contains(it.id) } ?: false
                    zapSource == MainSection.MOVIES -> playingMovie?.let { movieFavoriteIds.contains(it.id) } ?: false
                    zapSource == MainSection.SERIES -> playingSeries?.let { seriesFavoriteIds.contains(it.id) } ?: false
                    else -> false
                }
                PlayerHud(
                    player = if (liveOnExo) liveVm.previewEngine else mpvEngine, // HUD drives the active engine
                    onBack = exitPlayer,
                    onPip = dockPlayer, // PiP/dock works for live on either engine now
                    onAudioMode = toAudioMode,
                    // The channel-list overlay draws ABOVE the HUD; while it's open the HUD goes inert so
                    // its hide/error focus grabs can't yank D-pad focus off the overlay.
                    inert = showChannelList || showHistoryList || showCategoryBrowser || showSubtitleSearch || showLocalSubPicker,
                    onChannelUp = zap?.let { z -> { z(-1) } },
                    onChannelDown = zap?.let { z -> { z(1) } },
                    onOpenChannelList = if (isTunedLive && liveCanZap) { { showChannelList = true } } else null,
                    onOpenHistoryList = if (isTunedLive) { { showHistoryList = true } } else null,
                    onRewindLive = if (isTunedLive && canRewindLive) liveVm::rewindLive else null,
                    onForwardLive = if (isTunedLive) liveVm::forwardLive else null,
                    onGoToLive = if (isTunedLive) liveVm::goToLive else null,
                    onScrubLive = if (isTunedLive && canRewindLive) liveVm::scrubLive else null,
                    timeshiftOffsetSec = if (isTunedLive) timeshiftOffset else null,
                    onTuneToNumber = if (directTuneEnabled && isTunedLive && isLiveStream && timeshiftOffset == null && previewChannel != null) liveVm::tuneByNumber else null,
                    directTuneContextKey = previewChannel?.id ?: 0L,
                    // Show the ACTUAL running engine (mpv when pinned OR auto-fallen-back), not just the pin —
                    // otherwise an auto-fallback to mpv still read "EXO". true = on mpv (pill shows MPV, teal).
                    compatMode = if (isTunedLive) !liveOnExo else null,
                    onToggleCompatMode = if (isTunedLive) liveVm::toggleForceMpv else null,
                    // VOD engine toggle (movies/series only — live and catch-up channels keep their own
                    // engine handling above): flip the current item between mpv and ExoPlayer.
                    vodOnExo = if (!isLiveStream && !isTunedLive) vodExoActive else null,
                    onToggleVodEngine = if (!isLiveStream && !isTunedLive) player::toggleVodEngine else null,
                    // ADD SUBTITLES entry: movies/episodes only, and only when the play path set an
                    // item context (subtitle plan §4). Opens the OpenSubtitles search overlay below.
                    onSearchSubtitles = if (!isLiveStream && !isLiveChannel && subtitleContext != null) {
                        { showSubtitleSearch = true }
                    } else null,
                    // Local subtitle file (plan §7): same movie/episode gating, no account needed.
                    onSelectLocalSubtitle = if (!isLiveStream && !isLiveChannel && subtitleContext != null) {
                        { showLocalSubPicker = true }
                    } else null,
                    // In-stream favorite toggle for the current channel/movie/series.
                    favorite = favActive,
                    onToggleFavorite = favToggle,
                    // Guide card for the playing channel (nowNext follows previewChannel = what's playing).
                    liveEpgCard = if (isLiveChannel) {
                        {
                            val epg by liveVm.nowNext.collectAsStateWithLifecycle()
                            tv.own.owntv.features.shell.components.LiveEpgCard(epg = epg)
                        }
                    } else null,
                    modifier = Modifier.fillMaxSize(),
                )
                // OpenSubtitles search overlay (movies/episodes) — drawn above the HUD; the HUD is inert
                // while it's open so the D-pad stays on the overlay.
                if (showSubtitleSearch) {
                    tv.own.owntv.features.subtitles.SubtitleSearchScreen(
                        onDismiss = { showSubtitleSearch = false },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                // Local subtitle-file picker (plan §7.3) — hosted in a Dialog window, so D-pad focus
                // can't fall through to the HUD behind; picking imports a managed UTF-8 copy and
                // attaches it live to whichever engine is playing.
                if (showLocalSubPicker) {
                    tv.own.owntv.ui.components.StorageBrowser(
                        title = stringResource(R.string.content_subtitle_select_file),
                        mode = tv.own.owntv.ui.components.BrowseMode.FILE,
                        fileExtensions = setOf("srt", "ass", "ssa", "vtt", "webvtt"),
                        onPick = { file ->
                            showLocalSubPicker = false
                            scope.launch {
                                runCatching { subtitleController.applyLocal(file) }
                                    .onFailure { e ->
                                        localSubToast.show(e.message ?: subtitleLoadFailed)
                                    }
                            }
                        },
                        onDismiss = { showLocalSubPicker = false },
                    )
                }
                tv.own.owntv.ui.components.InAppToast(localSubToast)
                // Left — the playing channel's own provider category.
                if (showChannelList && isLiveChannel) {
                    if (showCategoryBrowser) {
                        // Second Left — every Live TV category.
                        tv.own.owntv.features.shell.components.CategoryBrowserOverlay(
                            categories = browserCategories,
                            currentCategoryId = previewChannel?.categoryId,
                            onSelect = { catId -> liveVm.loadChannelsForCategory(catId) },
                            onDismiss = { liveVm.hideCategoryBrowser() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else if (zapChannels.isNotEmpty()) {
                        // First Left — the channels of the current category. A browsed-to category may
                        // hold a single channel, so this renders for any non-empty list.
                        tv.own.owntv.features.shell.components.ChannelListOverlay(
                            channels = zapChannels,
                            currentId = previewChannel?.id,
                            nowPlaying = overlayNowPlaying,
                            title = zapOverlayTitle,
                            showNumbers = directTuneEnabled,
                            onSelect = { liveVm.ensurePlaying(it); showChannelList = false },
                            onDismiss = { showChannelList = false },
                            onOpenCategories = { liveVm.showCategories() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                // Right — recently watched, to hop straight back to the previous channel.
                if (showHistoryList && isLiveChannel && historyChannels.isNotEmpty()) {
                    tv.own.owntv.features.shell.components.ChannelListOverlay(
                        channels = historyChannels,
                        currentId = previewChannel?.id,
                        nowPlaying = historyNowPlaying,
                        title = stringResource(R.string.content_history),
                        showNumbers = directTuneEnabled,
                        alignEnd = true,
                        onSelect = { liveVm.ensurePlaying(it); showHistoryList = false },
                        onDismiss = { showHistoryList = false },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                MiniPlayer(
                    player = if (liveOnExo) liveVm.previewEngine else mpvEngine,
                    onExpand = expandPlayer,
                    onClose = exitPlayer,
                    onCycleSize = { scope.launch { settingsRepo.setMiniPlayerSizePct(tv.own.owntv.player.MiniPlayerSize.next(miniSizePct)) } },
                    onCyclePosition = { scope.launch { settingsRepo.setMiniPlayerPosition(miniPos.next().name) } },
                    onAudioMode = toAudioMode,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
      }

        if (showExit) {
            ExitDialog(onConfirm = onExitApp, onDismiss = dismissExit)
        }
        if (showAvatarPicker) {
            AvatarPickerDialog(
                selectedId = avatarId,
                onSelect = onSetAvatar,
                onDismiss = { showAvatarPicker = false },
            )
        }
        // Automatic update check (GitHub Releases) shortly after launch, once per session: a small
        // top-right status card shows "Checking… / up to date" (auto-hides) or stays with
        // Update now / Later when a release is newer. Hidden while in Settings (its manual
        // "Check for updates" dialog drives the same state machine) and during playback.
        // Interrupted restore (B2): the marker outlives the process, so if it's still set at launch
        // the last restore didn't complete. Acknowledging clears it.
        val restoreSettings = koinInject<tv.own.owntv.features.settings.data.SettingsRepository>()
        val incompleteRestore by restoreSettings.restoreInProgress.collectAsStateWithLifecycle(initialValue = null)
        var restoreNoticeDismissed by remember { mutableStateOf(false) }
        incompleteRestore?.takeIf { !restoreNoticeDismissed }?.let { description ->
            IncompleteRestoreDialog(
                description = description,
                onDismiss = {
                    restoreNoticeDismissed = true
                    scope.launch { restoreSettings.clearRestoreMarker() }
                },
            )
        }

        val updateManager = koinInject<UpdateManager>()
        var showStartupToast by remember { mutableStateOf(false) }
        var showChangelog by remember { mutableStateOf(false) }
        val settingsRepo = koinInject<tv.own.owntv.features.settings.data.SettingsRepository>()
        val updateCheckOnStart by settingsRepo.updateCheckOnStart.collectAsStateWithLifecycle(initialValue = false)
        LaunchedEffect(updateCheckOnStart) {
            if (updateCheckOnStart && !showStartupToast) {
                kotlinx.coroutines.delay(5_000)
                showStartupToast = true
                updateManager.check()
            }
        }
        if (showChangelog) {
            // Full "What's New" changelog (same dialog the manual Settings check uses), shown when
            // the startup card's "What's New" is pressed. No re-check — the release is already loaded.
            UpdateDialog(onDismiss = { showChangelog = false; showStartupToast = false; updateManager.reset() }, checkOnOpen = false)
        } else if (showStartupToast && selectedSection != MainSection.SETTINGS && playerMode == PlayerMode.NONE) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                UpdateStatusToast(
                    onDone = { showStartupToast = false; updateManager.reset() },
                    onViewChangelog = { showChangelog = true },
                )
            }
        }
    }
}

/** A thin bar shown above the browse UI when the device loses internet. */
@Composable
private fun OfflineBanner() {
    val colors = OwnTVTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.tertiaryContainer)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.content_offline_banner),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onTertiaryContainer,
        )
    }
}

/**
 * Bottom-right brand watermark (design spec §3) — the play-glyph + "OwnTV" wordmark row. Same shape
 * as `ui/components/BrandLockup` (the treatment Setup's onboarding uses) and the old Sidebar's
 * `AppLogo` mark, but tinted with [OwnTVTheme.colors.primary] instead of BrandLockup's fixed
 * `AccentCyan` so the watermark follows the user's chosen accent color like the rest of the shell's
 * chrome (matching the old `AppLogo`, which used `colors.primary` too) — not the orphaned
 * `R.drawable.owntv_wordmark` PNG, which has a near-white "Own" that vanishes on AMOLED black.
 * Non-focusable (no focus targets), decorative-only, drawn above content and below dialogs/player.
 */
@Composable
private fun ShellWatermark(modifier: Modifier = Modifier) {
    val colors = OwnTVTheme.colors
    val own = stringResource(R.string.brand_own)
    val tv = stringResource(R.string.brand_tv)
    val markShape = RoundedCornerShape(percent = 28)
    val markSize = 28.dp
    Row(
        modifier = modifier
            .padding(end = Dimens.GapLarge, bottom = Dimens.GapLarge)
            .alpha(0.13f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(markSize)
                .clip(markShape)
                .background(colors.card)
                .border(2.dp, colors.primary, markShape),
            contentAlignment = Alignment.Center,
        ) {
            OwnTVIcon(icon = OwnTVIcon.PLAY, tint = colors.primary, filled = true, modifier = Modifier.size(markSize * 0.5f))
        }
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = colors.textPrimary, fontWeight = FontWeight.Bold)) { append(own) }
                withStyle(SpanStyle(color = colors.primary, fontWeight = FontWeight.Bold)) { append(tv) }
            },
            fontSize = 20.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val MainSection.emptyIcon: OwnTVIcon
    get() = when (this) {
        MainSection.SEARCH -> OwnTVIcon.SEARCH
        MainSection.HOME -> OwnTVIcon.HOME
        MainSection.LIVE_TV -> OwnTVIcon.LIVE_TV
        MainSection.MOVIES -> OwnTVIcon.MOVIES
        MainSection.SERIES -> OwnTVIcon.SERIES
        MainSection.DOWNLOADS -> OwnTVIcon.DOWNLOADS
        MainSection.EPG -> OwnTVIcon.EPG
        MainSection.SETTINGS -> OwnTVIcon.SETTINGS
    }

private fun railCategoriesFor(section: MainSection): List<RailCategory> = when (section) {
    MainSection.SEARCH -> emptyList()
    MainSection.HOME -> emptyList()
    MainSection.EPG -> emptyList()
    MainSection.LIVE_TV -> listOf(
        RailCategory("Favorites", OwnTVIcon.FAVORITE, tv.own.owntv.R.string.content_category_favorites),
        RailCategory("History", OwnTVIcon.HISTORY, tv.own.owntv.R.string.content_category_history),
        RailCategory("All Channels", labelRes = tv.own.owntv.R.string.content_category_all_channels, showGenreDot = false),
        RailCategory("United Kingdom", labelRes = tv.own.owntv.R.string.content_category_united_kingdom),
        RailCategory("United States", labelRes = tv.own.owntv.R.string.content_category_united_states),
        RailCategory("Germany", labelRes = tv.own.owntv.R.string.content_category_germany),
        RailCategory("Sports", labelRes = tv.own.owntv.R.string.content_category_sports),
    )
    MainSection.MOVIES -> listOf(
        RailCategory("Favorites", OwnTVIcon.FAVORITE, tv.own.owntv.R.string.content_category_favorites),
        RailCategory("History", OwnTVIcon.HISTORY, tv.own.owntv.R.string.content_category_history),
        RailCategory("All Movies", labelRes = tv.own.owntv.R.string.content_category_all_movies, showGenreDot = false),
        RailCategory("Action", labelRes = tv.own.owntv.R.string.content_category_action),
        RailCategory("Drama", labelRes = tv.own.owntv.R.string.content_category_drama),
        RailCategory("Comedy", labelRes = tv.own.owntv.R.string.content_category_comedy),
        RailCategory("Horror", labelRes = tv.own.owntv.R.string.content_category_horror),
    )
    MainSection.SERIES -> listOf(
        RailCategory("Favorites", OwnTVIcon.FAVORITE, tv.own.owntv.R.string.content_category_favorites),
        RailCategory("History", OwnTVIcon.HISTORY, tv.own.owntv.R.string.content_category_history),
        RailCategory("All Series", labelRes = tv.own.owntv.R.string.content_category_all_series, showGenreDot = false),
        RailCategory("Drama", labelRes = tv.own.owntv.R.string.content_category_drama),
        RailCategory("Action", labelRes = tv.own.owntv.R.string.content_category_action),
        RailCategory("Animation", labelRes = tv.own.owntv.R.string.content_category_animation),
        RailCategory("Documentary", labelRes = tv.own.owntv.R.string.content_category_documentary),
    )
    MainSection.DOWNLOADS -> listOf(
        RailCategory("All Downloads", labelRes = tv.own.owntv.R.string.content_category_all_downloads, showGenreDot = false),
        RailCategory("Movies", labelRes = tv.own.owntv.R.string.content_category_movies),
        RailCategory("Series", labelRes = tv.own.owntv.R.string.content_category_series),
    )
    MainSection.SETTINGS -> emptyList()
}

@Composable
private fun placeholderCount(section: MainSection): String = when (section) {
    MainSection.SEARCH, MainSection.HOME, MainSection.EPG, MainSection.SETTINGS -> ""
    MainSection.LIVE_TV -> stringResource(R.string.content_zero_channels)
    MainSection.MOVIES -> stringResource(R.string.content_zero_movies)
    MainSection.SERIES -> stringResource(R.string.content_zero_series)
    MainSection.DOWNLOADS -> stringResource(R.string.content_zero_downloads)
}
