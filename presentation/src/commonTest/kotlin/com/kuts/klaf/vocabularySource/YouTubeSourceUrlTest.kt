package com.kuts.klaf.vocabularySource

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YouTubeSourceUrlTest {
    @Test
    fun `accepts supported YouTube links without rewriting them`() {
        val links = listOf(
            "https://www.youtube.com/watch?v=video-id&t=12",
            "https://m.youtube.com/shorts/video-id",
            "https://youtube.com/live/video-id",
            "https://youtube.com/playlist?list=playlist-id",
            "https://youtu.be/video-id",
            "https://www.youtube.com:443/watch?v=video-id",
            "https://youtu.be:443/video-id",
            "HTTPS://YOUTUBE.COM/watch?v=video-id",
        )

        links.forEach { link ->
            assertEquals(link, validatedYouTubeSourceUrl("  $link  "))
        }
    }

    @Test
    fun `rejects empty insecure and lookalike links`() {
        val rejected = listOf(
            "",
            "https://",
            "http://youtube.com/watch?v=video-id",
            "https://youtube.com.evil.test/watch?v=video-id",
            "https://youtu.be@evil.test/video-id",
            "https://evil.test/youtube.com/watch",
            "https://youtube.com\\@evil.test/watch",
            "https://youtube.com:444/watch?v=video-id",
            "https://youtube.com:443@evil.test/watch?v=video-id",
            "https://youtube.com/watch video",
            "javascript:alert(1)",
        )

        rejected.forEach { link ->
            assertNull(validatedYouTubeSourceUrl(link), link)
        }
    }
}
