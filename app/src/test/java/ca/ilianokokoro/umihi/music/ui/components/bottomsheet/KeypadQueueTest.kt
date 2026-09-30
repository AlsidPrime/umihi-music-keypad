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

    @Test fun missingOrAmbiguousIdentityCannotActOnAnotherEntry() {
        assertNull(queueEntryIndex(emptyList(), "target"))
        assertNull(queueEntryIndex(listOf("", "a"), ""))
        assertNull(queueEntryIndex(listOf("target", "a", "target"), "target"))
    }
}
