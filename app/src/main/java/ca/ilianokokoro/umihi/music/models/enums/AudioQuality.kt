package ca.ilianokokoro.umihi.music.models.enums

enum class AudioQuality(val maxBitrateBps: Long?) {
    BEST(null),
    BALANCED(128_000),
    DATA_SAVER(64_000);

    companion object {
        fun fromString(value: String?): AudioQuality = entries.firstOrNull { it.name == value } ?: BEST
    }
}

/** Keep the original highest-bitrate default; limited modes never prefer an unknown rate. */
internal fun <T> selectAudioStream(streams: List<T>, quality: AudioQuality, bitrateBps: (T) -> Long): T? {
    val known = streams.filter { bitrateBps(it) > 0 }
    if (known.isEmpty()) return streams.firstOrNull()
    val cap = quality.maxBitrateBps ?: return known.maxByOrNull(bitrateBps)
    return known.filter { bitrateBps(it) <= cap }.maxByOrNull(bitrateBps)
        ?: known.minByOrNull(bitrateBps)
}
