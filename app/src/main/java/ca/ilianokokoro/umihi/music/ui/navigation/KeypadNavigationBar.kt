package ca.ilianokokoro.umihi.music.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.ui.components.keypad.KeypadButton
import ca.ilianokokoro.umihi.music.ui.components.keypad.KeypadNavigationFocus
import ca.ilianokokoro.umihi.music.ui.components.keypad.keypadPlayback

@Composable
internal fun KeypadNavigationBar(focus: KeypadNavigationFocus, onHome: () -> Unit,
    onSearch: () -> Unit, onSettings: () -> Unit, onPlayer: () -> Unit) {
    val targets = remember(focus) { listOf(focus.navigation, FocusRequester(), FocusRequester(), FocusRequester()) }
    val actions = listOf(R.string.home to onHome, R.string.keypad_nav_find to onSearch,
        R.string.keypad_nav_setup to onSettings, R.string.keypad_nav_now to onPlayer)
    Row(Modifier.keypadPlayback().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        actions.forEachIndexed { index, (label, action) ->
            KeypadButton(stringResource(label), action, Modifier.weight(1f)
                .focusRequester(targets[index]).focusProperties {
                    left = targets.getOrNull(index - 1) ?: FocusRequester.Cancel
                    right = targets.getOrNull(index + 1) ?: FocusRequester.Cancel
                    up = focus.content ?: FocusRequester.Default
                    down = FocusRequester.Cancel
                }, compact = true)
        }
    }
}
