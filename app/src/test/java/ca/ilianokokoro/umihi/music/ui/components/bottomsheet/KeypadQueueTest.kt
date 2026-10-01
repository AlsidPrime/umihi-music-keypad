package ca.ilianokokoro.umihi.music.ui.components.bottomsheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeypadQueueTest {
    @Test fun menuTargetFollowsEntryAfterReorderOrRemoval() {
        assertEquals(2, queueEntryIndex(listOf("a", "b", "target"), "target"))
        assertEquals(0, queueEntryIndex(listOf("target", "a", "b"), "target"))
        assertEquals(1, queueEntryIndex(listOf("a", "target"), "target"))
        assertNull(queueEntryIndex(listOf("a", "b"), "target"))
    }

    @Test fun playNextKeepsCurrentSongAndMovesSelectedEntryImmediatelyAfterIt() {
        for ((from, current) in listOf(0 to 2, 4 to 1, 2 to 1, 0 to 4)) {
            val queue = mutableListOf("a", "b", "c", "d", "e")
            val playing = queue[current]
            val selected = queue[from]
            val destination = queuePlayNextIndex(from, current, queue.size)!!
            queue.add(destination, queue.removeAt(from))
            assertEquals(selected, queue[queue.indexOf(playing) + 1])
            assertEquals(5, queue.size)
        }
        assertNull(queuePlayNextIndex(1, 1, 5))
        assertNull(queuePlayNextIndex(0, -1, 5))
        assertNull(queuePlayNextIndex(5, 0, 5))
    }

    @Test fun missingOrAmbiguousIdentityCannotActOnAnotherEntry() {
        assertNull(queueEntryIndex(emptyList(), "target"))
        assertNull(queueEntryIndex(listOf("", "a"), ""))
        assertNull(queueEntryIndex(listOf("target", "a", "target"), "target"))
    }
}
