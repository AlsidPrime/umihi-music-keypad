package ca.ilianokokoro.umihi.music.ui.components.bottomsheet

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.ui.components.keypad.*

@Composable
internal fun KeypadQueue(songs: List<Song>, currentIndex: Int, onClose: () -> Unit) {
    var optionsUid by remember { mutableStateOf<String?>(null) }

    // Resolve against the live timeline when acting, rather than retaining a row index.
    fun act(uid: String, action: (Int) -> Unit) {
        val index = queueEntryIndex(PlayerManager.getQueue().map { it.uid }, uid) ?: return
        action(index)
    }

    val entries = songs.mapIndexed { index, song ->
        KeypadEntry("song:${song.uid}", "${index + 1}. ${song.title}",
            subtitle = buildList {
                if (index == currentIndex) add(stringResource(R.string.playing_now))
                add(song.artist)
            }.joinToString(" · "),
            onOptions = { optionsUid = song.uid }) {
            act(song.uid) { PlayerManager.seekToIndex(it); PlayerManager.currentController?.play() }
        }
    } + KeypadEntry("close", stringResource(R.string.close), onClick = onClose)

    KeypadDialog(stringResource(R.string.queue), entries, onClose,
        message = stringResource(if (songs.isEmpty()) R.string.queue_empty else R.string.keypad_queue_hint),
        initialKey = songs.getOrNull(currentIndex)?.let { "song:${it.uid}" }, playbackShortcuts = true)

    val uid = optionsUid
    val index = uid?.let { queueEntryIndex(songs.map { song -> song.uid }, it) }
    LaunchedEffect(uid, index) {
        if (uid != null && index == null) optionsUid = null
    }
    if (uid != null && index != null) {
        val song = songs[index]
        fun move(offset: Int) {
            optionsUid = null
            act(uid) { from ->
                val controller = PlayerManager.currentController ?: return@act
                val to = from + offset
                if (to in 0 until controller.mediaItemCount) controller.moveMediaItem(from, to)
            }
        }
        KeypadDialog(song.title, listOf(
            KeypadEntry("play", stringResource(R.string.play)) {
                optionsUid = null
                act(uid) { PlayerManager.seekToIndex(it); PlayerManager.currentController?.play() }
            },
            KeypadEntry("up", stringResource(R.string.keypad_move_up), enabled = index > 0) { move(-1) },
            KeypadEntry("down", stringResource(R.string.keypad_move_down), enabled = index < songs.lastIndex) { move(1) },
            KeypadEntry("remove", stringResource(R.string.remove_from_queue)) {
                optionsUid = null
                act(uid) { PlayerManager.removeMediaItem(it) }
                if (PlayerManager.getQueue().isEmpty()) onClose()
            },
            KeypadEntry("close", stringResource(R.string.close)) { optionsUid = null },
        ), { optionsUid = null }, playbackShortcuts = true)
    }
}

/** A missing or ambiguous ID must never fall back to another song's row. */
internal fun queueEntryIndex(ids: List<String>, uid: String): Int? {
    if (uid.isBlank()) return null
    val index = ids.indexOf(uid)
    return index.takeIf { it >= 0 && ids.lastIndexOf(uid) == it }
}
