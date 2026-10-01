package ca.ilianokokoro.umihi.music.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.media.AudioManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import ca.ilianokokoro.umihi.music.audio.PlaybackService
import ca.ilianokokoro.umihi.music.core.helpers.LogHelper
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.asStateFlow

/** Only side-volume keys, only with this app's queue, never numeric/TT9 keys. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class KeypadAccessibilityService : AccessibilityService() {
    companion object {
        private val connected = MutableStateFlow(false)
        val connectionState = connected.asStateFlow()
        var appVisible = false
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val gesture = VolumeShortcutGesture()
    private val timers = mutableMapOf<Int, Runnable>()
    private var enabled = false
    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null
    private lateinit var audio: AudioManager
    override fun onServiceConnected() {
        super.onServiceConnected()
        connected.value = true
        audio = getSystemService(AUDIO_SERVICE) as AudioManager
        updateKeyFiltering(false)
        scope.launch {
            DatastoreRepository(applicationContext).settings.collect { settings ->
                if (enabled != settings.backgroundShortcuts) {
                    enabled = settings.backgroundShortcuts
                    updateKeyFiltering(enabled || gesture.hasPressedKeys)
                    cancelTimers()
                    gesture.cancelActions()
                    if (enabled) connect() else disconnect()
                }
            }
        }
    }
    private fun updateKeyFiltering(value: Boolean) {
        val info = serviceInfo ?: return
        info.flags = if (value) info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            else info.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS.inv()
        serviceInfo = info
    }
    private fun connect() {
        if (future != null) return
        val pending = MediaController.Builder(applicationContext,
            SessionToken(applicationContext, ComponentName(this, PlaybackService::class.java)))
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) {
                    if (this@KeypadAccessibilityService.controller === controller) {
                        this@KeypadAccessibilityService.controller = null
                        future = null
                        cancelTimers()
                        gesture.cancelActions()
                    }
                }
            }).buildAsync()
        future = pending
        pending.addListener({
            if (future === pending) {
                runCatching { pending.get() }.onSuccess { controller = it }
                    .onFailure { future = null; LogHelper.printe("Shortcut controller: ${it.message}") }
            }
        }, { command -> handler.post(command) })
    }
    private fun disconnect() {
        controller = null
        val pending = future
        future = null
        pending?.let { MediaController.releaseFuture(it) }
    }
    private fun canHandle(): Boolean {
        val player = controller ?: return false
        val mainPanelOn = (getSystemService(DISPLAY_SERVICE) as DisplayManager)
            .getDisplay(Display.DEFAULT_DISPLAY)?.state == Display.STATE_ON
        return enabled && !(appVisible && mainPanelOn) && audio.mode == AudioManager.MODE_NORMAL &&
            player.isConnected && player.mediaItemCount > 0 && player.playbackState == Player.STATE_READY &&
            (player.isPlaying || !audio.isMusicActive)
    }
    override fun onKeyEvent(event: KeyEvent): Boolean {
        val key = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> 1
            KeyEvent.KEYCODE_VOLUME_DOWN -> -1
            else -> return false
        }
        if (gesture.isPressed(key) && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            timers.remove(key)?.let(handler::removeCallbacks)
            gesture.up(key, canceled = true)
        }
        if (gesture.isPressed(key)) {
            if (event.action == KeyEvent.ACTION_UP) {
                timers.remove(key)?.let(handler::removeCallbacks)
                dispatch(gesture.up(key, event.isCanceled || !canHandle()))
                if (!enabled && !gesture.hasPressedKeys) updateKeyFiltering(false)
            }
            return true
        }
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0 || !canHandle()) {
            if (!enabled && !gesture.hasPressedKeys) updateKeyFiltering(false)
            if (enabled && future == null) connect()
            return false
        }
        dispatch(gesture.down(key))
        if (gesture.isChord) cancelTimers() else {
            val timer = Runnable {
                timers.remove(key)
                if (canHandle()) dispatch(gesture.hold(key)) else gesture.cancelActions()
            }
            timers[key] = timer
            handler.postDelayed(timer, ViewConfiguration.getLongPressTimeout().toLong())
        }
        return true
    }
    private fun dispatch(effect: VolumeShortcutGesture.Effect?) {
        val player = controller ?: return
        if (!canHandle()) return
        when (effect) {
            VolumeShortcutGesture.Effect.NEXT -> player.seekToNextMediaItem()
            VolumeShortcutGesture.Effect.PREVIOUS -> player.seekToPreviousMediaItem()
            VolumeShortcutGesture.Effect.TOGGLE -> if (player.playWhenReady) player.pause() else player.play()
            VolumeShortcutGesture.Effect.LOUDER -> audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            VolumeShortcutGesture.Effect.QUIETER -> audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            null -> Unit
        }
    }
    private fun cancelTimers() { timers.values.forEach(handler::removeCallbacks); timers.clear() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() { cancelTimers(); gesture.cancelActions() }
    override fun onDestroy() {
        connected.value = false
        cancelTimers()
        disconnect()
        scope.cancel()
        super.onDestroy()
    }
}
