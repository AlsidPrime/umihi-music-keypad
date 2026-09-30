package ca.ilianokokoro.umihi.music.ui.screens.player

import android.app.Application
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.ui.components.keypad.isKeypadScreen
import ca.ilianokokoro.umihi.music.ui.components.keypad.keypadPlayback
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.ui.components.SquareImage
import ca.ilianokokoro.umihi.music.ui.components.bottomsheet.QueueBottomSheet
import ca.ilianokokoro.umihi.music.ui.components.bottomsheet.SleepTimerBottomSheet
import ca.ilianokokoro.umihi.music.ui.components.bottomsheet.SpeedSelectorBottomSheet
import ca.ilianokokoro.umihi.music.ui.components.bottomsheet.VolumeBottomSheet
import ca.ilianokokoro.umihi.music.ui.components.song.ExplicitBadge
import ca.ilianokokoro.umihi.music.ui.screens.player.components.PlayerControls
import ca.ilianokokoro.umihi.music.ui.screens.player.components.KeypadPlayer
import ca.ilianokokoro.umihi.music.ui.screens.player.components.TopPlayer

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    application: Application,
    playerViewModel: PlayerViewModel = viewModel(
        factory =
            PlayerViewModel.Factory(application = application)
    )
) {
    val uiState = playerViewModel.uiState.collectAsStateWithLifecycle().value
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation
    val useKeypadLayout = isKeypadScreen()
    val currentSong = uiState.queue.getOrNull(uiState.currentIndex)
    LaunchedEffect(uiState.queue.isEmpty()) {
        if (uiState.queue.isEmpty()) onBack()
    }

    // Dialogs own their input scope; no shortcuts are attached to text fields.
    val shortcutsEnabled = !uiState.isSpeedSelectorShown && !uiState.isQueueModalShown &&
        !uiState.isSleepTimerModalShown && !uiState.showVolumeDialog
    val keypadFocusRequester = remember { FocusRequester() }
    val playFocusRequester = remember { FocusRequester() }
    var keypadReturnFocus by remember(uiState.isLoggedIn) { mutableStateOf<FocusRequester?>(null) }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(shortcutsEnabled, windowFocused, useKeypadLayout, uiState.lyricsShown) {
        if (shortcutsEnabled && windowFocused) {
            if (useKeypadLayout) {
                val target = if (uiState.lyricsShown) playFocusRequester else keypadReturnFocus ?: playFocusRequester
                target.requestFocus()
            } else keypadFocusRequester.requestFocus()
        }
    }

    val playbackProgress by playerViewModel.playbackProgress.collectAsState()


    Scaffold(
        modifier = Modifier
            .keypadPlayback(shortcutsEnabled)
            .focusRequester(keypadFocusRequester)
            .focusable(enabled = shortcutsEnabled)
            .padding(
                start = 8.dp,
                end = 8.dp,
                bottom = 10.dp
            ),
        bottomBar = {
            Column {
                if (useKeypadLayout) {
                    Text(stringResource(R.string.keypad_player_navigation_hint),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(
                        R.string.keypad_previous,
                        R.string.keypad_play_pause,
                        R.string.keypad_next,
                    ).forEach { label ->
                        Text(
                            text = stringResource(label),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        if (useKeypadLayout) {
            KeypadPlayer(
                uiState = uiState,
                progress = playbackProgress,
                playerViewModel = playerViewModel,
                initialFocusRequester = playFocusRequester,
                onControlFocused = { keypadReturnFocus = it },
                onClose = onBack,
                modifier = modifier.fillMaxSize().padding(paddingValues),
            )
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            Column(
                modifier = modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .fillMaxSize()
                    .padding(paddingValues),

                horizontalAlignment = Alignment.CenterHorizontally

            ) {

                TopPlayer(
                    currentSong = currentSong,
                    isLyricsShown = uiState.lyricsShown,
                    lyricsState = uiState.lyrics,
                    positionMs = { playbackProgress.position.toLong() },
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SongInfo(
                        song = currentSong,
                        isLoggedIn = uiState.isLoggedIn,
                        isLiked = uiState.isLiked,
                        isLiking = uiState.isLiking,
                        onToggleLike = playerViewModel::toggleLike,
                    )

                    PlayerControlsSection(uiState, playerViewModel)
                    
                }
            }
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Row(
                modifier = modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    TopPlayer(
                        currentSong = currentSong,
                        isLyricsShown = uiState.lyricsShown,
                        lyricsState = uiState.lyrics,
                        positionMs = { playbackProgress.position.toLong() },
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    SongInfo(
                        song = currentSong,
                        isLoggedIn = uiState.isLoggedIn,
                        isLiked = uiState.isLiked,
                        isLiking = uiState.isLiking,

                        onToggleLike = playerViewModel::toggleLike,
                    )

                    PlayerControlsSection(uiState, playerViewModel)
                }
            }

        }
    }


    if (uiState.isSpeedSelectorShown) {
        SpeedSelectorBottomSheet(
            changeVisibility = playerViewModel::setSpeedSelectorVisibility,
            currentSpeed = uiState.playbackSpeed,
            onSelectSpeed = playerViewModel::setPlaybackSpeed,
        )
    } else if (uiState.isQueueModalShown) {
        QueueBottomSheet(
            changeVisibility = playerViewModel::setQueueVisibility,
            songs = uiState.queue,
            currentIndex = uiState.currentIndex
        )
    } else if (uiState.isSleepTimerModalShown) {
        SleepTimerBottomSheet(
            changeVisibility = playerViewModel::setSleepTimerSheetVisibility,
            activeRemainingSeconds = uiState.sleepTimerRemainingSeconds,
            onStartTimer = playerViewModel::startSleepTimer,
            onStartEndOfSong = playerViewModel::startSleepTimerEndOfSong,
            onCancelTimer = playerViewModel::cancelSleepTimer,
        )
    } else if (uiState.showVolumeDialog) {
        VolumeBottomSheet(
            changeVisibility = playerViewModel::updateShowVolumeDialog,
            currentVolume = uiState.appVolume,
            onVolumeChange = playerViewModel::setAppVolume
        )
    }
}

@Composable
fun Thumbnail(
    href: String,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        val size = minOf(maxWidth, maxHeight)

        AnimatedContent(
            targetState = href,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(Constants.Player.IMAGE_TRANSITION_DELAY)
                ).togetherWith(
                    fadeOut(
                        animationSpec = tween(Constants.Player.IMAGE_TRANSITION_DELAY)
                    )
                )
            }
        ) { targetState ->
            SquareImage(
                uri = targetState,
                modifier = Modifier.size(size)
            )
        }
    }
}

private fun Modifier.swipeUpToOpen(
    thresholdPx: Float,
    onTriggered: () -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        var accumulatedDrag = 0f
        var triggered = false
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change = event.changes.firstOrNull { it.pressed } ?: break
            if (triggered) {
                change.consume()
            } else {
                val dy = change.position.y - change.previousPosition.y
                accumulatedDrag += dy
                if (accumulatedDrag < -thresholdPx) {
                    triggered = true
                    onTriggered()
                    change.consume()
                }
            }
        } while (event.changes.any { it.pressed })
    }
}

@Composable
private fun PlayerControlsSection(
    uiState: PlayerState,
    playerViewModel: PlayerViewModel,
) {
    val density = LocalDensity.current
    val swipeUpThresholdPx = with(density) { 48.dp.toPx() }

    PlayerControls(
        isPlaying = uiState.isPlaying,
        isLoading = uiState.isLoading,
        isLyricsShown = uiState.lyricsShown,
        progress = playerViewModel.playbackProgress,
        onSeek = playerViewModel::seek,
        onSeekPlayer = playerViewModel::seekPlayer,
        onUpdateSeekBarHeldState = playerViewModel::updateSeekBarHeldState,
        onOpenQueue = { playerViewModel.setQueueVisibility(true) },
        onOpenVolume = { playerViewModel.updateShowVolumeDialog(true) },
        onOpenSleepTimer = { playerViewModel.setSleepTimerSheetVisibility(true) },
        onOpenSpeedSelector = { playerViewModel.setSpeedSelectorVisibility(true) },
        playbackSpeed = uiState.playbackSpeed,
        onToggleLyrics = playerViewModel::toggleLyrics,
        sleepTimerRemainingSeconds = uiState.sleepTimerRemainingSeconds,
        modifier = Modifier.swipeUpToOpen(swipeUpThresholdPx) {
            playerViewModel.setQueueVisibility(true)
        },
    )
}


@Composable
fun SongInfo(
    song: Song?,
    isLoggedIn: Boolean = false,
    isLiked: Boolean = false,
    isLiking: Boolean = false,
    onToggleLike: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = if (isLoggedIn) {
                Modifier.weight(1f)
            } else {
                Modifier.fillMaxWidth()
            },
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (song?.isExplicit == true) {
                    ExplicitBadge()
                }
                Text(
                    text = song?.title ?: "",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.basicMarquee()
                )
            }
            Text(
                text = song?.artist ?: "",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.basicMarquee()
            )
        }

        if (isLoggedIn) {
            Box(modifier = Modifier.padding(start = 8.dp)) {
                FilledIconToggleButton(
                    checked = isLiked,
                    onCheckedChange = {
                        if (isLiking) {
                            return@FilledIconToggleButton
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onToggleLike()
                    },
                    shapes = IconButtonDefaults.toggleableShapes(),
                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        checkedContainerColor = IconButtonDefaults.filledIconToggleButtonColors().checkedContainerColor,
                        checkedContentColor = IconButtonDefaults.filledIconToggleButtonColors().checkedContentColor,
                    ),
                ) {
                    Icon(
                        imageVector = if (isLiked) {
                            Icons.Rounded.Favorite
                        } else {
                            Icons.Rounded.FavoriteBorder
                        },
                        contentDescription = if (isLiked) {
                            stringResource(R.string.unlike)
                        } else {
                            stringResource(R.string.like)
                        }
                    )
                }
            }
        }
    }
}
