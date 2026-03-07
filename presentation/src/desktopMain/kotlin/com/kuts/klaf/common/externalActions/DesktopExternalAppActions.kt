package com.kuts.klaf.common.externalActions

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

class DesktopExternalAppActions : IExternalAppActions {

    override fun consumeProcessTextWord(): String? = null

    override fun copyTextToClipboard(text: String) {
        val selection = StringSelection(text)

        Toolkit.getDefaultToolkit()
            .systemClipboard
            .setContents(selection, null)
    }

    override fun openExternalUrl(url: String): Boolean {
        val normalizedUrl = url.trim()

        if (normalizedUrl.isBlank()) return false

        val uri = runCatching { URI(normalizedUrl) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase()

        if (scheme != "http" && scheme != "https") return false

        return runCatching {
            if (!Desktop.isDesktopSupported()) return false
            val desktop = Desktop.getDesktop()
            if (!desktop.isSupported(Desktop.Action.BROWSE)) return false

            desktop.browse(uri)
            true
        }.getOrDefault(false)
    }
}
