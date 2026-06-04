package com.example.data.provider

import com.example.data.model.LyricLine
import com.example.data.model.LyricsParser
import com.example.data.model.SongLyrics
import com.example.data.model.Track

object LyricsProvider {

    private val presetLyrics = mapOf(
        "track_1" to """
            [00:00.00] ♫ [Instrumental Intro - Cosmic Lounge beats and ambient hums] ♫
            [00:08.50] Soft light spills across the midnight room
            [00:15.00] Chase away the shadows, lifting up the gloom
            [00:22.00] Steady low frequency, relaxing our bones
            [00:29.00] Floating seamlessly through coffee shop tones
            [00:36.50] Cosmic travelers with nowhere to go
            [00:43.00] Synchronized pulses starting to flow
            [00:50.00] ♫ [Mellow Saxophone improvisation playing in the reverb] ♫
            [01:05.00] Breathe in the harmony, breathe out the stress
            [01:12.50] Floating on waves of sub-bass happiness
            [01:20.00] Tell me your story, I'll play you my tune
            [01:28.00] Aether Flow is dancing, under the blue moon
            [01:36.00] ♫ [Atmospheric Chill Synth Solo] ♫
            [01:55.00] Leave your heavy burdens at the lounge front door
            [02:03.00] Let the simple rhythms guide us evermore
            [02:11.00] Midnight Lounge is where we clear up our sight
            [02:18.50] Floating with a whisper through the cosmic night...
            [02:27.00] ♫ [Chillhop beat outro fading smoothly] ♫
        """.trimIndent(),
        
        "track_2" to """
            [00:00.00] ♫ [Lo-Fi vinyl hiss & cozy acoustic guitar strumming] ♫
            [00:10.00] Warm breeze kissing the edge of the sky
            [00:18.00] Clouds made of amber are drifting slow and high
            [00:26.50] Golden rays fading, leaving traces on your hand
            [00:34.00] Endless summer memories drawn into the sand
            [00:42.00] Solaris Kid is tuning the late afternoon sun
            [00:50.50] A brand new cosmic cycle has just begun
            [00:58.00] ♫ [Soft Rhodes piano chords drifting over the beat] ♫
            [01:14.00] Chasing the horizon, where the fire meets the blue
            [01:22.00] Every single warm shade reminds me of you
            [01:30.00] Hold onto the morning, remember the day
            [01:38.50] We watched the solar flares wash the cold away
            [01:46.00] ♫ [Dreamy synthesizer chords swelling with warmth] ♫
            [02:05.00] High above the line where the earth joins the night
            [02:13.50] Everything we feel is a spark of dynamic light
            [02:22.00] Rest your weary mind, let the golden hours keep
            [02:30.00] Rocking us all gently into peaceful summer sleep...
            [02:39.00] ♫ [Satisfying lo-fi beat fading along the shore] ♫
        """.trimIndent(),
        
        "track_3" to """
            [00:00.00] ♫ [High-voltage analog synth arpeggio starting up] ♫
            [00:06.50] Zero to sixty, neon on the dash
            [00:12.00] Electric heartbeats, static in the clash
            [00:18.00] Out on the highway, Cyber Runner flies
            [00:24.00] Mirror reflections, lasers in your eyes!
            [00:30.50] Chrome wheels turning, outrunning the daylight
            [00:36.00] We are the heroes of this digital night
            [00:42.50] ♫ [Intensifying drum rolls and stereo synth sweeps] ♫
            [00:55.00] Racing past the speedway, shattering the line
            [01:01.00] Catching up to tomorrow, bending space and time
            [01:07.50] Synthwave surges, riding through the grid
            [01:13.00] Living out the visions that we always hid
            [01:19.50] ♫ [Fierce, screaming analog lead synth solo performance] ♫
            [01:40.00] Speed lines blurring, red-line on the gauge
            [01:46.00] Writing down our futures on this retro page
            [01:52.50] Neon Highway guide us, carry us away
            [01:58.00] Into the dark horizon, far beyond the day...
            [02:05.00] ♫ [Pulse-pounding outrun synthesizer beat beat outro] ♫
        """.trimIndent(),
        
        "track_4" to """
            [00:00.00] ♫ [Relaxing sound of ocean tides mixed with fingerstyle acoustic guitar] ♫
            [00:12.00] Salt on your lashes, sun on your skin
            [00:20.50] Quietly wondering where the winds begin
            [00:29.00] Coral Breeze whisper, telling us to stay
            [00:37.50] Drifting further from the noise of the day
            [00:46.00] ♫ [Gentle cello joining the soft acoustic melody] ♫
            [01:02.00] No clocks to watch here, no places to be
            [01:11.00] Just simple string chords, echoing the sea
            [01:19.50] Ocean Acoustic, playing deep and wide
            [01:28.00] Taking all our worries on the outgoing tide
            [01:37.00] ♫ [Bright acoustic guitar solo with background ocean waves] ♫
            [01:58.00] Sit on the driftwood, count the white sails
            [02:06.50] Listen to the wind, spin its ancient tales
            [02:15.00] Let the ocean wash, let the ocean heal
            [02:24.00] Everything that's beautiful is everything we feel...
            [02:33.00] ♫ [Peaceful acoustic guitar plucking fading with sea waves] ♫
        """.trimIndent(),
        
        "track_5" to """
            [00:00.00] ♫ [Singing metal bowls vibrating with deep cosmic sub-bass layers] ♫
            [00:15.00] Enter the sanctuary of perfect mental peace
            [00:30.00] Watch as the spinning thoughts slow down and cease
            [00:45.00] Brainwaves aligning in a symmetrical flow
            [01:00.00] Finding deep focus in the stillness we know
            [01:15.00] ♫ [Ethereal flute melody and celestial pad chords] ♫
            [01:35.00] Inhale the energy of the bright, empty sky
            [01:50.00] Exhale the clutter, let the past pass on by
            [02:05.00] Pure concentration, effortless, serene
            [02:20.00] Resting inside this beautiful dream...
            [02:35.00] ♫ [Resonating bowls and alpha-wave ambient drone outro] ♫
        """.trimIndent(),
        
        "track_6" to """
            [00:00.00] ♫ [Double bass intro walking through a smoky jazz club atmosphere] ♫
            [00:11.50] Velvet club curtains, dim amber light
            [00:19.00] Saxophone player is weeping tonight
            [00:27.00] The Blue Notes are swinging, taking their time
            [00:35.00] Lost in the beat, no reason, no rhyme
            [00:43.00] Glasses are clinking, spirits on high
            [00:51.50] Watching the city and neon pass by
            [01:00.00] ♫ [Elegant jazz piano roll improvisation] ♫
            [01:18.00] Play me a ballad of shadows and rain
            [01:26.50] Melodies soothing the weariness and pain
            [01:35.00] Velvet club jazz, cool, steady and deep
            [01:43.00] Keeping the secrets of nights we won't keep...
            [01:52.00] ♫ [Muted trumpet solo soaring over walking double bass] ♫
            [02:18.00] Crimson and gold, the spotlight glows low
            [02:26.50] Walking the streets where the cold winds blow
            [02:35.00] We'll meet here tomorrow, we'll listen again
            [02:43.50] Finding our warmth in this late-night refrain...
            [02:52.00] ♫ [Slow, sultry jazz trio rhythm fading away with brush drums] ♫
        """.trimIndent()
    )

    /**
     * Retrieves preset lyrics if present; otherwise, dynamically generates
     * a fully aligned set of synchronized lyrics based on track properties
     * so that any custom or local MP3 file has a fully functional and immersive experience.
     */
    fun getLyricsForTrack(track: Track): SongLyrics {
        val presetText = presetLyrics[track.id]
        if (presetText != null) {
            return LyricsParser.parse(track.id, presetText)
        }
        
        // Generate beautiful dynamic lyrics for custom / local files if not in presets!
        val duration = track.durationMs
        val lines = mutableListOf<LyricLine>()
        
        lines.add(LyricLine(0L, "♫ [Incoming stream: '${track.title}'] ♫"))
        lines.add(LyricLine(4000L, "♫ [Artist: ${track.artist} | Album: ${track.album}] ♫"))
        
        // Distribute 8 beautiful universal dynamic lines evenly across the track
        val sectionDuration = duration / 10L
        val universalFlow = listOf(
            "Starting up the rhythm, feeling the air",
            "Waves of sound are moving everywhere",
            "♫ [Enjoying '${track.genre}' instrumentals] ♫",
            "Step into the heartbeat of this song",
            "Every melody is where we belong",
            "Melodies spinning, circles in the dark",
            "Feeling the fire, capturing the spark",
            "♫ [Music is the language of the soul] ♫"
        )
        
        for (i in universalFlow.indices) {
            val timestamp = (i + 1) * sectionDuration
            if (timestamp < duration - 10000L) {
                lines.add(LyricLine(timestamp, universalFlow[i]))
            }
        }
        
        lines.add(LyricLine((duration - 8000L).coerceAtLeast(0L), "♫ [Fading out: '${track.title}'] ♫"))
        
        return SongLyrics(track.id, lines)
    }
}
