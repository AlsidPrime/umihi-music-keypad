package ca.ilianokokoro.umihi.music.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.models.PlaylistInfo
import ca.ilianokokoro.umihi.music.ui.components.keypad.KeypadEntry
import ca.ilianokokoro.umihi.music.ui.components.keypad.KeypadList

@Composable
internal fun KeypadHomeScreen(state: HomeState, onPlaylist: (PlaylistInfo) -> Unit,
    onLogin: () -> Unit, onCreate: () -> Unit, onRefresh: () -> Unit, onRetry: () -> Unit) {
    val screen = state.screenState
    val playlists = (screen as? ScreenState.LoggedIn)?.playlistInfos?.filter { !it.hidden }.orEmpty()
    val entries = playlists.mapIndexed { index, playlist ->
        KeypadEntry("playlist:${playlist.id.ifBlank { index.toString() }}", playlist.title,
            subtitle = playlist.songCount?.let { stringResource(R.string.songs, it) }) { onPlaylist(playlist) }
    } + when (screen) {
        is ScreenState.LoggedIn -> listOf(
            KeypadEntry("refresh", stringResource(R.string.keypad_refresh), onClick = onRefresh),
            if (screen.isLoggedIn) KeypadEntry("create", stringResource(R.string.create_playlist), onClick = onCreate)
            else KeypadEntry("login", stringResource(R.string.log_in), onClick = onLogin),
        )
        is ScreenState.Error -> listOf(KeypadEntry("retry", stringResource(R.string.retry), onClick = onRetry))
        ScreenState.Loading -> emptyList()
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(4.dp)) {
        Text(stringResource(R.string.playlists), style = MaterialTheme.typography.titleSmall)
        val status = when {
            screen is ScreenState.Loading || state.isRefreshing -> R.string.keypad_loading
            screen is ScreenState.Error -> R.string.keypad_load_failed
            playlists.isEmpty() -> R.string.no_playlists
            else -> R.string.keypad_list_hint
        }
        Text(stringResource(status), style = MaterialTheme.typography.labelSmall)
        KeypadList(entries, Modifier.weight(1f).fillMaxWidth(), playbackShortcuts = true)
    }
}
