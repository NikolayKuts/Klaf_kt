package com.kuts.klaf.webContent

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import java.awt.EventQueue
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import java.io.File
import kotlin.io.path.createTempDirectory

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
    onCloseRequest: () -> Unit,
) {
    val currentOnNavigationRequest = rememberUpdatedState(newValue = onNavigationRequest)
    val currentOnPageLoadStarted = rememberUpdatedState(newValue = onPageLoadStarted)
    val currentOnPageLoadFinished = rememberUpdatedState(newValue = onPageLoadFinished)
    val currentOnPageLoadProgressChanged = rememberUpdatedState(newValue = onPageLoadProgressChanged)
    val currentOnPageLoadError = rememberUpdatedState(newValue = onPageLoadError)
    val currentOnCloseRequest = rememberUpdatedState(newValue = onCloseRequest)
    val host = remember {
        DesktopWebContentHost(initialConfig = config)
    }

    host.onNavigationRequest = currentOnNavigationRequest.value
    host.onPageLoadStarted = currentOnPageLoadStarted.value
    host.onPageLoadFinished = currentOnPageLoadFinished.value
    host.onPageLoadProgressChanged = currentOnPageLoadProgressChanged.value
    host.onPageLoadError = currentOnPageLoadError.value
    host.onCloseRequest = currentOnCloseRequest.value
    host.update(
        url = url,
        config = config,
    )

    DisposableEffect(host) {
        onDispose {
            host.dispose()
        }
    }

    SwingPanel(
        modifier = modifier,
        factory = { host.createPanel() },
    )
}

private class DesktopWebContentHost(
    initialConfig: WebContentConfig,
) {
    var onNavigationRequest: (String?) -> String? = { requestedUrl -> requestedUrl }
    var onPageLoadStarted: (String?) -> Unit = {}
    var onPageLoadFinished: (String?) -> Unit = {}
    var onPageLoadProgressChanged: (Int) -> Unit = {}
    var onPageLoadError: (String?) -> Unit = {}
    var onCloseRequest: () -> Unit = {}

    private var panel: JFXPanel? = null
    private var webEngine: WebEngine? = null
    private var pendingUrl: String? = null
    private var pendingConfig: WebContentConfig = initialConfig
    private var lastLoadedUrl: String? = null
    private var expectedProgrammaticUrl: String? = null
    private var lastAppliedCachePolicy: WebContentCachePolicy? = null
    private var isDisposed = false
    private var isToolkitReady = false

    fun createPanel(): JFXPanel {
        return JFXPanel().also(::attachPanel)
    }

    private fun attachPanel(panel: JFXPanel) {
        this.panel = panel
        Platform.setImplicitExit(false)
        isToolkitReady = true

        Platform.runLater {
            if (isDisposed) {
                return@runLater
            }

            val webView = WebView().apply {
                isContextMenuEnabled = false
                minWidth = 0.0
                minHeight = 0.0
                prefWidth = panel.width.toDouble().coerceAtLeast(0.0)
                prefHeight = panel.height.toDouble().coerceAtLeast(0.0)
            }
            val engine = webView.engine

            engine.createPopupHandler = null
            engine.configureSettings(config = pendingConfig) { cachePolicy ->
                lastAppliedCachePolicy = cachePolicy
            }
            engine.locationProperty().addListener { _, oldLocation, newLocation ->
                handleLocationChanged(
                    engine = engine,
                    previousLocation = oldLocation,
                    nextLocation = newLocation,
                )
            }
            engine.loadWorker.progressProperty().addListener { _, _, nextProgress ->
                if (isDisposed) {
                    return@addListener
                }

                val rawProgress = nextProgress?.toDouble() ?: 0.0
                val progress = if (rawProgress.isNaN() || rawProgress < 0.0) {
                    0
                } else {
                    (rawProgress * 100).toInt().coerceIn(minimumValue = 0, maximumValue = 100)
                }
                onPageLoadProgressChanged(progress)
            }
            engine.loadWorker.stateProperty().addListener { _, _, nextState ->
                if (isDisposed) {
                    return@addListener
                }

                val currentLocation = engine.location?.takeIf(String::isNotBlank)
                when (nextState) {
                    Worker.State.SCHEDULED,
                    Worker.State.RUNNING,
                    -> onPageLoadStarted(currentLocation)

                    Worker.State.SUCCEEDED -> {
                        lastLoadedUrl = currentLocation
                        onPageLoadFinished(currentLocation)
                    }

                    Worker.State.FAILED -> {
                        onPageLoadError(
                            engine.loadWorker.exception?.message ?: "Failed to load page"
                        )
                    }

                    Worker.State.CANCELLED -> onPageLoadError("Page load cancelled")
                    else -> Unit
                }
            }

            webEngine = engine
            panel.scene = Scene(webView).apply {
                addEventFilter(KeyEvent.KEY_PRESSED) { event ->
                    if (event.code == KeyCode.ESCAPE) {
                        EventQueue.invokeLater(onCloseRequest)
                        event.consume()
                    }
                }
            }
            pendingUrl?.let { initialUrl ->
                engine.loadUrlIfNeeded(initialUrl) { loadingUrl ->
                    expectedProgrammaticUrl = loadingUrl
                }
            }
        }

        panel.addComponentListener(
            object : ComponentAdapter() {
                override fun componentResized(event: ComponentEvent?) {
                    if (!isToolkitReady || isDisposed) {
                        return
                    }

                    val width = panel.width.toDouble().coerceAtLeast(0.0)
                    val height = panel.height.toDouble().coerceAtLeast(0.0)
                    Platform.runLater {
                        val webView = panel.scene?.root as? WebView ?: return@runLater
                        webView.prefWidth = width
                        webView.prefHeight = height
                    }
                }
            }
        )
    }

    fun update(
        url: String,
        config: WebContentConfig,
    ) {
        pendingUrl = url
        pendingConfig = config

        if (!isToolkitReady || panel == null) {
            return
        }

        Platform.runLater {
            if (isDisposed) {
                return@runLater
            }

            val engine = webEngine ?: return@runLater
            engine.configureSettings(config = config) { cachePolicy ->
                lastAppliedCachePolicy = cachePolicy
            }
            engine.loadUrlIfNeeded(url) { loadingUrl ->
                expectedProgrammaticUrl = loadingUrl
            }
        }
    }

    fun dispose() {
        isDisposed = true

        if (!isToolkitReady) {
            panel = null
            return
        }

        Platform.runLater {
            webEngine?.load(ABOUT_BLANK_URL)
            webEngine = null
            panel?.scene = null
            panel = null
        }
    }

    private fun handleLocationChanged(
        engine: WebEngine,
        previousLocation: String?,
        nextLocation: String?,
    ) {
        val targetLocation = nextLocation?.takeIf(String::isNotBlank) ?: return
        if (isDisposed) {
            return
        }

        if (targetLocation == expectedProgrammaticUrl) {
            lastLoadedUrl = targetLocation
            expectedProgrammaticUrl = null
            return
        }

        val validatedUrl = onNavigationRequest(targetLocation)
        when {
            validatedUrl == null -> {
                onPageLoadError("Blocked unsupported URL scheme")
                val fallbackUrl = previousLocation
                    ?.takeIf { it.isNotBlank() && it != targetLocation }
                    ?: lastLoadedUrl
                    ?: ABOUT_BLANK_URL

                expectedProgrammaticUrl = fallbackUrl
                engine.load(fallbackUrl)
            }

            validatedUrl != targetLocation -> {
                expectedProgrammaticUrl = validatedUrl
                engine.load(validatedUrl)
            }

            else -> {
                lastLoadedUrl = validatedUrl
            }
        }
    }

    private fun WebEngine.configureSettings(
        config: WebContentConfig,
        onCachePolicyApplied: (WebContentCachePolicy) -> Unit,
    ) {
        isJavaScriptEnabled = config.javaScriptEnabled

        if (lastAppliedCachePolicy != config.cachePolicy || userDataDirectory == null) {
            val userDataDirectory = when (config.cachePolicy) {
                WebContentCachePolicy.NO_CACHE -> createTempDirectory(prefix = "klaf-web-content").toFile().apply {
                    deleteOnExit()
                }

                WebContentCachePolicy.DEFAULT,
                WebContentCachePolicy.CACHE_ELSE_NETWORK,
                WebContentCachePolicy.CACHE_ONLY,
                -> File(System.getProperty("user.home"), ".klaf/web-content-cache").apply {
                    mkdirs()
                }
            }

            runCatching {
                this.userDataDirectory = userDataDirectory
            }
            onCachePolicyApplied(config.cachePolicy)
        }
    }
}

private fun WebEngine.loadUrlIfNeeded(
    url: String,
    onUrlWillLoad: (String) -> Unit,
) {
    if (location != url) {
        onUrlWillLoad(url)
        load(url)
    }
}

private const val ABOUT_BLANK_URL = "about:blank"
