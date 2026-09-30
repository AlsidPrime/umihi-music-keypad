package ca.ilianokokoro.umihi.music.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.helpers.ComposeHelper
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.extensions.toTimeString
import ca.ilianokokoro.umihi.music.ui.screens.player.PlaybackProgress
import ca.ilianokokoro.umihi.music.ui.screens.player.PlayerState
import ca.ilianokokoro.umihi.music.ui.screens.player.PlayerViewModel
import kotlinx.coroutines.launch

private data class KeypadAction(val label: String, val onClick: () -> Unit)

/** Small displays use fixed button rows instead of overflowing icon groups. */
@Composable
internal fun KeypadPlayer(
    uiState: PlayerState,
    progress: PlaybackProgress,
    playerViewModel: PlayerViewModel,
    initialFocusRequester: FocusRequester,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val song = uiState.queue.getOrNull(uiState.currentIndex)
    val context = LocalContext.current
    val controller by PlayerManager.controllerState.collectAsStateWithLifecycle()
    val repeatMode = ComposeHelper.rememberRepeatMode(controller)
    val repeatLabel = stringResource(when (repeatMode) {
        Player.REPEAT_MODE_ONE -> R.string.repeat_one
        Player.REPEAT_MODE_ALL -> R.string.repeat_all
        else -> R.string.repeat_off
    })
    val rows = listOf(
        listOf(
            KeypadAction(stringResource(R.string.keypad_previous), PlayerManager::skipToPrevious),
            KeypadAction(stringResource(if (uiState.isPlaying) R.string.keypad_pause else R.string.keypad_play)) {
                PlayerManager.currentController?.let {
                    if (it.playWhenReady) it.pause() else it.play()
                }
            },
            KeypadAction(stringResource(R.string.keypad_next), PlayerManager::skipToNext),
        ),
        listOf(
            KeypadAction(stringResource(R.string.keypad_rewind)) { seekBy(-10_000) },
            KeypadAction(stringResource(R.string.keypad_forward)) { seekBy(10_000) },
        ),
        listOf(
            KeypadAction(stringResource(R.string.queue)) { playerViewModel.setQueueVisibility(true) },
            KeypadAction(stringResource(R.string.volume)) { playerViewModel.updateShowVolumeDialog(true) },
        ),
        listOf(
            KeypadAction(stringResource(R.string.shuffle)) { PlayerManager.shuffleQueue(context) },
            KeypadAction(repeatLabel, PlayerManager::cycleRepeatMode),
        ),
        listOf(
            KeypadAction(stringResource(R.string.playback_speed)) { playerViewModel.setSpeedSelectorVisibility(true) },
            KeypadAction(stringResource(R.string.sleep_timer)) { playerViewModel.setSleepTimerSheetVisibility(true) },
        ),
        buildList {
            add(KeypadAction(stringResource(R.string.lyrics), playerViewModel::toggleLyrics))
            if (uiState.isLoggedIn) {
                add(KeypadAction(stringResource(if (uiState.isLiked) R.string.unlike else R.string.like)) {
                    if (!uiState.isLiking) playerViewModel.toggleLike()
                })
            }
        },
        listOf(KeypadAction(stringResource(R.string.keypad_close), onClose)),
    )
    // Stable requesters survive playback updates; only login changes the row shape.
    val targets = remember(uiState.isLoggedIn, initialFocusRequester) {
        rows.mapIndexed { row, actions ->
            actions.mapIndexed { column, _ ->
                if (row == 0 && column == 1) initialFocusRequester else FocusRequester()
            }
        }
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).focusGroup().padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(song?.title.orEmpty(), style = MaterialTheme.typography.titleSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song?.artist.orEmpty(), style = MaterialTheme.typography.bodySmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${progress.position.toTimeString()} / ${progress.duration.toTimeString()}",
            style = MaterialTheme.typography.labelSmall)
        rows.forEachIndexed { row, actions ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                actions.forEachIndexed { column, action ->
                    KeypadButton(
                        action = action,
                        modifier = Modifier.weight(1f)
                            .focusRequester(targets[row][column])
                            .focusProperties {
                                left = targets[row].getOrNull(column - 1) ?: FocusRequester.Cancel
                                right = targets[row].getOrNull(column + 1) ?: FocusRequester.Cancel
                                up = targets.getOrNull(row - 1)?.let { it[column.coerceAtMost(it.lastIndex)] }
                                    ?: FocusRequester.Cancel
                                down = targets.getOrNull(row + 1)?.let { it[column.coerceAtMost(it.lastIndex)] }
                                    ?: FocusRequester.Cancel
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(action: KeypadAction, modifier: Modifier) {
    var focused by remember { mutableStateOf(false) }
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 40.dp)
            .bringIntoViewRequester(bringIntoView)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) scope.launch { bringIntoView.bringIntoView() }
            }
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
            .border(if (focused) 3.dp else 1.dp,
                if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape)
            .clickable(role = Role.Button, onClick = action.onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(action.label, style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private fun seekBy(offsetMs: Long) {
    PlayerManager.currentController?.run {
        val target = (currentPosition + offsetMs).coerceAtLeast(0L)
        seekTo(if (duration > 0) target.coerceAtMost(duration) else target)
    }
}
