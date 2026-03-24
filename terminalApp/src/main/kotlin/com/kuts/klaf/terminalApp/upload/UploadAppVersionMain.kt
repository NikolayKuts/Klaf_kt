package com.kuts.klaf.terminalApp.upload

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.layout.DrawStyle
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.drawBehind
import com.jakewharton.mosaic.layout.fillMaxSize
import com.jakewharton.mosaic.layout.onPreviewKeyEvent
import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.runMosaicBlocking
import com.jakewharton.mosaic.terminal.Terminal
import com.jakewharton.mosaic.ui.Alignment
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Spacer
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    runMosaicBlocking {
        UploadAppVersionScreen(configuration = args.toUploadConfiguration())
    }
}

private data class UploadPalette(
    val screenBackground: Color,
    val panelBackground: Color,
    val panelBorder: Color,
    val titleBackground: Color,
    val titleText: Color,
    val sectionBackground: Color,
    val sectionText: Color,
    val statusText: Color,
    val statusIdleBackground: Color,
    val statusLoadingBackground: Color,
    val statusLoadingBackgroundPulse: Color,
    val statusSuccessBackground: Color,
    val statusErrorBackground: Color,
    val primaryText: Color,
    val mutedText: Color,
    val accentText: Color,
    val focusedButtonBackground: Color,
    val focusedButtonBorder: Color,
    val startButtonBackground: Color,
    val cancelButtonBackground: Color,
    val confirmedStartBackground: Color,
    val confirmedCancelBackground: Color,
)

private data class StatusStyle(
    val background: Color,
    val text: Color,
    val details: Color,
)

private enum class UploadLayoutMode {
    Full,
    Compact,
    Minimal,
}

@Composable
private fun UploadAppVersionScreen(
    configuration: UploadAppVersionConfiguration,
) {
    val terminalState = LocalTerminalState.current
    val viewModel = remember(configuration) { UploadAppVersionViewModel(configuration) }
    val coroutineScope = rememberCoroutineScope()
    val uiState = viewModel.uiState
    val palette = rememberUploadPalette(theme = terminalState.theme)
    val terminalWidth = terminalState.size.columns.coerceAtLeast(minimumValue = 1)
    val terminalHeight = terminalState.size.rows.coerceAtLeast(minimumValue = 1)
    val panelWidth = minOf(76, (terminalWidth - 2).coerceAtLeast(minimumValue = 1))
    val contentWidth = (panelWidth - 2).coerceAtLeast(minimumValue = 1)
    var loadingAnimationStep by remember { mutableStateOf(0) }
    val statusStyle = palette.statusStyle(
        kind = uiState.statusKind,
        loadingPulseActive = loadingAnimationStep % 2 == 1,
    )
    val animatedStatusMessage = if (uiState.statusKind == UploadStatusKind.Loading) {
        uiState.statusMessage.asAnimatedLoadingText(step = loadingAnimationStep)
    } else {
        uiState.statusMessage
    }
    val layoutMode = when {
        terminalHeight >= 20 -> UploadLayoutMode.Full
        terminalHeight >= 13 -> UploadLayoutMode.Compact
        else -> UploadLayoutMode.Minimal
    }

    LaunchedEffect(viewModel) {
        viewModel.loadMetadata()
    }

    LaunchedEffect(uiState.statusKind) {
        if (uiState.statusKind == UploadStatusKind.Loading) {
            while (true) {
                loadingAnimationStep = (loadingAnimationStep + 1) % 4
                delay(350)
            }
        } else {
            loadingAnimationStep = 0
        }
    }

    LaunchedEffect(Unit) {
        awaitCancellation()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = palette.screenBackground),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .width(width = panelWidth)
                .border(color = palette.panelBorder)
                .background(color = palette.panelBackground)
                .padding(all = 1)
                .onPreviewKeyEvent { event ->
                    when (event.key.lowercase()) {
                        "arrowleft", "arrowup" -> {
                            viewModel.focusAction(UploadAction.Start)
                            true
                        }

                        "arrowright", "arrowdown" -> {
                            viewModel.focusAction(UploadAction.Cancel)
                            true
                        }

                        "enter", "return", "\n", "\r" -> {
                            coroutineScope.launch {
                                when (viewModel.confirmSelectedAction()) {
                                    UploadAppVersionCommand.Exit -> {
                                        exitProcess(0)
                                    }

                                    null -> Unit
                                }
                            }
                            true
                        }

                        else -> false
                    }
                },
        ) {
            ScreenTitle(
                width = contentWidth,
                compact = layoutMode != UploadLayoutMode.Full,
                palette = palette,
            )

            when (layoutMode) {
                UploadLayoutMode.Full -> FullMetadata(
                    configuration = uiState.configuration,
                    contentWidth = contentWidth,
                    palette = palette,
                )

                UploadLayoutMode.Compact -> CompactMetadata(
                    configuration = uiState.configuration,
                    contentWidth = contentWidth,
                    palette = palette,
                )

                UploadLayoutMode.Minimal -> MinimalMetadata(
                    configuration = uiState.configuration,
                    contentWidth = contentWidth,
                    palette = palette,
                )
            }

            StatusTitle(
                width = contentWidth,
                style = statusStyle,
            )
            StatusText(
                text = animatedStatusMessage,
                width = contentWidth,
                style = statusStyle,
            )
            uiState.uploadProgress
                ?.let { progress ->
                    StatusText(
                        text = progress.asProgressBar(width = contentWidth),
                        width = contentWidth,
                        style = statusStyle,
                    )
                }
            uiState.statusDetails
                ?.takeIf { it.isNotBlank() }
                ?.let { details ->
                    StatusText(
                        text = details,
                        width = contentWidth,
                        style = statusStyle,
                        details = true,
                    )
                }
            Gap()

            if (uiState.isMetadataLoaded && !uiState.isBusy && !uiState.isCompleted) {
                ActionButtons(
                    selectedAction = uiState.selectedAction,
                    confirmedAction = uiState.confirmedAction,
                    contentWidth = contentWidth,
                    palette = palette,
                )
                if (layoutMode != UploadLayoutMode.Minimal) {
                    WrappedText(
                        text = "Use ←/→ or ↑/↓ to move focus. Press Enter to confirm the selected button.",
                        width = contentWidth,
                        color = palette.mutedText,
                    )
                    Gap()
                }
            } else {
                WrappedText(
                    text = if (uiState.isBusy) {
                        "Please wait while the current operation is running."
                    } else {
                        "Waiting until all metadata is loaded."
                    },
                    width = contentWidth,
                    color = palette.mutedText,
                )
                Gap()
            }
        }
    }
}

@Composable
private fun ScreenTitle(
    width: Int,
    compact: Boolean,
    palette: UploadPalette,
) {
    Text(
        value = fitSingleLine(
            text = "Upload App Version",
            width = width,
        ),
        color = palette.titleText,
        background = palette.titleBackground,
        textStyle = TextStyle.Bold,
    )
    if (!compact) {
        Text(
            value = fitSingleLine(
                text = "Terminal launcher for the Telegram APK distribution flow",
                width = width,
            ),
            color = palette.accentText,
            textStyle = TextStyle.Bold,
        )
        Gap()
    }
}

@Composable
private fun FullMetadata(
    configuration: UploadAppVersionConfiguration,
    contentWidth: Int,
    palette: UploadPalette,
) {
    SectionTitle(title = "Plugin", width = contentWidth, palette = palette)
    InfoLine(label = "Name", value = configuration.pluginName, width = contentWidth, palette = palette)
    InfoLine(label = "Task", value = configuration.taskName, width = contentWidth, palette = palette)
    Gap()
    SectionTitle(title = "Selected Build", width = contentWidth, palette = palette)
    InfoLine(label = "Project", value = configuration.projectName, width = contentWidth, palette = palette)
    InfoLine(label = "Build Type", value = configuration.buildType, width = contentWidth, palette = palette)
    InfoLine(label = "Version Name", value = configuration.versionName, width = contentWidth, palette = palette)
    InfoLine(label = "Version Code", value = configuration.versionCode, width = contentWidth, palette = palette)
    Gap()
    SectionTitle(title = "Description", width = contentWidth, palette = palette)
    WrappedText(
        text = "This launcher is a terminal front end for the APK upload plugin.",
        width = contentWidth,
        color = palette.primaryText,
    )
    WrappedText(
        text = "It lets the user confirm or cancel the upload flow before the Gradle task starts.",
        width = contentWidth,
        color = palette.primaryText,
    )
    Gap()
}

@Composable
private fun CompactMetadata(
    configuration: UploadAppVersionConfiguration,
    contentWidth: Int,
    palette: UploadPalette,
) {
    SectionTitle(title = "Plugin", width = contentWidth, palette = palette)
    InfoLine(
        label = "Task",
        value = "${configuration.pluginName} / ${configuration.taskName}",
        width = contentWidth,
        palette = palette,
    )
    InfoLine(
        label = "Build",
        value = "${configuration.projectName} ${configuration.buildType} ${configuration.versionName} (${configuration.versionCode})",
        width = contentWidth,
        palette = palette,
    )
    Gap()
}

@Composable
private fun MinimalMetadata(
    configuration: UploadAppVersionConfiguration,
    contentWidth: Int,
    palette: UploadPalette,
) {
    Text(
        value = fitSingleLine(
            text = "${configuration.projectName} ${configuration.buildType} ${configuration.versionName} (${configuration.versionCode})",
            width = contentWidth,
        ),
        color = palette.accentText,
        textStyle = TextStyle.Bold,
    )
    Text(
        value = fitSingleLine(
            text = configuration.taskName,
            width = contentWidth,
        ),
        color = palette.mutedText,
    )
    Gap()
}

@Composable
private fun SectionTitle(
    title: String,
    width: Int,
    palette: UploadPalette,
) {
    TitleChip(
        text = title,
        width = width,
        background = palette.sectionBackground,
        textColor = palette.sectionText,
        emphasize = false,
    )
}

@Composable
private fun StatusTitle(
    width: Int,
    style: StatusStyle,
) {
    Text(
        value = centerSingleLine(
            text = "Status",
            width = width,
        ),
        color = style.text,
        background = style.background,
        textStyle = TextStyle.Bold,
    )
}

@Composable
private fun StatusText(
    text: String,
    width: Int,
    style: StatusStyle,
    details: Boolean = false,
) {
    wrapText(text = text, width = width)
        .forEach { line ->
            Text(
                value = fitSingleLine(text = line, width = width),
                color = if (details) style.details else style.text,
            )
        }
}

@Composable
private fun TitleChip(
    text: String,
    width: Int,
    background: Color,
    textColor: Color,
    emphasize: Boolean,
) {
    val horizontalPadding = if (emphasize) 1 else 1
    val paddedText = paddedSingleLine(
        text = text,
        horizontalPadding = horizontalPadding,
    )

    if (emphasize) {
        Box(
            modifier = Modifier.width(width = width),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                value = paddedText,
                color = textColor,
                textStyle = TextStyle.Bold,
                background = background,
            )
        }
    } else {
        Text(
            value = paddedText,
            color = textColor,
            textStyle = TextStyle.Bold,
            background = background,
        )
    }
}

@Composable
private fun InfoLine(
    label: String,
    value: String,
    width: Int,
    palette: UploadPalette,
) {
    Text(
        value = fitSingleLine(
            text = "${label.padEnd(length = 13, padChar = ' ')} $value",
            width = width,
        ),
        color = palette.primaryText,
    )
}

@Composable
private fun WrappedText(
    text: String,
    width: Int,
    color: Color,
) {
    wrapText(text = text, width = width)
        .forEach { line ->
            Text(
                value = line,
                color = color,
            )
        }
}

@Composable
private fun ActionButtons(
    selectedAction: UploadAction,
    confirmedAction: UploadAction?,
    contentWidth: Int,
    palette: UploadPalette,
) {
    val spacing = if (contentWidth >= 28) 2 else 1
    val maxButtonWidth = ((contentWidth - spacing) / 2).coerceAtLeast(minimumValue = 10)
    val buttonWidth = minOf(14, maxButtonWidth)

    Box(
        modifier = Modifier.width(width = contentWidth),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row {
            ActionButton(
                action = UploadAction.Start,
                selectedAction = selectedAction,
                confirmedAction = confirmedAction,
                buttonWidth = buttonWidth,
                palette = palette,
            )
            Spacer(modifier = Modifier.width(width = spacing))
            ActionButton(
                action = UploadAction.Cancel,
                selectedAction = selectedAction,
                confirmedAction = confirmedAction,
                buttonWidth = buttonWidth,
                palette = palette,
            )
        }
    }
}

@Composable
private fun ActionButton(
    action: UploadAction,
    selectedAction: UploadAction,
    confirmedAction: UploadAction?,
    buttonWidth: Int,
    palette: UploadPalette,
) {
    val isSelected = selectedAction == action
    val isConfirmed = confirmedAction == action

    val background = when {
        isConfirmed && action == UploadAction.Start -> palette.confirmedStartBackground
        isConfirmed && action == UploadAction.Cancel -> palette.confirmedCancelBackground
        isSelected -> palette.focusedButtonBackground
        action == UploadAction.Start -> palette.startButtonBackground
        else -> palette.cancelButtonBackground
    }
    val foreground = when {
        isSelected || isConfirmed -> palette.primaryText
        else -> Color.Black
    }
//    val secondaryForeground = when {
//        isSelected || isConfirmed -> Color.Black
//        else -> palette.mutedText
//    }
    val borderColor = when {
        isSelected || isConfirmed -> palette.focusedButtonBorder
        else -> palette.panelBorder
    }
//    val marker = when {
//        isConfirmed -> "*"
//        isSelected -> ">"
//        else -> " "
//    }

    Box(
        modifier = Modifier
            .width(width = buttonWidth)
            .border(color = borderColor)
            .background(color = background)
            .padding(horizontal = 1),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            value = centerSingleLine(
                text = action.label(),
                width = (buttonWidth - 2).coerceAtLeast(minimumValue = 1),
            ),
            color = foreground,
            textStyle = TextStyle.Bold,
        )
//        Text(
//            value = fitSingleLine(
//                text = if (isSelected) "Press Enter" else "",
//                width = (buttonWidth - 2).coerceAtLeast(minimumValue = 1),
//            ),
//            color = secondaryForeground,
//        )
    }
}

@Composable
private fun Gap() {
    Text(value = "")
}

@Composable
private fun rememberUploadPalette(
    theme: Terminal.Theme,
): UploadPalette =
    when (theme) {
        Terminal.Theme.Light -> UploadPalette(
            screenBackground = Color(244, 246, 248),
            panelBackground = Color(252, 253, 255),
            panelBorder = Color(146, 160, 176),
            titleBackground = Color(223, 231, 238),
            titleText = Color(22, 32, 44),
            sectionBackground = Color(236, 218, 154),
            sectionText = Color(27, 31, 38),
            statusText = Color(22, 32, 44),
            statusIdleBackground = Color(213, 225, 235),
            statusLoadingBackground = Color(239, 234, 224),
            statusLoadingBackgroundPulse = Color(229, 220, 204),
            statusSuccessBackground = Color(197, 229, 203),
            statusErrorBackground = Color(239, 198, 198),
            primaryText = Color(36, 44, 54),
            mutedText = Color(101, 113, 126),
            accentText = Color(60, 102, 125),
            focusedButtonBackground = Color(182, 210, 223),
            focusedButtonBorder = Color(82, 125, 149),
            startButtonBackground = Color(222, 238, 228),
            cancelButtonBackground = Color(242, 225, 228),
            confirmedStartBackground = Color(128, 202, 153),
            confirmedCancelBackground = Color(234, 144, 149),
        )

        Terminal.Theme.Dark,
        Terminal.Theme.Unknown,
        -> UploadPalette(
            screenBackground = Color(43, 43, 43),
            panelBackground = Color(48, 48, 48),
            panelBorder = Color(82, 82, 82),
            titleBackground = Color(58, 58, 58),
            titleText = Color(236, 236, 236),
            sectionBackground = Color(68, 68, 68),
            sectionText = Color(236, 236, 236),
            statusText = Color(236, 236, 236),
            statusIdleBackground = Color(63, 63, 63),
            statusLoadingBackground = Color(76, 70, 62),
            statusLoadingBackgroundPulse = Color(88, 78, 68),
            statusSuccessBackground = Color(60, 90, 60),
            statusErrorBackground = Color(98, 62, 62),
            primaryText = Color(224, 224, 224),
            mutedText = Color(172, 172, 172),
            accentText = Color(198, 198, 198),
            focusedButtonBackground = Color(76, 76, 76),
            focusedButtonBorder = Color(118, 118, 118),
            startButtonBackground = Color(54, 61, 54),
            cancelButtonBackground = Color(61, 54, 54),
            confirmedStartBackground = Color(86, 104, 86),
            confirmedCancelBackground = Color(104, 86, 86),
        )
    }

private fun UploadPalette.statusStyle(
    kind: UploadStatusKind,
    loadingPulseActive: Boolean,
): StatusStyle {
    val background = when (kind) {
        UploadStatusKind.Idle -> statusIdleBackground
        UploadStatusKind.Loading -> {
            if (loadingPulseActive) statusLoadingBackgroundPulse else statusLoadingBackground
        }
        UploadStatusKind.Success -> statusSuccessBackground
        UploadStatusKind.Error -> statusErrorBackground
    }

    return StatusStyle(
        background = background,
        text = statusText,
        details = mutedText,
    )
}

private fun UploadAction.label(): String =
    when (this) {
        UploadAction.Start -> "Upload"
        UploadAction.Cancel -> "Cancel"
    }

private fun wrapText(
    text: String,
    width: Int,
): List<String> {
    if (text.isBlank()) return listOf("")
    if (width <= 1) return listOf(text.take(1))

    val lines = mutableListOf<String>()
    val paragraphs = text
        .replace(oldValue = "\r", newValue = "")
        .split('\n')

    for (paragraph in paragraphs) {
        if (paragraph.isBlank()) {
            lines += ""
            continue
        }

        val currentLine = StringBuilder()
        val words = paragraph.split(' ')

        for (rawWord in words) {
            if (rawWord.isEmpty()) continue

            var word = rawWord
            while (word.length > width) {
                if (currentLine.isNotEmpty()) {
                    lines += currentLine.toString()
                    currentLine.clear()
                }

                lines += word.take(width)
                word = word.drop(width)
            }

            if (currentLine.isEmpty()) {
                currentLine.append(word)
                continue
            }

            val candidateLength = currentLine.length + 1 + word.length
            if (candidateLength <= width) {
                currentLine.append(' ').append(word)
            } else {
                lines += currentLine.toString()
                currentLine.clear()
                currentLine.append(word)
            }
        }

        if (currentLine.isNotEmpty()) {
            lines += currentLine.toString()
        }
    }

    return if (lines.isEmpty()) listOf("") else lines
}

private fun fitSingleLine(
    text: String,
    width: Int,
): String {
    if (width <= 0) return ""
    if (text.length <= width) return text
    if (width == 1) return "…"
    return text.take(width - 1) + "…"
}

private fun centerSingleLine(
    text: String,
    width: Int,
): String {
    val fitted = fitSingleLine(
        text = text,
        width = width,
    )
    if (width <= fitted.length) return fitted

    val totalPadding = width - fitted.length
    val startPadding = totalPadding / 2
    val endPadding = totalPadding - startPadding
    return buildString(capacity = width) {
        repeat(startPadding) { append(' ') }
        append(fitted)
        repeat(endPadding) { append(' ') }
    }
}


private fun paddedSingleLine(
    text: String,
    horizontalPadding: Int,
): String {
    val safePadding = horizontalPadding.coerceAtLeast(minimumValue = 0)
    return buildString {
        repeat(safePadding) { append(' ') }
        append(text)
        repeat(safePadding) { append(' ') }
    }
}

private fun String.asAnimatedLoadingText(
    step: Int,
): String {
    val baseText = trimEnd { it == '.' || it == ' ' }
    val dots = when (step) {
        1 -> "."
        2 -> ".."
        3 -> "..."
        else -> ""
    }

    return baseText + dots
}

private fun UploadProgressState.asProgressBar(
    width: Int,
): String {
    val suffix = " ${percent}%"
    val barWidth = (width - suffix.length - 2)
        .coerceAtLeast(minimumValue = 10)
    val filledCount = ((barWidth * percent) / 100)
        .coerceIn(minimumValue = 0, maximumValue = barWidth)
    val bar = buildString(capacity = barWidth) {
        repeat(filledCount) { append('#') }
        repeat(barWidth - filledCount) { append('-') }
    }

    return "[${bar}]$suffix"
}

private fun Modifier.border(
    color: Color,
    width: Int = 1,
): Modifier = drawBehind {
    drawRect(
        background = color,
        drawStyle = DrawStyle.Stroke(width = width),
    )
}

private fun Array<String>.toUploadConfiguration(): UploadAppVersionConfiguration {
    val values = buildMap {
        for (argument in this@toUploadConfiguration) {
            if (!argument.startsWith(prefix = "--")) continue
            val separatorIndex = argument.indexOf('=')
            if (separatorIndex <= 2 || separatorIndex == argument.lastIndex) continue

            val key = argument.substring(startIndex = 2, endIndex = separatorIndex)
            val value = argument.substring(startIndex = separatorIndex + 1)
            put(key, value)
        }
    }

    return UploadAppVersionConfiguration(
        pluginName = values["plugin"].orEmpty(),
        taskName = values["task"].orEmpty(),
        projectName = values["project"].orEmpty(),
        buildType = values["buildType"].orEmpty(),
        versionName = values["versionName"].orEmpty(),
        versionCode = values["versionCode"].orEmpty(),
    )
}
