package com.kuts.klaf.webContent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData
import platform.Foundation.NSURLRequestReturnCacheDataDontLoad
import platform.Foundation.NSURLRequestReturnCacheDataElseLoad
import platform.Foundation.NSURLRequestUseProtocolCachePolicy
import platform.Foundation.NSURLErrorCancelled
import platform.UIKit.UIColor
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKPreferences
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

private const val REQUEST_TIMEOUT_SECONDS = 30.0

@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
actual fun PlatformWebContentView(
    url: String,
    config: WebContentConfig,
    modifier: Modifier,
    onNavigationRequest: (String?) -> String?,
    onPageLoadStarted: (String?) -> Unit,
    onPageLoadFinished: (String?) -> Unit,
    onPageLoadProgressChanged: (Int) -> Unit,
    onPageLoadError: (String?) -> Unit,
) {
    val currentOnNavigationRequest = rememberUpdatedState(newValue = onNavigationRequest)
    val currentOnPageLoadStarted = rememberUpdatedState(newValue = onPageLoadStarted)
    val currentOnPageLoadFinished = rememberUpdatedState(newValue = onPageLoadFinished)
    val currentOnPageLoadProgressChanged = rememberUpdatedState(newValue = onPageLoadProgressChanged)
    val currentOnPageLoadError = rememberUpdatedState(newValue = onPageLoadError)

    val webViewDelegate = remember { SimpleWebViewNavigationDelegate() }

    webViewDelegate.onNavigationRequest = { requestedUrl ->
        currentOnNavigationRequest.value(requestedUrl)
    }
    webViewDelegate.onPageLoadStarted = { pageUrl ->
        currentOnPageLoadStarted.value(pageUrl)
    }
    webViewDelegate.onPageLoadFinished = { pageUrl ->
        currentOnPageLoadFinished.value(pageUrl)
    }
    webViewDelegate.onPageLoadProgressChanged = { progress ->
        currentOnPageLoadProgressChanged.value(progress)
    }
    webViewDelegate.onPageLoadError = { message ->
        currentOnPageLoadError.value(message)
    }
    webViewDelegate.currentConfig = config

    UIKitView(
        modifier = modifier,
        factory = {
            WKWebView(
                frame = CGRectMake(0.0, 0.0, 0.0, 0.0),
                configuration = createConfiguration(config = config),
            ).apply {
                opaque = false
                backgroundColor = UIColor.clearColor
                navigationDelegate = webViewDelegate

                webViewDelegate.webView = this
                webViewDelegate.lastLoadedUrl = url
                buildRequest(
                    url = url,
                    cachePolicy = config.cachePolicy,
                )?.let(::loadRequest) ?: currentOnPageLoadError.value("Invalid URL")
            }
        },
        update = { webView ->
            webViewDelegate.webView = webView
            webViewDelegate.currentConfig = config

            webView.configuration.defaultWebpagePreferences.allowsContentJavaScript =
                config.javaScriptEnabled
            webView.configuration.preferences.javaScriptCanOpenWindowsAutomatically = false

            if (webViewDelegate.lastLoadedUrl != url) {
                webViewDelegate.lastLoadedUrl = url
                buildRequest(
                    url = url,
                    cachePolicy = config.cachePolicy,
                )?.let(webView::loadRequest) ?: currentOnPageLoadError.value("Invalid URL")
            }
        },
        onRelease = { webView ->
            if (webViewDelegate.webView === webView) {
                webViewDelegate.webView = null
            }

            webView.stopLoading()
            webView.navigationDelegate = null
        },
        properties = UIKitInteropProperties(
            isNativeAccessibilityEnabled = true,
        ),
    )
}

private fun createConfiguration(config: WebContentConfig): WKWebViewConfiguration {
    return WKWebViewConfiguration().apply {
        allowsInlineMediaPlayback = true
        preferences = WKPreferences().apply {
            javaScriptCanOpenWindowsAutomatically = false
        }
        defaultWebpagePreferences.allowsContentJavaScript = config.javaScriptEnabled
    }
}

private fun buildRequest(
    url: String,
    cachePolicy: WebContentCachePolicy,
): NSURLRequest? {
    val nsUrl = NSURL(string = url) ?: return null

    return NSURLRequest(
        uRL = nsUrl,
        cachePolicy = cachePolicy.toNativeCachePolicy(),
        timeoutInterval = REQUEST_TIMEOUT_SECONDS,
    )
}

private fun WebContentCachePolicy.toNativeCachePolicy(): ULong {
    return when (this) {
        WebContentCachePolicy.DEFAULT -> NSURLRequestUseProtocolCachePolicy
        WebContentCachePolicy.CACHE_ELSE_NETWORK -> NSURLRequestReturnCacheDataElseLoad
        WebContentCachePolicy.NO_CACHE -> NSURLRequestReloadIgnoringLocalCacheData
        WebContentCachePolicy.CACHE_ONLY -> NSURLRequestReturnCacheDataDontLoad
    }
}

@OptIn(ExperimentalForeignApi::class)
private class SimpleWebViewNavigationDelegate : NSObject(), WKNavigationDelegateProtocol {
    var webView: WKWebView? = null
    var lastLoadedUrl: String? = null
    var currentConfig: WebContentConfig = WebContentConfig()

    var onNavigationRequest: (String?) -> String? = { it }
    var onPageLoadStarted: (String?) -> Unit = {}
    var onPageLoadFinished: (String?) -> Unit = {}
    var onPageLoadProgressChanged: (Int) -> Unit = {}
    var onPageLoadError: (String?) -> Unit = {}

    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
        val requestedUrl = decidePolicyForNavigationAction.request.URL?.absoluteString
        if (requestedUrl == null) {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
            return
        }

        val validatedUrl = onNavigationRequest(requestedUrl)
        when {
            validatedUrl == null -> {
                decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            }

            decidePolicyForNavigationAction.targetFrame == null || validatedUrl != requestedUrl -> {
                lastLoadedUrl = validatedUrl
                val request = buildRequest(
                        url = validatedUrl,
                        cachePolicy = currentConfig.cachePolicy,
                )
                if (request != null) {
                    webView.loadRequest(request = request)
                } else {
                    onPageLoadError("Invalid URL")
                }
                decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            }

            else -> {
                lastLoadedUrl = requestedUrl
                decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
            }
        }
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didStartProvisionalNavigation: WKNavigation?,
    ) {
        val currentUrl = webView.URL?.absoluteString ?: lastLoadedUrl
        onPageLoadStarted(currentUrl)
        onPageLoadProgressChanged(0)
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didFinishNavigation: WKNavigation?,
    ) {
        val currentUrl = webView.URL?.absoluteString ?: lastLoadedUrl
        lastLoadedUrl = currentUrl
        onPageLoadProgressChanged(100)
        onPageLoadFinished(currentUrl)
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didFailNavigation: WKNavigation?,
        withError: NSError,
    ) {
        handleFailure(webView = webView, error = withError)
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didFailProvisionalNavigation: WKNavigation?,
        withError: NSError,
    ) {
        handleFailure(webView = webView, error = withError)
    }

    private fun handleFailure(
        webView: WKWebView,
        error: NSError,
    ) {
        if (error.code == NSURLErrorCancelled) {
            return
        }

        val currentUrl = webView.URL?.absoluteString ?: lastLoadedUrl
        onPageLoadStarted(currentUrl)
        onPageLoadError(error.localizedDescription)
    }
}
