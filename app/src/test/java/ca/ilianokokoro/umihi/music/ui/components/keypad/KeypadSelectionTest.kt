package ca.ilianokokoro.umihi.music.ui.components.keypad

import org.junit.Assert.*
import org.junit.Test

class KeypadSelectionTest {
    @Test fun selectionFollowsSongIdentityAfterReordering() {
        assertEquals(2, selectedEntryIndex(listOf("c", "a", "b"), listOf(true, true, true), "b", null))
    }

    @Test fun removedOrUnavailableSelectionFallsBackToAnEnabledRow() {
        assertEquals(1, selectedEntryIndex(listOf("a", "b"), listOf(false, true), "missing", "a"))
        assertEquals(1, selectedEntryIndex(listOf("a", "b"), listOf(false, true), "a", null))
    }

    @Test fun unavailableSongsAreSkippedInBothDirections() {
        val enabled = listOf(true, false, false, true)
        assertEquals(3, moveEntrySelection(enabled, 0, 1))
        assertEquals(0, moveEntrySelection(enabled, 3, -1))
    }

    @Test fun listBoundariesHandFocusBackToNavigation() {
        assertNull(moveEntrySelection(listOf(true, true), 0, -1))
        assertNull(moveEntrySelection(listOf(true, true), 1, 1))
        assertNull(selectedEntryIndex(emptyList(), emptyList(), null, null))
        assertNull(selectedEntryIndex(listOf("a"), listOf(false), "a", null))
    }

    @Test fun aLongPlaylistCanBeTraversedBeyondTheVisibleRowsAndBack() {
        val enabled = List(1000) { it % 7 != 0 }
        val expected = enabled.indices.filter { enabled[it] }
        val visited = mutableListOf<Int>()
        var cursor = selectedEntryIndex(enabled.indices.map(Int::toString), enabled, null, null)
        while (cursor != null) {
            visited.add(cursor)
            cursor = moveEntrySelection(enabled, cursor, 1)
        }
        assertEquals(expected, visited)
        val backwards = mutableListOf<Int>()
        cursor = visited.last()
        while (cursor != null) {
            backwards.add(cursor)
            cursor = moveEntrySelection(enabled, cursor, -1)
        }
        assertEquals(expected.reversed(), backwards)
    }
}
