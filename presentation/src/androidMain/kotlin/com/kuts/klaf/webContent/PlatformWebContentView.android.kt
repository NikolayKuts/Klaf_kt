package com.kuts.klaf.webContent

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@SuppressLint("SetJavaScriptEnabled")
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
    val webViewHolder = remember { mutableStateOf<WebView?>(value = null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewHolder.value?.release()
            webViewHolder.value = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                configureSettings(config = config)
                webChromeClient = object : WebChromeClient() {

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        currentOnPageLoadProgressChanged.value(newProgress)
                    }

                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request?.deny()
                    }

                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: android.os.Message?,
                    ): Boolean = false
                }

                webViewClient = object : WebViewClient() {

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        val validatedUrl = currentOnNavigationRequest.value(request?.url?.toString())
                        if (validatedUrl != null) {
                            view?.loadUrlIfNeeded(validatedUrl)
                        }
                        return true
                    }

                    override fun onPageStarted(
                        view: WebView?,
                        url: String?,
                        favicon: Bitmap?,
                    ) {
                        currentOnPageLoadStarted.value(url)
                    }

                    override fun onPageFinished(
                        view: WebView?,
                        url: String?,
                    ) {
                        currentOnPageLoadFinished.value(url)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        if (request?.isForMainFrame == true) {
                            currentOnPageLoadError.value(error?.description?.toString())
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?,
                    ) {
                        if (request?.isForMainFrame == true && errorResponse != null) {
                            currentOnPageLoadError.value("HTTP ${errorResponse.statusCode}")
                        }
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: android.net.http.SslError?,
                    ) {
                        handler?.cancel()
                        currentOnPageLoadError.value("SSL error")
                    }

                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: RenderProcessGoneDetail?,
                    ): Boolean {
                        currentOnPageLoadError.value("Web content renderer crashed")
                        return true
                    }

                    override fun onSafeBrowsingHit(
                        view: WebView?,
                        request: WebResourceRequest?,
                        threatType: Int,
                        callback: SafeBrowsingResponse?,
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            callback?.backToSafety(true)
                        } else {
                            @Suppress("DEPRECATION")
                            callback?.backToSafety(true)
                        }
                        currentOnPageLoadError.value("Unsafe page blocked")
                    }
                }

                webViewHolder.value = this
                loadUrlIfNeeded(url)
            }
        },
        update = { webView ->
            webView.configureSettings(config = config)
            webView.loadUrlIfNeeded(url)
        },
    )
}

private fun WebView.loadUrlIfNeeded(url: String) {
    if (this.url != url) {
        loadUrl(url)
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureSettings(config: WebContentConfig) {
    settings.apply {
        javaScriptEnabled = config.javaScriptEnabled
        domStorageEnabled = config.domStorageEnabled
        cacheMode = when (config.cachePolicy) {
            WebContentCachePolicy.DEFAULT -> WebSettings.LOAD_DEFAULT
            WebContentCachePolicy.CACHE_ELSE_NETWORK -> WebSettings.LOAD_CACHE_ELSE_NETWORK
            WebContentCachePolicy.NO_CACHE -> WebSettings.LOAD_NO_CACHE
            WebContentCachePolicy.CACHE_ONLY -> WebSettings.LOAD_CACHE_ONLY
        }

        allowFileAccess = false
        allowContentAccess = false
        javaScriptCanOpenWindowsAutomatically = false
        setSupportMultipleWindows(false)
        loadsImagesAutomatically = true
        loadWithOverviewMode = true
        useWideViewPort = true
        mediaPlaybackRequiresUserGesture = true
        builtInZoomControls = false
        displayZoomControls = false
        setSupportZoom(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            safeBrowsingEnabled = true
        }
    }

    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setAcceptThirdPartyCookies(this@configureSettings, false)
        }
    }
}

private fun WebView.release() {
    stopLoading()
    webChromeClient = null
    webViewClient = WebViewClient()
    loadUrl("about:blank")
    clearHistory()
    removeAllViews()
    (parent as? ViewGroup)?.removeView(this)
    destroy()
}
