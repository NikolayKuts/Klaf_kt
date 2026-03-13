package com.kuts.klaf.common.externalActions

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard

class IosExternalAppActions : IExternalAppActions {

    override fun consumeProcessTextWord(): String? = null

    override fun copyTextToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    override fun openExternalUrl(url: String): Boolean {
        val normalizedUrl = normalizeHttpUrl(rawUrl = url) ?: return false
        val nsUrl = NSURL(string = normalizedUrl) ?: return false
        val app = UIApplication.sharedApplication

        if (!app.canOpenURL(nsUrl)) {
            return false
        }

        app.openURL(nsUrl)
        return true
    }

    private fun normalizeHttpUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return null
        }

        return when {
            trimmed.startsWith(prefix = "http://", ignoreCase = true) -> trimmed
            trimmed.startsWith(prefix = "https://", ignoreCase = true) -> trimmed
            "://" in trimmed -> null
            else -> "https://$trimmed"
        }
    }
}
