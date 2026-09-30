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

@Composable
internal fun KeypadNavigationBar(focus: KeypadNavigationFocus, onHome: () -> Unit,
    onSearch: () -> Unit, onSettings: () -> Unit, onPlayer: () -> Unit) {
    val targets = remember(focus) { listOf(focus.navigation, FocusRequester(), FocusRequester(), FocusRequester()) }
    val actions = listOf(R.string.home to onHome, R.string.search to onSearch,
        R.string.settings to onSettings, R.string.keypad_player to onPlayer)
    Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        actions.forEachIndexed { index, (label, action) ->
            KeypadButton(stringResource(label), action, Modifier.weight(1f)
                .focusRequester(targets[index]).focusProperties {
                    left = targets.getOrNull(index - 1) ?: FocusRequester.Cancel
                    right = targets.getOrNull(index + 1) ?: FocusRequester.Cancel
                    up = focus.content ?: FocusRequester.Default
                    down = FocusRequester.Cancel
                })
        }
    }
}
