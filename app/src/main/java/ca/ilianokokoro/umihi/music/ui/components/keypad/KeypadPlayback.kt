package ca.ilianokokoro.umihi.music.ui.components.keypad

import android.view.KeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager

/** Attach only to non-editable focus scopes so TT9 keeps every number key. */
internal fun Modifier.keypadPlayback(enabled: Boolean = true): Modifier = onPreviewKeyEvent { event ->
    val key = event.nativeKeyEvent
    val action = playbackShortcut(key.keyCode) ?: return@onPreviewKeyEvent false
    if (!enabled || !key.hasNoModifiers()) return@onPreviewKeyEvent false
    if (key.action == KeyEvent.ACTION_DOWN && key.repeatCount == 0) {
        when (action) {
            PlaybackShortcut.Previous -> PlayerManager.skipToPrevious()
            PlaybackShortcut.Toggle -> PlayerManager.currentController?.let {
                if (it.playWhenReady) it.pause() else it.play()
            }
            PlaybackShortcut.Next -> PlayerManager.skipToNext()
        }
    }
    // Consume release and repeats too; holding a key must not skip several songs.
    true
}

internal enum class PlaybackShortcut { Previous, Toggle, Next }

internal fun playbackShortcut(keyCode: Int): PlaybackShortcut? = when (keyCode) {
    KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> PlaybackShortcut.Previous
    KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> PlaybackShortcut.Toggle
    KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> PlaybackShortcut.Next
    else -> null
}
