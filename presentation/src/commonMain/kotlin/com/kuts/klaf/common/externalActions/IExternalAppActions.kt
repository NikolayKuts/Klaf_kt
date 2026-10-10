package com.kuts.klaf.common.externalActions

interface IExternalAppActions {

    val supportsYouTubeSourceOpening: Boolean
        get() = false

    fun consumeProcessTextWord(): String?

    fun copyTextToClipboard(text: String)

    fun openExternalUrl(url: String): Boolean
}
