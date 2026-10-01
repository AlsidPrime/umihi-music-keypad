package ca.ilianokokoro.umihi.music.ui.screens.settings

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.DiagnosticLog
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.PreferenceKeys
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.UpdateChannel
import ca.ilianokokoro.umihi.music.models.UmihiSettings
import ca.ilianokokoro.umihi.music.models.enums.AudioQuality
import ca.ilianokokoro.umihi.music.models.enums.ThemeMode
import ca.ilianokokoro.umihi.music.ui.components.keypad.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun audioQualityLabel(quality: AudioQuality): String = stringResource(when (quality) {
    AudioQuality.BEST -> R.string.keypad_quality_best
    AudioQuality.BALANCED -> R.string.keypad_quality_balanced
    AudioQuality.DATA_SAVER -> R.string.keypad_quality_saver
})

@Composable
internal fun KeypadAudioQuality(downloads: Boolean, selected: AudioQuality,
    onChange: (AudioQuality) -> Unit, onClose: () -> Unit) {
    val title = stringResource(if (downloads) R.string.keypad_download_quality else R.string.keypad_streaming_quality)
    var showInfo by remember { mutableStateOf(false) }
    KeypadDialog(title, buildList {
        AudioQuality.entries.forEach { quality ->
            add(KeypadEntry(quality.name, (if (quality == selected) "✓ " else "") + audioQualityLabel(quality)) {
                onChange(quality)
            })
        }
        add(KeypadEntry("info", stringResource(R.string.keypad_details)) { showInfo = true })
        add(KeypadEntry("cancel", stringResource(R.string.cancel), onClick = onClose))
    }, onClose, initialKey = selected.name, playbackShortcuts = true)
    if (showInfo) KeypadTextDialog(title, stringResource(R.string.keypad_quality_help), { showInfo = false })
}

@Composable
internal fun KeypadSettingsDialogs(state: SettingsState, settings: UmihiSettings, viewModel: SettingsViewModel,
    onPickFolder: (Uri?) -> Unit) {
    when {
        state.showThemeSelectorSheet -> {
            fun close() { viewModel.updateShowThemeSelectorSheet(false) }
            KeypadDialog(stringResource(R.string.choose_theme), buildList {
                ThemeMode.entries.forEach { theme ->
                    val label = stringResource(when (theme) {
                        ThemeMode.SYSTEM -> R.string.theme_system
                        ThemeMode.DARK -> R.string.theme_dark
                        ThemeMode.LIGHT -> R.string.theme_light
                    })
                    add(KeypadEntry(theme.name, (if (theme == settings.themeMode) "✓ " else "") + label) {
                        viewModel.updateSetting(PreferenceKeys.THEME_MODE, theme.name); close()
                    })
                }
                add(KeypadEntry("cancel", stringResource(R.string.cancel), onClick = ::close))
            }, ::close, initialKey = settings.themeMode.name, playbackShortcuts = true)
        }
        state.showUpdateChannelSheet -> {
            fun close() { viewModel.updateShowUpdateChannelSheet(false) }
            KeypadDialog(stringResource(R.string.change_update_channel), buildList {
                UpdateChannel.entries.forEach { channel ->
                    add(KeypadEntry(channel.name, (if (channel == settings.updateChannel) "✓ " else "") + channel.name) {
                        viewModel.updateSetting(PreferenceKeys.UPDATE_CHANNEL, channel.name); close()
                    })
                }
                add(KeypadEntry("cancel", stringResource(R.string.cancel), onClick = ::close))
            }, ::close, initialKey = settings.updateChannel.name, playbackShortcuts = true)
        }
        state.showDownloadDeleteConfirm -> KeypadSettingsConfirm(R.string.download_clear_confirm_title,
            R.string.download_clear_confirm_text, onClose = { viewModel.updateShowDownloadDeleteConfirm(false) }) {
            viewModel.clearDownloads(); viewModel.updateShowDownloadDeleteConfirm(false)
        }
        state.showCacheSizeInputSheet -> KeypadCacheSize(state.cacheTypeForInput,
            if (state.cacheTypeForInput == CacheType.AUDIO) settings.exoPlayerCacheSizeMB else settings.thumbnailCacheSizeMB,
            onSave = { viewModel.saveCacheSize(it, state.cacheTypeForInput) },
            onClose = { viewModel.updateShowCacheSizeInputSheet(false) })
        state.showCacheClearConfirm -> KeypadSettingsConfirm(R.string.clear_cache, R.string.clear_cache_message,
            onClose = { viewModel.updateShowCacheClearConfirm(false) }) {
            viewModel.clearCache(); viewModel.updateShowCacheClearConfirm(false)
        }
        state.showLoginClearConfirm -> KeypadSettingsConfirm(R.string.clear_login_info, R.string.clear_login_confirm_message,
            onClose = { viewModel.updateShowLoginClearConfirm(false) }) {
            viewModel.clearLogins(); viewModel.updateShowLoginClearConfirm(false)
        }
        state.showHiddenPlaylistsSheet -> KeypadDialog(stringResource(R.string.hidden_playlists), buildList {
            state.hiddenPlaylists.forEach { playlist ->
                add(KeypadEntry(playlist.info.id, playlist.info.title, stringResource(R.string.unhide_playlist)) {
                    viewModel.unhidePlaylist(playlist)
                })
            }
            add(KeypadEntry("close", stringResource(R.string.close)) { viewModel.updateShowHiddenPlaylistsSheet(false) })
        }, { viewModel.updateShowHiddenPlaylistsSheet(false) },
            message = if (state.hiddenPlaylists.isEmpty()) stringResource(R.string.no_hidden_playlists) else null,
            playbackShortcuts = true)
        state.showDiagnosticsLogsSheet -> KeypadDiagnostics { viewModel.updateShowDiagnosticsLogsSheet(false) }
        state.showDownloadLocationDialog -> {
            fun close() { viewModel.updateShowDownloadLocationDialog(false) }
            KeypadTextDialog(stringResource(R.string.change_download_location),
                stringResource(R.string.download_location_warning_description), ::close, listOf(
                    KeypadEntry("cancel", stringResource(R.string.cancel), onClick = ::close),
                    KeypadEntry("reset", stringResource(R.string.keypad_use_internal)) { viewModel.resetDownloadLocation(); close() },
                    KeypadEntry("choose", stringResource(R.string.keypad_choose_folder)) { onPickFolder(settings.downloadLocation); close() },
                ))
        }
        state.showShortcutsPrompt -> {
            val context = LocalContext.current
            fun close() { viewModel.updateShowShortcutsPrompt(false) }
            val enabled = settings.backgroundShortcuts
            val connected by ca.ilianokokoro.umihi.music.services.KeypadAccessibilityService.connectionState.collectAsState()
            KeypadTextDialog(stringResource(R.string.background_shortcuts_title),
                stringResource(R.string.background_shortcuts_description) + "\n\n" +
                    stringResource(if (connected) R.string.shortcuts_connected else R.string.shortcuts_disconnected), ::close, listOf(
                    KeypadEntry("cancel", stringResource(R.string.cancel), onClick = ::close),
                    KeypadEntry("toggle", stringResource(if (enabled) R.string.keypad_off else R.string.keypad_on)) {
                        viewModel.updateSetting(PreferenceKeys.BACKGROUND_SHORTCUTS, !enabled)
                        close()
                    },
                    KeypadEntry("settings", stringResource(R.string.accessibility_settings)) {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }.onFailure {
                            android.widget.Toast.makeText(context, R.string.shortcuts_settings_unavailable, android.widget.Toast.LENGTH_LONG).show()
                        }
                        close()
                    },
                ))
        }
    }
}

@Composable
private fun KeypadSettingsConfirm(titleRes: Int, textRes: Int, onClose: () -> Unit, onConfirm: () -> Unit) {
    KeypadTextDialog(stringResource(titleRes), stringResource(textRes), onClose, listOf(
        KeypadEntry("cancel", stringResource(R.string.cancel), onClick = onClose),
        KeypadEntry("confirm", stringResource(R.string.confirm), onClick = onConfirm),
    ))
}

@Composable
private fun KeypadCacheSize(type: CacheType, initial: Int, onSave: (Int) -> Unit, onClose: () -> Unit) {
    val audio = type == CacheType.AUDIO
    val min = if (audio) Constants.Cache.Audio.MIN_SIZE_MB else Constants.Cache.Thumbnail.MIN_SIZE_MB
    val max = if (audio) Constants.Cache.Audio.MAX_SIZE_MB else Constants.Cache.Thumbnail.MAX_SIZE_MB
    val step = if (audio) Constants.Cache.Audio.STEP_MB else Constants.Cache.Thumbnail.STEP_MB
    var value by rememberSaveable(type) { mutableIntStateOf(initial.coerceIn(min, max)) }
    KeypadDialog(stringResource(if (audio) R.string.exoplayer_cache_title else R.string.thumbnail_cache_title), buildList {
        add(KeypadEntry("less", stringResource(R.string.keypad_cache_less, step), enabled = value > min) {
            value = (value - step).coerceAtLeast(min)
        })
        add(KeypadEntry("more", stringResource(R.string.keypad_cache_more, step), enabled = value < max) {
            value = (value + step).coerceAtMost(max)
        })
        (if (audio) listOf(100, 500, 1000, 2000) else listOf(20, 60, 100, 200, 500)).forEach { preset ->
            add(KeypadEntry("preset:$preset", stringResource(R.string.cache_size_mb, preset)) { value = preset })
        }
        add(KeypadEntry("save", stringResource(R.string.keypad_save), stringResource(R.string.cache_size_mb, value)) { onSave(value) })
        add(KeypadEntry("cancel", stringResource(R.string.cancel), onClick = onClose))
    }, onClose, message = stringResource(R.string.cache_size_mb, value) + "\n" + stringResource(R.string.keypad_cache_restart),
        playbackShortcuts = true)
}

@Composable
private fun KeypadDiagnostics(onClose: () -> Unit) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { content = withContext(Dispatchers.IO) { DiagnosticLog.getLastLines() } }
    KeypadTextDialog(stringResource(R.string.show_logs), content, onClose, listOf(
        KeypadEntry("close", stringResource(R.string.close), onClick = onClose),
        KeypadEntry("export", stringResource(R.string.export_logs)) { DiagnosticLog.share(context) },
        KeypadEntry("clear", stringResource(R.string.clear_logs)) { DiagnosticLog.clear(); content = "" },
    ))
}
