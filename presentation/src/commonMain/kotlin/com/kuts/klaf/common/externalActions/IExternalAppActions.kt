package com.kuts.klaf.common.externalActions

interface IExternalAppActions {

    fun consumeProcessTextWord(): String?

    fun copyTextToClipboard(text: String)

    fun openExternalUrl(url: String): Boolean
}
