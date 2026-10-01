package ca.ilianokokoro.umihi.music.ui.screens.settings

import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ca.ilianokokoro.umihi.music.BuildConfig
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.DiagnosticLog
import ca.ilianokokoro.umihi.music.core.managers.VersionManager
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.PreferenceKeys
import ca.ilianokokoro.umihi.music.extensions.folderDisplayPath
import ca.ilianokokoro.umihi.music.models.enums.ThemeMode
import ca.ilianokokoro.umihi.music.ui.components.keypad.*

private enum class SettingsPage { DOWNLOADS, PLAYBACK, STORAGE, ACCOUNT, GENERAL, INFO, UPDATES, DIAGNOSTICS }

@Composable
internal fun KeypadSettingsScreen(state: SettingsState, viewModel: SettingsViewModel,
    onBack: () -> Unit, onOpenAuth: () -> Unit, onPickFolder: (Uri?) -> Unit) {
    val context = LocalContext.current
    var showFrontDiagnostics by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    var qualityForDownloads by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var explanation by remember { mutableStateOf<Pair<String, String>?>(null) }
    val stateHolder = rememberSaveableStateHolder()
    val listFocus = remember { FocusRequester() }
    val backFocus = remember { FocusRequester() }
    val settings = (state.screenState as? ScreenState.Success)?.settings
    val modalOpen = showFrontDiagnostics || qualityForDownloads != null || explanation != null || state.showThemeSelectorSheet ||
        state.showUpdateChannelSheet || state.showDownloadDeleteConfirm || state.showCacheSizeInputSheet ||
        state.showCacheClearConfirm || state.showLoginClearConfirm || state.showHiddenPlaylistsSheet ||
        state.showDiagnosticsLogsSheet || state.showDownloadLocationDialog || state.showShortcutsPrompt
    fun goBack() { if (page != null) page = null else onBack() }
    BackHandler(enabled = !modalOpen, onBack = ::goBack)
    fun explain(title: String, text: String) { explanation = title to text }
    val pageLabels = mapOf(SettingsPage.DOWNLOADS to R.string.keypad_downloads,
        SettingsPage.PLAYBACK to R.string.playback, SettingsPage.STORAGE to R.string.keypad_storage,
        SettingsPage.ACCOUNT to R.string.account, SettingsPage.GENERAL to R.string.general,
        SettingsPage.INFO to R.string.app_info, SettingsPage.UPDATES to R.string.updates,
        SettingsPage.DIAGNOSTICS to R.string.diagnostics)

    val entries = when {
        state.screenState is ScreenState.Error -> listOf(KeypadEntry("retry", stringResource(R.string.retry),
            onClick = viewModel::getSettings))
        settings == null -> emptyList()
        page == null -> buildList {
            pageLabels.forEach { (destination, label) ->
                if (destination == SettingsPage.UPDATES && !BuildConfig.UPDATER_ENABLED) return@forEach
                if (destination == SettingsPage.DIAGNOSTICS && BuildConfig.BUILD_TYPE != DiagnosticLog.DIAGNOSTIC_BUILD_TYPE) return@forEach
                add(KeypadEntry(destination.name, stringResource(label)) { page = destination })
            }
        }
        else -> buildList {
            when (page) {
                SettingsPage.DOWNLOADS -> {
                    add(toggleSetting("offline", R.string.offline_mode_title, R.string.offline_mode_description,
                        settings.offlineMode, ::explain, viewModel::updateOfflineModeSetting))
                    add(toggleSetting("metered", R.string.download_on_metered_title, R.string.download_on_metered_description,
                        settings.downloadOnMetered, ::explain) { viewModel.updateSetting(PreferenceKeys.DOWNLOAD_ON_METERED, it) })
                    add(KeypadEntry("quality", stringResource(R.string.keypad_download_quality),
                        audioQualityLabel(settings.downloadQuality)) { qualityForDownloads = true })
                    val location = runCatching { settings.downloadLocation?.folderDisplayPath() }.getOrNull()
                        ?: stringResource(R.string.internal)
                    add(KeypadEntry("location", stringResource(R.string.change_download_location), location) {
                        viewModel.updateShowDownloadLocationDialog(true)
                    })
                    add(KeypadEntry("delete", stringResource(R.string.delete_downloads),
                        Formatter.formatShortFileSize(context, state.downloadsUsage.audioBytes)) {
                        viewModel.updateShowDownloadDeleteConfirm(true)
                    })
                }
                SettingsPage.PLAYBACK -> {
                    add(KeypadEntry("quality", stringResource(R.string.keypad_streaming_quality),
                        audioQualityLabel(settings.streamingQuality)) { qualityForDownloads = false })
                    add(toggleSetting("offload", R.string.enable_audio_offload, R.string.audio_offload_subtitle,
                        settings.useAudioOffload, ::explain, viewModel::updateAudioOffloadSetting))
                    add(toggleSetting("history", R.string.send_playback_data_title, R.string.send_playback_data_description,
                        settings.sendPlaybackData, ::explain) { viewModel.updateSetting(PreferenceKeys.SEND_PLAYBACK_DATA, it) })
                }
                SettingsPage.STORAGE -> {
                    add(KeypadEntry("audio", stringResource(R.string.exoplayer_cache_title),
                        stringResource(R.string.cache_used_state, Formatter.formatShortFileSize(context, state.audioCacheUsed),
                            settings.exoPlayerCacheSizeMB)) { viewModel.updateShowCacheSizeInputSheet(true, CacheType.AUDIO) })
                    add(KeypadEntry("images", stringResource(R.string.thumbnail_cache_title),
                        stringResource(R.string.cache_used_state, Formatter.formatShortFileSize(context, state.thumbnailCacheUsed),
                            settings.thumbnailCacheSizeMB)) { viewModel.updateShowCacheSizeInputSheet(true, CacheType.THUMBNAIL) })
                    add(KeypadEntry("clear", stringResource(R.string.clear_cache)) { viewModel.updateShowCacheClearConfirm(true) })
                    add(KeypadEntry("refresh", stringResource(R.string.keypad_refresh), onClick = viewModel::refreshStorageUsage))
                }
                SettingsPage.ACCOUNT -> {
                    val loggedIn = !settings.cookies.isEmpty()
                    add(KeypadEntry("login", stringResource(if (loggedIn) R.string.log_out else R.string.log_in),
                        stringResource(if (loggedIn) R.string.logged_in_message else R.string.logged_out_message),
                        onClick = if (loggedIn) viewModel::logOut else onOpenAuth))
                    add(KeypadEntry("clear", stringResource(R.string.clear_login_info)) { viewModel.updateShowLoginClearConfirm(true) })
                }
                SettingsPage.GENERAL -> {
                    add(KeypadEntry("theme", stringResource(R.string.theme), stringResource(when (settings.themeMode) {
                        ThemeMode.DARK -> R.string.theme_dark; ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.SYSTEM -> R.string.theme_system
                    })) { viewModel.updateShowThemeSelectorSheet(true) })
                    add(KeypadEntry("hidden", stringResource(R.string.show_hidden_playlists_title),
                        state.hiddenPlaylists.size.toString()) { viewModel.updateShowHiddenPlaylistsSheet(true) })
                    add(toggleSetting("awake", R.string.keep_screen_on_title, R.string.keep_screen_on_title_description,
                        settings.keepScreenOn, ::explain, viewModel::updateKeepScreenOnSetting))
                    add(KeypadEntry("shortcuts", stringResource(R.string.background_shortcuts_title),
                        stringResource(if (settings.backgroundShortcuts) R.string.keypad_on else R.string.keypad_off)) {
                        viewModel.updateShowShortcutsPrompt(true)
                    })
                }
                SettingsPage.INFO -> {
                    add(KeypadEntry("front-display", stringResource(R.string.front_display_diagnostics)) { showFrontDiagnostics = true })
                    val title = stringResource(R.string.current_version)
                    val version = VersionManager.getVersionName()
                    add(KeypadEntry("version", title, version) { explain(title, version) })
                }
                SettingsPage.UPDATES -> {
                    add(KeypadEntry("check", stringResource(R.string.check_for_updates), onClick = viewModel::checkForUpdates))
                    add(toggleSetting("auto", R.string.auto_update_title, R.string.auto_update_subtitle,
                        settings.updateChecking, ::explain) { viewModel.updateSetting(PreferenceKeys.AUTO_UPDATE, it) })
                    add(KeypadEntry("channel", stringResource(R.string.change_update_channel), settings.updateChannel.toString()) {
                        viewModel.updateShowUpdateChannelSheet(true)
                    })
                }
                SettingsPage.DIAGNOSTICS -> add(KeypadEntry("logs", stringResource(R.string.show_logs)) {
                    viewModel.updateShowDiagnosticsLogsSheet(true)
                })
                null -> Unit
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeypadButton(stringResource(R.string.keypad_back), ::goBack,
                Modifier.width(64.dp).keypadPlayback().keypadBackAtLeftEdge(onBack = ::goBack)
                    .focusRequester(backFocus).focusProperties {
                    down = listFocus; right = listFocus; up = FocusRequester.Cancel; left = FocusRequester.Cancel
                })
            Text(stringResource(page?.let { pageLabels.getValue(it) } ?: R.string.settings),
                style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        }
        Text(stringResource(when (state.screenState) {
            ScreenState.Loading -> R.string.keypad_loading
            is ScreenState.Error -> R.string.keypad_load_failed
            is ScreenState.Success -> R.string.keypad_settings_hint
        }), style = MaterialTheme.typography.labelSmall)
        stateHolder.SaveableStateProvider(page?.name ?: "settings_root") {
            KeypadList(entries, Modifier.weight(1f).fillMaxWidth(), focusRequester = listFocus,
                onLeft = ::goBack, onUpBoundary = { backFocus.requestFocus() }, playbackShortcuts = true)
        }
    }

    if (settings != null) {
        KeypadSettingsDialogs(state, settings, viewModel, onPickFolder)
        qualityForDownloads?.let { downloads ->
            KeypadAudioQuality(downloads, if (downloads) settings.downloadQuality else settings.streamingQuality,
                onChange = { quality ->
                    viewModel.updateSetting(if (downloads) PreferenceKeys.DOWNLOAD_QUALITY else PreferenceKeys.STREAMING_QUALITY, quality.name)
                    qualityForDownloads = null
                }, onClose = { qualityForDownloads = null })
        }
    }
    if (showFrontDiagnostics) ca.ilianokokoro.umihi.music.ui.screens.diagnostics.FrontScreenDiagnostics { showFrontDiagnostics = false }
    explanation?.let { (title, text) -> KeypadTextDialog(title, text, { explanation = null }) }
}

@Composable
private fun toggleSetting(key: String, titleRes: Int, descriptionRes: Int, value: Boolean,
    onInfo: (String, String) -> Unit, onChange: (Boolean) -> Unit): KeypadEntry {
    val title = stringResource(titleRes)
    val description = stringResource(descriptionRes)
    return KeypadEntry(key, title, stringResource(R.string.keypad_toggle_state,
        stringResource(if (value) R.string.keypad_on else R.string.keypad_off)),
        onOptions = { onInfo(title, description) }) { onChange(!value) }
}
