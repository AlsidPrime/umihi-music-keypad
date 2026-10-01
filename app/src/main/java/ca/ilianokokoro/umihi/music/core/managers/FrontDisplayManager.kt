package ca.ilianokokoro.umihi.music.core.managers

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
import ca.ilianokokoro.umihi.music.core.helpers.LogHelper
import ca.ilianokokoro.umihi.music.ui.screens.diagnostics.FrontDisplayPresentation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Manual, time-limited probe using application context, independent of Activity.onStop. */
object FrontDisplayManager {
    private val handler = Handler(Looper.getMainLooper())
    private val report = MutableStateFlow("No test started.")
    val status = report.asStateFlow()
    private var manager: DisplayManager? = null
    private var context: Context? = null
    private var presentation: FrontDisplayPresentation? = null
    private var active = false
    private var result = "No test started."
    private val timeout = Runnable { stop("Test stopped after 60 seconds.") }
    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) { if (active) show(); refresh() }
        override fun onDisplayChanged(displayId: Int) { if (active && displayId != Display.DEFAULT_DISPLAY) show(); refresh() }
        override fun onDisplayRemoved(displayId: Int) { if (active) show(); refresh() }
    }
    fun refresh(appContext: Context? = context) {
        val dm = appContext?.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager ?: return
        val presentationIds = dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).map { it.displayId }
        val displays = dm.displays.joinToString("\n\n") { display ->
            val metrics = DisplayMetrics()
            display.getRealMetrics(metrics)
            "ID ${display.displayId}: ${display.name}\n${metrics.widthPixels} x ${metrics.heightPixels} px; ${metrics.densityDpi} dpi\nState ${display.state}; flags 0x${display.flags.toString(16)}; presentation=${display.displayId in presentationIds}"
        }
        report.value = "Result: $result\n\n$displays\n\nState: 1=off, 2=on. Close the lid while music is playing during the 60-second test. Test visibility is unverified; the vendor UI may cover it. No vendor broadcasts are sent."
    }
    fun startProbe(appContext: Context) {
        stop("Starting test.")
        context = appContext.applicationContext
        manager = context!!.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        active = true
        manager!!.registerDisplayListener(listener, handler)
        handler.postDelayed(timeout, 60_000)
        show()
        refresh()
    }
    private fun show() {
        val display = manager?.displays?.firstOrNull { it.displayId == 1 }
            ?: manager?.displays?.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
        if (display == null) { result = "No secondary display visible to this app."; return }
        if (display.state != Display.STATE_ON) { result = "Secondary display is off. Waiting for it to turn on."; return }
        if (presentation?.isShowing == true) {
            result = "Presentation active on ID ${display.displayId}. Check the physical screen."
            return
        }
        try {
            val probe = FrontDisplayPresentation(context!!, display)
            probe.setOnDismissListener {
                if (presentation === probe) {
                    presentation = null
                    result = "Presentation dismissed by the system."
                    refresh()
                }
            }
            probe.show()
            presentation = probe
            result = "Presentation.show succeeded on ID ${display.displayId}. Check the physical screen."
            LogHelper.printd(result, "UmihiFrontDisplay")
        } catch (e: Exception) {
            presentation = null
            result = "${e.javaClass.simpleName}: ${e.message}"
            LogHelper.printe(result, "UmihiFrontDisplay", e)
        }
    }
    fun stop(message: String = "Test stopped.") {
        active = false
        handler.removeCallbacks(timeout)
        manager?.unregisterDisplayListener(listener)
        val old = presentation
        presentation = null
        old?.dismiss()
        result = message
        refresh()
        manager = null
        context = null
    }
}
