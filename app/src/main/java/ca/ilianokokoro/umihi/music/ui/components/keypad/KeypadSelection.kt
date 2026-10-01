package ca.ilianokokoro.umihi.music.ui.components.keypad

internal fun selectedEntryIndex(keys: List<String>, enabled: List<Boolean>, selected: String?, initial: String?): Int? {
    for (key in listOfNotNull(selected, initial)) {
        val index = keys.indexOf(key)
        if (index >= 0 && enabled[index]) return index
    }
    return enabled.indexOfFirst { it }.takeIf { it >= 0 }
}

/** Null means the list boundary was reached and navigation can take focus. */
internal fun moveEntrySelection(enabled: List<Boolean>, current: Int?, direction: Int): Int? {
    if (current == null) return null
    var index = current + direction
    while (index in enabled.indices) {
        if (enabled[index]) return index
        index += direction
    }
    return null
}
