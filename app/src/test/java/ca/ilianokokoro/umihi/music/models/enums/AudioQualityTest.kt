package ca.ilianokokoro.umihi.music.models.enums

import org.junit.Assert.*
import org.junit.Test

class AudioQualityTest {
    private data class Stream(val id: String, val rate: Long)
    private val streams = listOf(Stream("unknown", -1), Stream("high", 160_000),
        Stream("medium", 128_000), Stream("low", 48_000), Stream("low_plus", 64_000))
    private fun select(quality: AudioQuality, choices: List<Stream> = streams) =
        selectAudioStream(choices, quality) { it.rate }

    @Test fun defaultAndUnknownPreferencesPreserveHighestQuality() {
        assertEquals(AudioQuality.BEST, AudioQuality.fromString(null))
        assertEquals(AudioQuality.BEST, AudioQuality.fromString("future_mode"))
        assertEquals(AudioQuality.BALANCED, AudioQuality.fromString("BALANCED"))
        assertEquals(AudioQuality.DATA_SAVER, AudioQuality.fromString("DATA_SAVER"))
        assertEquals("high", select(AudioQuality.BEST)?.id)
    }

    @Test fun balancedUsesHighestRateWithinItsLimit() {
        assertEquals("medium", select(AudioQuality.BALANCED)?.id)
    }

    @Test fun dataSaverUsesTheExactBoundaryWithoutChoosingUnknownRates() {
        assertEquals("low_plus", select(AudioQuality.DATA_SAVER)?.id)
    }

    @Test fun unavailableLimitedRatesFallBackToLowestAvailable() {
        val highOnly = listOf(Stream("high", 192_000), Stream("less_high", 160_000))
        assertEquals("less_high", select(AudioQuality.DATA_SAVER, highOnly)?.id)
    }

    @Test fun noMetadataStillAllowsPlaybackAndEmptyInputReturnsNull() {
        assertEquals("first", select(AudioQuality.DATA_SAVER,
            listOf(Stream("first", 0), Stream("second", -1)))?.id)
        assertNull(select(AudioQuality.BEST, emptyList()))
    }

    @Test fun bitrateTiesPreserveExtractorOrder() {
        val tied = listOf(Stream("first_codec", 128_000), Stream("second_codec", 128_000))
        assertEquals("first_codec", select(AudioQuality.BEST, tied)?.id)
        assertEquals("first_codec", select(AudioQuality.BALANCED, tied)?.id)
    }
}
