package ca.ilianokokoro.umihi.music.ui.components.keypad

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ca.ilianokokoro.umihi.music.R
import kotlinx.coroutines.launch

/** Actions stay visible while long explanations and logs scroll independently. */
@Composable
internal fun KeypadTextDialog(title: String, text: String, onDismiss: () -> Unit,
    actions: List<KeypadEntry>? = null, dismissEnabled: Boolean = true) {
    val textFocus = remember { FocusRequester() }
    val actionsFocus = remember { FocusRequester() }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val step = with(LocalDensity.current) { 48.dp.toPx() }
    var textFocused by remember { mutableStateOf(false) }
    val entries = actions ?: listOf(KeypadEntry("close", stringResource(R.string.close), onClick = onDismiss))

    Dialog(onDismissRequest = { if (dismissEnabled) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false,
            dismissOnBackPress = dismissEnabled)) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).keypadPlayback().padding(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.keypad_text_read_hint), style = MaterialTheme.typography.labelSmall)
            KeypadList(entries, Modifier.fillMaxWidth().height((entries.size * 48).coerceIn(48, 128).dp),
                focusRequester = actionsFocus, registerNavigation = false,
                onLeft = { if (dismissEnabled) onDismiss() }, onBoundary = { textFocus.requestFocus() })
            Box(Modifier.weight(1f).fillMaxWidth()
                .border(if (textFocused) 3.dp else 1.dp,
                    if (textFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                .keypadBackAtLeftEdge(enabled = dismissEnabled, onBack = onDismiss)
                .onPreviewKeyEvent { event ->
                    val key = event.nativeKeyEvent
                    if (!key.hasNoModifiers() || key.keyCode !in listOf(KeyEvent.KEYCODE_DPAD_UP,
                            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT,
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER)) return@onPreviewKeyEvent false
                    if (key.action == KeyEvent.ACTION_DOWN) {
                        when (key.keyCode) {
                            KeyEvent.KEYCODE_DPAD_UP -> if (scroll.value == 0) actionsFocus.requestFocus()
                                else scope.launch { scroll.scrollTo((scroll.value - step.toInt()).coerceAtLeast(0)) }
                            KeyEvent.KEYCODE_DPAD_DOWN -> if (scroll.value == scroll.maxValue) actionsFocus.requestFocus()
                                else scope.launch { scroll.scrollTo((scroll.value + step.toInt()).coerceAtMost(scroll.maxValue)) }
                            else -> if (key.repeatCount == 0) actionsFocus.requestFocus()
                        }
                    }
                    true
                }
                .focusRequester(textFocus).onFocusChanged { textFocused = it.isFocused }.focusable()) {
                Text(text, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(8.dp))
            }
        }
    }
}
