package ca.ilianokokoro.umihi.music.ui.screens.playlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.models.Playlist
import ca.ilianokokoro.umihi.music.models.PlaylistInfo
import ca.ilianokokoro.umihi.music.models.PlaylistType
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.ui.components.keypad.*

private data class PlaylistConfirmation(val title: String, val message: String, val action: () -> Unit)

@Composable
internal fun KeypadPlaylistScreen(state: PlaylistState, info: PlaylistInfo, viewModel: PlaylistViewModel,
    searchFocus: FocusRequester, onBack: () -> Unit, onOpenPlayer: () -> Unit,
    onAddToPlaylist: (Song) -> Unit, onRemoveSong: (Song) -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val browseFocus = remember { FocusRequester() }
    var playlistOptions by remember { mutableStateOf(false) }
    var songOptions by remember { mutableStateOf<Song?>(null) }
    var confirmation by remember { mutableStateOf<PlaylistConfirmation?>(null) }
    val playlist = (state.screenState as? ScreenState.Success)?.playlist ?: Playlist(info)
    val songs = playlist.songs.filter {
        state.searchQuery.isBlank() || it.title.contains(state.searchQuery, true) || it.artist.contains(state.searchQuery, true)
    }
    val entries = buildList {
        if (playlist.songs.isNotEmpty()) {
            add(KeypadEntry("play", stringResource(R.string.play)) { viewModel.playPlaylist(); onOpenPlayer() })
            add(KeypadEntry("shuffle", stringResource(R.string.shuffle)) { viewModel.shufflePlaylist(); onOpenPlayer() })
        }
        add(KeypadEntry("search", stringResource(if (state.showingSearch) R.string.keypad_close_search else R.string.search)) {
            if (state.showingSearch) {
                viewModel.hideSearch()
                focusManager.clearFocus()
                browseFocus.requestFocus()
            } else viewModel.showSearch()
        })
        add(KeypadEntry("options", stringResource(R.string.keypad_playlist_options)) { playlistOptions = true })
        songs.forEach { song ->
            val details = buildList {
                add(song.artist)
                add(song.duration)
                if (song.downloaded) add(stringResource(R.string.downloaded))
                if (song.isExplicit) add(stringResource(R.string.keypad_explicit))
            }.joinToString(" · ")
            add(KeypadEntry("song:${song.uid}", song.title, details, enabled = song.isAvailable,
                onOptions = { songOptions = song }) {
                viewModel.playPlaylist(song)
                onOpenPlayer()
            })
        }
        if (state.screenState is ScreenState.Error) {
            add(KeypadEntry("retry", stringResource(R.string.retry), onClick = viewModel::getPlaylistInfo))
        }
        add(KeypadEntry("back", stringResource(R.string.keypad_back), onClick = onBack))
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(4.dp)) {
        Text(info.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(stringResource(when {
            state.screenState is ScreenState.Loading || state.isRefreshing -> R.string.keypad_loading
            state.screenState is ScreenState.Error -> R.string.keypad_load_failed
            songs.isEmpty() -> if (state.searchQuery.isBlank()) R.string.empty_playlist else R.string.no_results
            else -> R.string.keypad_song_hint
        }), style = MaterialTheme.typography.labelSmall)
        if (state.showingSearch) {
            OutlinedTextField(state.searchQuery, viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().focusRequester(searchFocus).focusProperties { down = browseFocus },
                singleLine = true, textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    browseFocus.requestFocus()
                }))
        }
        KeypadList(entries, Modifier.weight(1f).fillMaxWidth(), focusRequester = browseFocus,
            autoFocus = !state.showingSearch, onLeft = onBack, playbackShortcuts = true,
            onUpBoundary = if (state.showingSearch) ({ searchFocus.requestFocus() }) else null)
    }

    if (playlistOptions) {
        fun confirm(title: Int, message: Int, action: () -> Unit) {
            playlistOptions = false
            confirmation = PlaylistConfirmation(context.getString(title), context.getString(message), action)
        }
        val options = buildList {
            if (!info.isDownloadedPlaylist && state.screenState is ScreenState.Success && playlist.songs.isNotEmpty()) {
                when {
                    state.isDownloading -> add(KeypadEntry("cancel", stringResource(R.string.cancel_download)) {
                        confirm(R.string.cancel_playlist_download, R.string.cancel_playlist_download_text, viewModel::cancelDownload)
                    })
                    !playlist.downloaded -> add(KeypadEntry("download", stringResource(R.string.download)) {
                        playlistOptions = false
                        viewModel.downloadPlaylist()
                    })
                }
                add(KeypadEntry("remove-download", stringResource(R.string.remove_download)) {
                    confirm(R.string.remove_local_playlist, R.string.remove_local_confirm_text) { viewModel.deleteLocalPlaylist(context) }
                })
            }
            add(KeypadEntry("refresh", stringResource(R.string.keypad_refresh)) {
                playlistOptions = false
                viewModel.refreshPlaylistInfo()
            })
            if (!info.isDownloadedPlaylist) {
                add(KeypadEntry("visibility", stringResource(if (info.hidden) R.string.unhide_playlist else R.string.hide_playlist)) {
                    if (info.hidden) confirm(R.string.unhide_playlist, R.string.unhide_playlist_confirm_text, viewModel::unhidePlaylist)
                    else confirm(R.string.hide_playlist, R.string.hide_playlist_confirm_text) { viewModel.hidePlaylist(onBack) }
                })
                when (info.type) {
                    PlaylistType.CREATED_BY_USER -> add(KeypadEntry("delete", stringResource(R.string.delete_playlist)) {
                        confirm(R.string.delete_playlist, R.string.delete_playlist_text) { viewModel.deletePlaylist(onBack) }
                    })
                    PlaylistType.SAVED -> add(KeypadEntry("remove-library", stringResource(R.string.remove_library)) {
                        confirm(R.string.remove_library, R.string.remove_library_text) { viewModel.removeFromLibrary(onBack) }
                    })
                    else -> {}
                }
            }
            add(KeypadEntry("close", stringResource(R.string.close)) { playlistOptions = false })
        }
        KeypadDialog(stringResource(R.string.keypad_playlist_options), options, { playlistOptions = false })
    }
    songOptions?.let { song ->
        fun act(action: () -> Unit) { songOptions = null; action() }
        val options = buildList {
            add(KeypadEntry("next", stringResource(R.string.play_next)) { act { PlayerManager.addNext(song, context) } })
            add(KeypadEntry("queue", stringResource(R.string.add_to_queue)) { act { PlayerManager.addToQueue(song, context) } })
            if (!song.downloaded) add(KeypadEntry("download", stringResource(R.string.download)) { act { viewModel.downloadSong(song) } })
            if (state.isLoggedIn) {
                add(KeypadEntry("add", stringResource(R.string.add_to_playlist)) { act { onAddToPlaylist(song) } })
                if (viewModel.isUserEditablePlaylist) {
                    add(KeypadEntry("remove", stringResource(R.string.remove_from_playlist)) { act { onRemoveSong(song) } })
                }
            }
            add(KeypadEntry("close", stringResource(R.string.close)) { songOptions = null })
        }
        KeypadDialog(song.title, options, { songOptions = null })
    }
    confirmation?.let { pending ->
        KeypadDialog(pending.title, listOf(
            KeypadEntry("cancel", stringResource(R.string.cancel)) { confirmation = null },
            KeypadEntry("confirm", stringResource(R.string.confirm)) { confirmation = null; pending.action() },
        ), { confirmation = null }, pending.message)
    }
}
