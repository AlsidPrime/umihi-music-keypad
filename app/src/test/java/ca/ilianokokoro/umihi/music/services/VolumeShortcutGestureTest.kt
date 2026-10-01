package ca.ilianokokoro.umihi.music.services

import org.junit.Assert.*
import org.junit.Test
import ca.ilianokokoro.umihi.music.services.VolumeShortcutGesture.Effect

class VolumeShortcutGestureTest {
    @Test fun shortPressChangesVolumeOnlyOnRelease() {
        val g = VolumeShortcutGesture()
        assertNull(g.down(1)); assertEquals(Effect.LOUDER, g.up(1)); assertNull(g.up(1))
        assertNull(g.down(-1)); assertEquals(Effect.QUIETER, g.up(-1))
    }
    @Test fun holdSkipsOnceWithoutVolumeChange() {
        val g = VolumeShortcutGesture()
        g.down(1); assertEquals(Effect.NEXT, g.hold(1))
        assertNull(g.down(1)); assertNull(g.hold(1)); assertNull(g.up(1))
        g.down(-1); assertEquals(Effect.PREVIOUS, g.hold(-1)); assertNull(g.up(-1))
    }
    @Test fun chordTogglesOnceAndConsumesBothReleases() {
        val g = VolumeShortcutGesture()
        assertNull(g.down(1)); assertEquals(Effect.TOGGLE, g.down(-1))
        assertNull(g.down(-1)); assertNull(g.hold(1)); assertNull(g.hold(-1))
        assertNull(g.up(1)); assertNull(g.up(-1)); assertFalse(g.isChord)
    }
    @Test fun canceledPressDoesNotChangeVolume() {
        val g = VolumeShortcutGesture()
        g.down(1); assertNull(g.up(1, canceled = true)); assertFalse(g.isPressed(1))
    }
    @Test fun disablingPreservesCapturedPairsWithoutActions() {
        val g = VolumeShortcutGesture()
        g.down(-1); g.cancelActions()
        assertTrue(g.isPressed(-1)); assertNull(g.hold(-1)); assertNull(g.up(-1))
        g.down(-1); assertEquals(Effect.QUIETER, g.up(-1))
    }
    @Test fun addingSecondKeyAfterSkipDoesNotToggleOrAdjustVolume() {
        val g = VolumeShortcutGesture()
        g.down(-1); assertEquals(Effect.PREVIOUS, g.hold(-1))
        assertNull(g.down(1)); assertNull(g.hold(1)); assertNull(g.up(-1)); assertNull(g.up(1))
    }
}
