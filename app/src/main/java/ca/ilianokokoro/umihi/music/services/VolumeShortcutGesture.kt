package ca.ilianokokoro.umihi.music.services

/** Every captured key press produces at most one action. */
internal class VolumeShortcutGesture {
    enum class Effect { NEXT, PREVIOUS, TOGGLE, LOUDER, QUIETER }
    private val pressed = mutableMapOf<Int, Boolean>()
    var isChord = false
        private set
    val hasPressedKeys: Boolean get() = pressed.isNotEmpty()
    fun isPressed(key: Int) = key in pressed
    fun down(key: Int): Effect? {
        if (key in pressed) return null
        val untouched = pressed.values.none { it }
        pressed[key] = false
        if (pressed.size == 2) {
            isChord = true
            pressed.keys.toList().forEach { pressed[it] = true }
            return if (untouched) Effect.TOGGLE else null
        }
        return null
    }
    fun hold(key: Int): Effect? {
        if (pressed[key] != false || isChord) return null
        pressed[key] = true
        return if (key > 0) Effect.NEXT else Effect.PREVIOUS
    }
    fun up(key: Int, canceled: Boolean = false): Effect? {
        val consumed = pressed.remove(key) ?: return null
        if (pressed.isEmpty()) isChord = false
        if (consumed || canceled) return null
        return if (key > 0) Effect.LOUDER else Effect.QUIETER
    }
    fun cancelActions() { pressed.keys.toList().forEach { pressed[it] = true } }
}
