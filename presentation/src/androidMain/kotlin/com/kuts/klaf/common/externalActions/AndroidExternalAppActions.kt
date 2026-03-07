package com.kuts.klaf.common.externalActions

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.lib.lokdroid.core.logE

private const val MIME_TYPE_TEXT_PLAIN = "text/plain"

class AndroidExternalAppActions(
    private val context: Context,
) : IExternalAppActions {

    private val appContext = context.applicationContext

    override fun consumeProcessTextWord(): String? {
        val activity = context.findActivity() ?: return null
        val intent = activity.intent ?: return null
        if (intent.action != Intent.ACTION_PROCESS_TEXT) {
            return null
        }

        val selectedWord = intent.type
            ?.startsWith(MIME_TYPE_TEXT_PLAIN)
            ?.takeIf { it }
            ?.let { intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() }

        if (selectedWord != null) {
            intent.removeExtra(Intent.EXTRA_PROCESS_TEXT)
            intent.action = Intent.ACTION_MAIN
        }

        return selectedWord
    }

    override fun copyTextToClipboard(text: String) {
        val clipboard = appContext.getSystemService<ClipboardManager>() ?: return
        val clip = ClipData.newPlainText("prompt", text)
        clipboard.setPrimaryClip(clip)
    }

    override fun openExternalUrl(url: String): Boolean {
        val rawUrl = url.trim()

        if (rawUrl.isEmpty()) {
            return false
        }

        val normalizedUrl = run {
            val rawUri = rawUrl.toUri()
            when {
                rawUri.scheme.isNullOrBlank() -> "https://$rawUrl"
                rawUri.scheme.equals(other = "http", ignoreCase = true) -> rawUrl
                rawUri.scheme.equals(other = "https", ignoreCase = true) -> rawUrl
                else -> return false
            }
        }
        val uri = normalizedUrl.toUri()

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return runCatching {
            appContext.startActivity(intent)
            true
        }.onFailure { error ->
            logE("Failed to open external url: $normalizedUrl\n${error.stackTraceToString()}")
        }.getOrDefault(false)
    }

    private tailrec fun Context.findActivity(): Activity? {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }
    }
}
