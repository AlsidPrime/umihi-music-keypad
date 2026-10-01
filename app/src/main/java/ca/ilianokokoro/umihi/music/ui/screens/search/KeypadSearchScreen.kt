package ca.ilianokokoro.umihi.music.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import android.view.KeyEvent
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.ui.components.keypad.*

@Composable
internal fun KeypadSearchScreen(state: SearchState, viewModel: SearchViewModel,
    onAddToPlaylist: (Song) -> Unit, onOpenPlayer: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val queryFocus = remember { FocusRequester() }
    val browseFocus = remember { FocusRequester() }
    var editing by rememberSaveable { mutableStateOf(state.search.isBlank()) }
    var selectionSequence by rememberSaveable { mutableIntStateOf(0) }
    var selectionRequest by remember { mutableStateOf<KeypadSelectionRequest?>(null) }
    var optionsSong by remember { mutableStateOf<Song?>(null) }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(windowFocused) {
        if (windowFocused) {
            if (editing) queryFocus.requestFocus() else browseFocus.requestFocus()
        }
    }
    fun submit() {
        editing = false
        selectionRequest = KeypadSelectionRequest("search", ++selectionSequence)
        focusManager.clearFocus()
        viewModel.search()
        browseFocus.requestFocus()
    }
    val songs = (state.screenState as? ScreenState.Success)?.results.orEmpty()
    val entries = buildList {
        add(KeypadEntry("search", stringResource(R.string.search), onClick = ::submit))
        add(KeypadEntry("edit", stringResource(R.string.keypad_edit_query)) { queryFocus.requestFocus() })
        songs.forEachIndexed { index, song ->
            add(KeypadEntry("song:${song.uid}", song.title,
                subtitle = buildList {
                    add(song.artist)
                    if (song.duration.isNotBlank()) add(song.duration)
                    if (song.downloaded) add(stringResource(R.string.downloaded))
                    if (song.isExplicit) add(stringResource(R.string.keypad_explicit))
                }.joinToString(" · "), enabled = song.isAvailable,
                onOptions = { optionsSong = song }) {
                PlayerManager.playQueue(songs.map { it.mediaItem }, startIndex = index)
                onOpenPlayer()
            })
        }
        if (state.screenState is ScreenState.Error) {
            add(KeypadEntry("retry", stringResource(R.string.retry), onClick = ::submit))
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(4.dp)) {
        Text(stringResource(R.string.search), style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(state.search, viewModel::onSearchFieldChange,
            modifier = Modifier.fillMaxWidth().focusRequester(queryFocus)
                .onFocusChanged { if (it.isFocused) editing = true }
                .focusProperties { down = browseFocus; up = FocusRequester.Cancel }
                .onPreviewKeyEvent { event ->
                    val key = event.nativeKeyEvent
                    // Down leaves TT9 entry without depending on the IME's own navigation.
                    if (key.hasNoModifiers() && key.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (key.action == KeyEvent.ACTION_DOWN && key.repeatCount == 0) {
                            editing = false
                            selectionRequest = KeypadSelectionRequest("search", ++selectionSequence)
                            focusManager.clearFocus()
                            browseFocus.requestFocus()
                        }
                        true
                    } else false
                },
            placeholder = { Text(stringResource(R.string.keypad_query), style = MaterialTheme.typography.bodySmall) },
            singleLine = true, textStyle = MaterialTheme.typography.bodySmall,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }))
        Text(stringResource(when {
            state.screenState is ScreenState.Loading -> R.string.keypad_loading
            state.screenState is ScreenState.Error -> R.string.keypad_load_failed
            state.search.isBlank() -> R.string.keypad_search_hint
            songs.isEmpty() -> R.string.no_results
            else -> R.string.keypad_song_hint
        }), style = MaterialTheme.typography.labelSmall)
        KeypadList(entries, Modifier.weight(1f).fillMaxWidth(), focusRequester = browseFocus,
            autoFocus = false, playbackShortcuts = true,
            onUpBoundary = { queryFocus.requestFocus() }, selectionRequest = selectionRequest)
    }
    optionsSong?.let { song ->
        fun act(action: () -> Unit) { optionsSong = null; action() }
        KeypadDialog(song.title, buildList {
            add(KeypadEntry("next", stringResource(R.string.play_next)) { act { PlayerManager.addNext(song, context) } })
            add(KeypadEntry("queue", stringResource(R.string.add_to_queue)) { act { PlayerManager.addToQueue(song, context) } })
            if (state.isLoggedIn) add(KeypadEntry("playlist", stringResource(R.string.add_to_playlist)) {
                act { onAddToPlaylist(song) }
            })
            add(KeypadEntry("close", stringResource(R.string.close)) { optionsSong = null })
        }, { optionsSong = null })
    }
}
