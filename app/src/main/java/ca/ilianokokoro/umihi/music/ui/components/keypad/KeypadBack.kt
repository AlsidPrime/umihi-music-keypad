package ca.ilianokokoro.umihi.music.ui.components.keypad

import android.view.KeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent

/** Only edge controls opt in; interior controls keep normal Left focus movement. */
internal fun Modifier.keypadBackAtLeftEdge(enabled: Boolean = true, onBack: () -> Unit): Modifier =
    onPreviewKeyEvent { event ->
        val key = event.nativeKeyEvent
        if (!enabled || !key.hasNoModifiers() || key.keyCode != KeyEvent.KEYCODE_DPAD_LEFT) {
            return@onPreviewKeyEvent false
        }
        // Holding Left to move across a row must not also dismiss the screen.
        if (key.action == KeyEvent.ACTION_DOWN && key.repeatCount == 0) onBack()
        true
    }
