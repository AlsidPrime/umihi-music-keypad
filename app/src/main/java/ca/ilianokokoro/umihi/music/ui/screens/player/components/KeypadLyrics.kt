package ca.ilianokokoro.umihi.music.ui.screens.player.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.ui.components.keypad.KeypadButton
import ca.ilianokokoro.umihi.music.ui.components.keypad.KeypadEntry
import ca.ilianokokoro.umihi.music.ui.components.keypad.KeypadList
import ca.ilianokokoro.umihi.music.ui.screens.player.LyricsState
import ca.ilianokokoro.umihi.music.ui.screens.player.PlaybackProgress
import ca.ilianokokoro.umihi.music.ui.screens.player.PlayerState

@Composable
internal fun KeypadLyrics(uiState: PlayerState, progress: PlaybackProgress, onHide: () -> Unit,
    hideFocus: FocusRequester, modifier: Modifier) {
    BackHandler(onBack = onHide)
    val targets = remember { List(3) { FocusRequester() } }
    val lyricsFocus = remember { FocusRequester() }
    val lyrics = (uiState.lyrics as? LyricsState.Loaded)?.data
    val entries = if (lyrics?.hasSynced == true) {
        lyrics.displayLines.mapIndexed { index, line ->
            KeypadEntry("line:$index", line.text) {
                PlayerManager.currentController?.seekTo(line.timeMs.coerceAtLeast(0))
            }
        }
    } else {
        lyrics?.unsyncedLyrics?.lineSequence()?.filter { it.isNotBlank() }?.mapIndexed { index, line ->
            KeypadEntry("line:$index", line) {}
        }?.toList().orEmpty()
    }
    val currentLine = if (lyrics?.hasSynced == true) lyrics.indexOfCurrentLine(progress.position.toLong()) else -1
    Column(modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        KeypadButton(stringResource(R.string.keypad_hide_lyrics), onHide, Modifier.fillMaxWidth()
            .focusRequester(hideFocus).focusProperties {
                down = targets[1]
                up = FocusRequester.Cancel
            })
        val labels = listOf(R.string.keypad_previous,
            if (uiState.isPlaying) R.string.keypad_pause else R.string.keypad_play, R.string.keypad_next)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { index, label ->
                KeypadButton(stringResource(label), {
                    when (index) {
                        0 -> PlayerManager.skipToPrevious()
                        1 -> PlayerManager.currentController?.let { if (it.playWhenReady) it.pause() else it.play() }
                        2 -> PlayerManager.skipToNext()
                    }
                }, Modifier.weight(1f).focusRequester(targets[index]).focusProperties {
                    up = hideFocus
                    down = lyricsFocus
                    left = targets.getOrNull(index - 1) ?: FocusRequester.Cancel
                    right = targets.getOrNull(index + 1) ?: FocusRequester.Cancel
                })
            }
        }
        Text(uiState.queue.getOrNull(uiState.currentIndex)?.title.orEmpty(),
            style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (entries.isEmpty()) {
            Text(stringResource(if (uiState.lyrics is LyricsState.Unloaded) R.string.keypad_loading else R.string.no_lyrics_found),
                style = MaterialTheme.typography.bodySmall)
        }
        KeypadList(entries, Modifier.weight(1f).fillMaxWidth(), focusRequester = lyricsFocus,
            autoFocus = false, registerNavigation = false, onBoundary = { hideFocus.requestFocus() },
            onLeft = onHide, followKey = currentLine.takeIf { it >= 0 }?.let { "line:$it" })
    }
}
