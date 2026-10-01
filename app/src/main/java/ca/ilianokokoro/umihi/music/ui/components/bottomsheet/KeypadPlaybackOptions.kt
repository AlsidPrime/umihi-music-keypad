package ca.ilianokokoro.umihi.music.ui.components.bottomsheet

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper.speedLabel
import ca.ilianokokoro.umihi.music.ui.components.keypad.KeypadDialog
import ca.ilianokokoro.umihi.music.ui.components.keypad.KeypadEntry

@Composable
internal fun KeypadVolume(currentVolume: Int, onChange: (Int) -> Unit, onClose: () -> Unit) {
    val min = Constants.Player.Volume.MIN_PERCENT
    val max = Constants.Player.Volume.MAX_PERCENT
    val value = currentVolume.coerceIn(min, max)
    KeypadDialog(stringResource(R.string.volume), buildList {
        add(KeypadEntry("lower", stringResource(R.string.keypad_volume_lower), enabled = value > min) {
            onChange((value - 10).coerceAtLeast(min))
        })
        add(KeypadEntry("higher", stringResource(R.string.keypad_volume_higher), enabled = value < max) {
            onChange((value + 10).coerceAtMost(max))
        })
        Constants.Player.Volume.PRESETS.forEach { preset ->
            add(KeypadEntry("preset:$preset", (if (value == preset) "✓ " else "") + "$preset%") { onChange(preset) })
        }
        add(KeypadEntry("close", stringResource(R.string.close), onClick = onClose))
    }, onClose, message = stringResource(R.string.keypad_volume_current, value), playbackShortcuts = true)
}

@Composable
internal fun KeypadSpeed(currentSpeed: Float, onChange: (Float) -> Unit, onClose: () -> Unit) {
    KeypadDialog(stringResource(R.string.playback_speed), buildList {
        Constants.Player.SPEEDS.forEach { speed ->
            add(KeypadEntry("speed:$speed",
                (if (speed == currentSpeed) "✓ " else "") + speed.speedLabel()) { onChange(speed) })
        }
        add(KeypadEntry("close", stringResource(R.string.close), onClick = onClose))
    }, onClose, initialKey = "speed:$currentSpeed", playbackShortcuts = true)
}

@Composable
internal fun KeypadSleepTimer(activeRemainingSeconds: Long?, onStart: (Int) -> Unit,
    onEndOfSong: () -> Unit, onCancel: () -> Unit, onClose: () -> Unit) {
    val step = Constants.Ui.Player.SleepTimer.STEP_VALUE
    val max = step * Constants.Ui.Player.SleepTimer.STEP_AMOUNT
    var minutes by rememberSaveable { mutableIntStateOf(Constants.Ui.Player.SleepTimer.DEFAULT_VALUE) }
    KeypadDialog(stringResource(R.string.sleep_timer), buildList {
        if (activeRemainingSeconds != null) {
            add(KeypadEntry("cancel_timer", stringResource(R.string.cancel_timer)) { onCancel(); onClose() })
        } else {
            add(KeypadEntry("shorter", stringResource(R.string.keypad_timer_shorter), enabled = minutes > step) {
                minutes = (minutes - step).coerceAtLeast(step)
            })
            add(KeypadEntry("longer", stringResource(R.string.keypad_timer_longer), enabled = minutes < max) {
                minutes = (minutes + step).coerceAtMost(max)
            })
            listOf(15, 30, 60).forEach { preset ->
                add(KeypadEntry("preset:$preset", stringResource(R.string.minutes, preset)) { minutes = preset })
            }
            add(KeypadEntry("start", stringResource(R.string.sleep_timer_start), stringResource(R.string.minutes, minutes)) {
                onStart(minutes); onClose()
            })
            add(KeypadEntry("end_song", stringResource(R.string.sleep_timer_end_of_song_btn)) { onEndOfSong(); onClose() })
        }
        add(KeypadEntry("close", stringResource(R.string.close), onClick = onClose))
    }, onClose, message = if (activeRemainingSeconds != null && activeRemainingSeconds < 0) {
        stringResource(R.string.sleep_timer_end_of_song_btn)
    } else if (activeRemainingSeconds != null) {
        stringResource(R.string.keypad_timer_remaining, activeRemainingSeconds / 60, activeRemainingSeconds % 60)
    } else stringResource(R.string.minutes, minutes), playbackShortcuts = true)
}
