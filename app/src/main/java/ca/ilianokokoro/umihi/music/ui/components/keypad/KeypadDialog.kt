package ca.ilianokokoro.umihi.music.ui.components.keypad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import ca.ilianokokoro.umihi.music.R

@Composable
internal fun KeypadDialog(title: String, entries: List<KeypadEntry>, onDismiss: () -> Unit,
    message: String? = null, initialKey: String? = null, playbackShortcuts: Boolean = false) {
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.keypad_dialog_hint), style = MaterialTheme.typography.labelSmall)
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            KeypadList(entries, Modifier.weight(1f).fillMaxWidth(), registerNavigation = false,
                onBoundary = {}, onLeft = onDismiss, initialKey = initialKey,
                playbackShortcuts = playbackShortcuts)
        }
    }
}
