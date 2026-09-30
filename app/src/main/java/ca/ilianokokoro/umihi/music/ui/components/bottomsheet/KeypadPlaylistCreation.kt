package ca.ilianokokoro.umihi.music.ui.components.bottomsheet

import android.view.KeyEvent
import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.models.enums.Privacy
import ca.ilianokokoro.umihi.music.ui.components.keypad.*

@Composable
internal fun KeypadPlaylistCreation(
    onConfirm: (String, String, Privacy) -> Unit,
    onClose: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var privacy by rememberSaveable { mutableStateOf(Privacy.PRIVATE) }
    var editor by rememberSaveable { mutableStateOf<String?>(null) }
    var privacyOpen by rememberSaveable { mutableStateOf(false) }

    KeypadDialog(
        title = stringResource(R.string.create_playlist),
        entries = listOf(
            KeypadEntry("title", stringResource(R.string.title),
                title.ifBlank { stringResource(R.string.keypad_required_title) }) { editor = "title" },
            KeypadEntry("description", stringResource(R.string.description),
                description.ifBlank { stringResource(R.string.keypad_optional) }) { editor = "description" },
            KeypadEntry("privacy", stringResource(R.string.visibility), stringResource(privacy.labelRes)) {
                privacyOpen = true
            },
            KeypadEntry("create", stringResource(R.string.create), enabled = title.isNotBlank()) {
                onConfirm(title.trim(), description.trim(), privacy)
            },
            KeypadEntry("cancel", stringResource(R.string.cancel), onClick = onClose),
        ),
        onDismiss = onClose,
        playbackShortcuts = true,
    )

    editor?.let { field ->
        KeypadTextEditor(
            label = stringResource(if (field == "title") R.string.title else R.string.description),
            initialValue = if (field == "title") title else description,
            onSave = {
                if (field == "title") title = it else description = it
                editor = null
            },
            onClose = { editor = null },
        )
    }
    if (privacyOpen) {
        KeypadDialog(stringResource(R.string.visibility), buildList {
            Privacy.entries.forEach { option ->
                add(KeypadEntry(option.name,
                    (if (option == privacy) "✓ " else "") + stringResource(option.labelRes)) {
                    privacy = option
                    privacyOpen = false
                })
            }
            add(KeypadEntry("cancel", stringResource(R.string.cancel)) { privacyOpen = false })
        }, { privacyOpen = false }, initialKey = privacy.name, playbackShortcuts = true)
    }
}

/** Numbers belong to TT9 here. Playback shortcuts are deliberately absent from this dialog. */
@Composable
private fun KeypadTextEditor(label: String, initialValue: String,
    onSave: (String) -> Unit, onClose: () -> Unit) {
    var value by rememberSaveable { mutableStateOf(initialValue) }
    val inputFocus = remember { FocusRequester() }
    val actionsFocus = remember { FocusRequester() }
    Dialog(onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        val focusManager = LocalFocusManager.current
        val windowFocused = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(windowFocused) { if (windowFocused) inputFocus.requestFocus() }
        fun leaveInput() {
            focusManager.clearFocus()
            actionsFocus.requestFocus()
        }
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).imePadding().padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.keypad_text_hint), style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(value, { value = it }, singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().focusRequester(inputFocus)
                    .onPreviewKeyEvent { event ->
                        val key = event.nativeKeyEvent
                        if (key.hasNoModifiers() && key.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                            if (key.action == KeyEvent.ACTION_DOWN && key.repeatCount == 0) leaveInput()
                            true
                        } else false
                    },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { leaveInput() }))
            KeypadList(listOf(
                KeypadEntry("save", stringResource(R.string.keypad_save)) { onSave(value) },
                KeypadEntry("cancel", stringResource(R.string.cancel), onClick = onClose),
            ), Modifier.weight(1f).fillMaxWidth(), autoFocus = false, registerNavigation = false,
                focusRequester = actionsFocus, onBoundary = {}, onLeft = onClose,
                onUpBoundary = { inputFocus.requestFocus() })
        }
    }
}
