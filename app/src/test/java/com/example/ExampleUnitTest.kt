package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testLyricsParsingAndSynchronization() {
    val rawLyrics = """
      [00:00.00] Intro Music
      [00:10.50] Hello world from lyrics
      [01:05.10] Beautiful acoustic vibes
    """.trimIndent()

    val parsed = com.example.data.model.LyricsParser.parse("test_track", rawLyrics)
    
    // Check lines size
    assertEquals(3, parsed.lines.size)
    
    // Check line 0 timing (0 ms)
    assertEquals(0L, parsed.lines[0].timestampMs)
    assertEquals("Intro Music", parsed.lines[0].text)

    // Check line 1 timing (10.5 seconds = 10,500 ms)
    assertEquals(10500L, parsed.lines[1].timestampMs)
    assertEquals("Hello world from lyrics", parsed.lines[1].text)

    // Check line 2 timing (1 min 5.1 seconds = 65,100 ms)
    assertEquals(65100L, parsed.lines[2].timestampMs)
    assertEquals("Beautiful acoustic vibes", parsed.lines[2].text)

    // Test chronological synchronization mapping
    // Case A: Playback progress at 5,000ms (5s) -> should highlight index 0 ("Intro Music")
    assertEquals(0, parsed.getActiveLineIndex(5000L))

    // Case B: Playback progress at 15,000ms -> should highlight index 1 ("Hello world from lyrics")
    assertEquals(1, parsed.getActiveLineIndex(15000L))

    // Case C: Playback progress at 70,000ms -> should highlight index 2 ("Beautiful acoustic vibes")
    assertEquals(2, parsed.getActiveLineIndex(70000L))

    // Case D: Playback progress before start -> should return -1
    assertEquals(-1, parsed.getActiveLineIndex(-1000L))
  }
}
