package ca.ilianokokoro.umihi.music.ui.components.keypad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun KeypadDialog(title: String, entries: List<KeypadEntry>, onDismiss: () -> Unit, message: String? = null) {
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            KeypadList(entries, Modifier.weight(1f).fillMaxWidth(), registerNavigation = false,
                onBoundary = {}, onLeft = onDismiss)
        }
    }
}
