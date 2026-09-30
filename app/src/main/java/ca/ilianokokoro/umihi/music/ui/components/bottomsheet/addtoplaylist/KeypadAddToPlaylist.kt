package ca.ilianokokoro.umihi.music.ui.components.bottomsheet.addtoplaylist

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.ui.components.bottomsheet.PlaylistCreationBottomSheet
import ca.ilianokokoro.umihi.music.ui.components.keypad.KeypadDialog
import ca.ilianokokoro.umihi.music.ui.components.keypad.KeypadEntry

@Composable
internal fun KeypadAddToPlaylist(song: Song, state: AddToPlaylistState,
    viewModel: AddToPlaylistViewModel, onClose: () -> Unit, onStateChanged: () -> Unit) {
    var createOpen by rememberSaveable { mutableStateOf(false) }
    fun cancel() {
        if (!state.submitting) {
            viewModel.cancel()
            onClose()
        }
    }
    val success = state.screenState as? AddToPlaylistScreenState.Success
    val entries = buildList {
        if (success != null) {
            add(KeypadEntry("create", stringResource(R.string.create_playlist), enabled = !state.submitting) {
                createOpen = true
            })
            success.options.forEach { option ->
                add(KeypadEntry("playlist:${option.playlistId}",
                    (if (state.isChecked(option)) "✓ " else "○ ") + option.title,
                    subtitle = option.subtitle, enabled = !state.submitting) { viewModel.toggle(option.playlistId) })
            }
            add(KeypadEntry("confirm", stringResource(R.string.confirm),
                enabled = !state.submitting && state.hasPendingChanges) {
                viewModel.confirm(song, onStateChanged, onClose)
            })
        }
        if (state.screenState is AddToPlaylistScreenState.Error) {
            add(KeypadEntry("retry", stringResource(R.string.retry)) { viewModel.load(song.youtubeId) })
        }
        add(KeypadEntry("cancel", stringResource(R.string.cancel), enabled = !state.submitting, onClick = ::cancel))
    }
    KeypadDialog(
        title = stringResource(R.string.add_to_playlist), entries = entries, onDismiss = ::cancel,
        message = when {
            state.submitting -> stringResource(R.string.keypad_saving)
            state.screenState is AddToPlaylistScreenState.Loading -> stringResource(R.string.keypad_loading)
            state.screenState is AddToPlaylistScreenState.Error -> stringResource(R.string.keypad_load_failed)
            success?.options?.isEmpty() == true -> stringResource(R.string.no_playlists_found)
            else -> stringResource(R.string.keypad_playlist_select_hint)
        },
        playbackShortcuts = true,
    )
    if (createOpen) {
        PlaylistCreationBottomSheet(onClose = { createOpen = false }, onConfirm = { title, description, privacy ->
            createOpen = false
            viewModel.createPlaylist(title, description, privacy, onStateChanged)
        })
    }
}
