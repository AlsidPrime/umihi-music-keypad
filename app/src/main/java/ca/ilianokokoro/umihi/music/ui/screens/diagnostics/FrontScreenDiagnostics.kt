package ca.ilianokokoro.umihi.music.ui.screens.diagnostics

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.Display
import android.view.Gravity
import android.widget.TextView
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.managers.FrontDisplayManager
import ca.ilianokokoro.umihi.music.ui.components.keypad.KeypadEntry
import ca.ilianokokoro.umihi.music.ui.components.keypad.KeypadTextDialog

class FrontDisplayPresentation(outerContext: Context, display: Display) : Presentation(outerContext, display) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(context).apply {
            text = "Umihi\nDisplay test"
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 18f)
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.BLACK)
            gravity = Gravity.CENTER
        })
    }
}

@Composable
internal fun FrontScreenDiagnostics(onClose: () -> Unit) {
    val context = LocalContext.current
    val status by FrontDisplayManager.status.collectAsState()
    LaunchedEffect(Unit) { FrontDisplayManager.refresh(context) }
    KeypadTextDialog(stringResource(R.string.front_display_diagnostics), status, onClose, listOf(
        KeypadEntry("close", stringResource(R.string.close), onClick = onClose),
        KeypadEntry("start", stringResource(R.string.front_display_start)) { FrontDisplayManager.startProbe(context) },
        KeypadEntry("stop", stringResource(R.string.front_display_stop)) { FrontDisplayManager.stop() },
        KeypadEntry("refresh", stringResource(R.string.keypad_refresh)) { FrontDisplayManager.refresh(context) },
    ))
}
