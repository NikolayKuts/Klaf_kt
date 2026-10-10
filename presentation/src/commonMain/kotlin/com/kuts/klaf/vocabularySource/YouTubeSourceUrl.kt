package com.kuts.klaf.vocabularySource

private val YOUTUBE_HTTPS_URL = Regex(
    pattern = """https://(?:(?:[a-z0-9-]+\.)*youtube\.com|youtu\.be)(?::443)?(?:[/?#][^\s\\]*)?""",
    option = RegexOption.IGNORE_CASE,
)

internal fun validatedYouTubeSourceUrl(rawUrl: String): String? = rawUrl.trim()
    .takeIf(YOUTUBE_HTTPS_URL::matches)
