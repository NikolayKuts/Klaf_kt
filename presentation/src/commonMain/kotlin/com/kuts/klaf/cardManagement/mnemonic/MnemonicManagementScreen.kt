package com.kuts.klaf.cardManagement.mnemonic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.kuts.domain.entities.AgentDriverConnectionState
import com.kuts.domain.entities.canSendRequests
import com.kuts.klaf.cardManagement.cardAddition.CardAdditionViewModel
import com.kuts.klaf.cardManagement.cardEditing.CardEditingViewModel
import com.kuts.klaf.cardManagement.common.BaseCardManagementViewModel
import com.kuts.klaf.cardManagement.common.MnemonicImageVariantUiState
import com.kuts.klaf.cardManagement.common.MnemonicManagementUiState
import com.kuts.klaf.cardManagement.common.MnemonicVariantUiState
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.ContentHolder
import com.kuts.klaf.common.DIALOG_APP_LABEL_SIZE
import com.kuts.klaf.common.DialogAppLabel
import com.kuts.klaf.common.FullBackgroundDialog
import com.kuts.klaf.common.RoundButton
import com.kuts.klaf.navigation.AppDestination
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.ic_confirmation_24
import com.kuts.klaf.presentation.resources.ic_logout_24
import com.kuts.klaf.presentation.resources.mnemonic_image_comment_label
import com.kuts.klaf.presentation.resources.mnemonic_image_request_action
import com.kuts.klaf.presentation.resources.mnemonic_management_comment_label
import com.kuts.klaf.presentation.resources.mnemonic_management_image_empty_state
import com.kuts.klaf.presentation.resources.mnemonic_management_leave_dialog_question
import com.kuts.klaf.presentation.resources.mnemonic_management_request_action
import com.kuts.klaf.presentation.resources.mnemonic_management_title
import com.kuts.klaf.presentation.resources.mnemonic_management_variants_title
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.absoluteValue

@Composable
internal fun CardAdditionMnemonicManagementScreen(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
) {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(route = AppDestination.CardAddition(deckId = deckId))
    }
    val viewModel: CardAdditionViewModel = koinViewModel(viewModelStoreOwner = owner) {
        parametersOf(deckId, null)
    }

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    Surface {
        MnemonicManagementContent(
            viewModel = viewModel,
            onBack = navController::popBackStack,
        )
    }
}

@Composable
internal fun CardEditingMnemonicManagementScreen(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
    cardId: Int,
) {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(
            route = AppDestination.CardEditing(
                deckId = deckId,
                cardId = cardId,
            ),
        )
    }
    val viewModel: CardEditingViewModel = koinViewModel(viewModelStoreOwner = owner) {
        parametersOf(deckId, cardId)
    }

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    Surface {
        MnemonicManagementContent(
            viewModel = viewModel,
            onBack = navController::popBackStack,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MnemonicManagementContent(
    viewModel: BaseCardManagementViewModel,
    onBack: () -> Unit,
) {
    val mnemonicState by viewModel.mnemonicManagementState.collectAsState()
    val cardManagementState by viewModel.cardManagementState.collectAsState()
    val connectionState by viewModel.agentDriverConnectionState.collectAsState()
    val selectedVariant = mnemonicState.selectedVariant
    val foreignWord = cardManagementState.foreignWordFieldValue.text.trim()
    val initialSnapshot = remember { mnemonicState }
    val scrollState = rememberScrollState()
    val selectedVariantImageCount = selectedVariant?.imageVariants?.size ?: 0
    val hasSavableContent = mnemonicState.hasSavableContent
    var previousImageCount by remember(selectedVariant?.id) {
        mutableStateOf(selectedVariantImageCount)
    }
    var showLeaveDialog by remember { mutableStateOf(false) }

    val onDiscardAndLeave = remember(viewModel, initialSnapshot, onBack) {
        {
            viewModel.restoreMnemonicManagementState(snapshot = initialSnapshot)
            onBack()
        }
    }

    BackHandler {
        if (showLeaveDialog) {
            showLeaveDialog = false
        } else if (hasSavableContent) {
            showLeaveDialog = true
        } else {
            onBack()
        }
    }

    LaunchedEffect(selectedVariant?.id, selectedVariantImageCount, mnemonicState.isImageLoading) {
        if (!mnemonicState.isImageLoading && selectedVariantImageCount > previousImageCount) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        previousImageCount = selectedVariantImageCount
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MnemonicManagementHeader(
            hasSavableContent = hasSavableContent,
            onSaveAndLeave = onBack,
        )

        Text(text = foreignWord)

        MnemonicAssociationRequestSection(
            mnemonicState = mnemonicState,
            connectionState = connectionState,
            onCommentChange = viewModel::updateMnemonicRequestComment,
            onRequest = viewModel::requestMnemonicAssociation,
        )

        if (mnemonicState.variants.isNotEmpty()) {
            MnemonicVariantsSection(
                mnemonicState = mnemonicState,
                selectedVariant = selectedVariant,
                onSelectVariant = viewModel::selectMnemonicVariant,
            )

            selectedVariant?.let { variant ->
                MnemonicImageRequestSection(
                    mnemonicState = mnemonicState,
                    connectionState = connectionState,
                    selectedVariant = variant,
                    onCommentChange = viewModel::updateMnemonicImageRequestComment,
                    onRequestImage = viewModel::requestMnemonicImage,
                    onSelectImage = { imageId ->
                        viewModel.selectMnemonicImageVariant(
                            variantId = variant.id,
                            imageId = imageId,
                        )
                    },
                )
            }
        }
    }

    if (showLeaveDialog) {
        MnemonicLeaveDialog(
            onDismiss = { showLeaveDialog = false },
            onSaveAndLeave = {
                showLeaveDialog = false
                onBack()
            },
            onDiscardAndLeave = {
                showLeaveDialog = false
                onDiscardAndLeave()
            },
        )
    }
}

@Composable
private fun MnemonicManagementHeader(
    hasSavableContent: Boolean,
    onSaveAndLeave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.mnemonic_management_title),
            fontWeight = FontWeight.Bold,
        )
        if (hasSavableContent) {
            RoundButton(
                background = MainTheme.colors.common.positiveDialogButton,
                iconRes = Res.drawable.ic_confirmation_24,
                onClick = onSaveAndLeave,
                size = 40.dp,
            )
        }
    }
}

@Composable
private fun MnemonicAssociationRequestSection(
    mnemonicState: MnemonicManagementUiState,
    connectionState: AgentDriverConnectionState,
    onCommentChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onRequest: () -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = mnemonicState.requestComment,
        onValueChange = onCommentChange,
        label = { Text(text = stringResource(Res.string.mnemonic_management_comment_label)) },
    )

    MnemonicActionButton(
        label = stringResource(Res.string.mnemonic_management_request_action),
        isLoading = mnemonicState.isAssociationLoading,
        connectionState = connectionState,
        onClick = onRequest,
    )
}

@Composable
private fun MnemonicVariantsSection(
    mnemonicState: MnemonicManagementUiState,
    selectedVariant: MnemonicVariantUiState?,
    onSelectVariant: (String) -> Unit,
) {
    Text(
        text = stringResource(Res.string.mnemonic_management_variants_title),
        fontWeight = FontWeight.SemiBold,
    )

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        LazyRow(
            modifier = Modifier.zIndex(1f),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(mnemonicState.variants, key = MnemonicVariantUiState::id) { variant ->
                VariantChip(
                    variant = variant,
                    isSelected = mnemonicState.selectedVariantId == variant.id,
                    onClick = { onSelectVariant(variant.id) },
                )
            }
        }

        selectedVariant?.let { variant ->
            MnemonicVariantDetails(
                variant = variant,
                modifier = Modifier.offset(y = (-2).dp),
            )
        }
    }
}

@Composable
private fun MnemonicImageRequestSection(
    mnemonicState: MnemonicManagementUiState,
    connectionState: AgentDriverConnectionState,
    selectedVariant: MnemonicVariantUiState,
    onCommentChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onRequestImage: () -> Unit,
    onSelectImage: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = mnemonicState.imageRequestComment,
        onValueChange = onCommentChange,
        label = { Text(text = stringResource(Res.string.mnemonic_image_comment_label)) },
    )

    MnemonicActionButton(
        label = stringResource(Res.string.mnemonic_image_request_action),
        isLoading = mnemonicState.isImageLoading,
        connectionState = connectionState,
        onClick = onRequestImage,
    )

    if (selectedVariant.imageVariants.isEmpty()) {
        Text(text = stringResource(Res.string.mnemonic_management_image_empty_state))
    } else {
        MnemonicImageCarousel(
            imageVariants = selectedVariant.imageVariants,
            selectedImageId = selectedVariant.selectedImageId,
            onSelectImage = onSelectImage,
        )
    }
}

@Composable
private fun MnemonicActionButton(
    label: String,
    isLoading: Boolean,
    connectionState: AgentDriverConnectionState,
    onClick: () -> Unit,
) {
    val isConnectionReady = connectionState.canSendRequests
    Button(
        onClick = onClick,
        enabled = !isLoading && isConnectionReady,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = disabledMnemonicActionButtonColor(
                isLoading = isLoading,
                connectionState = connectionState,
            ),
            disabledContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(18.dp),
                strokeWidth = 2.dp,
            )
        }
        Text(text = label)
    }
}

@Composable
private fun disabledMnemonicActionButtonColor(
    isLoading: Boolean,
    connectionState: AgentDriverConnectionState,
): Color {
    return when {
        isLoading -> MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
        else -> when (connectionState) {
            is AgentDriverConnectionState.Error -> MainTheme.colors.common.negativeDialogButton
            is AgentDriverConnectionState.Reconnecting -> MainTheme.colors.common.agentDriverReconnectingButton
            AgentDriverConnectionState.Disconnected -> MainTheme.colors.common.agentDriverDisconnectedButton
            AgentDriverConnectionState.Ready -> MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
        }
    }
}

@Composable
private fun MnemonicLeaveDialog(
    onDismiss: () -> Unit,
    onSaveAndLeave: () -> Unit,
    onDiscardAndLeave: () -> Unit,
) {
    FullBackgroundDialog(
        onBackgroundClick = onDismiss,
        topContent = ContentHolder(size = DIALOG_APP_LABEL_SIZE.dp) { DialogAppLabel() },
        mainContent = {
            Text(
                text = stringResource(Res.string.mnemonic_management_leave_dialog_question),
                textAlign = TextAlign.Center,
            )
        },
        bottomContent = {
            RoundButton(
                background = MainTheme.colors.common.positiveDialogButton,
                iconRes = Res.drawable.ic_confirmation_24,
                onClick = onSaveAndLeave,
            )
            RoundButton(
                background = MainTheme.colors.common.negativeDialogButton,
                iconRes = Res.drawable.ic_logout_24,
                onClick = onDiscardAndLeave,
            )
        },
    )
}

@Composable
private fun VariantChip(
    variant: MnemonicVariantUiState,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val activeColor = MainTheme.colors.common.separator.copy(alpha = 0.08f)
    val inactiveColor = MainTheme.colors.common.separator.copy(alpha = 0.05f)
    val borderColor = MainTheme.colors.common.separator.copy(alpha = 0.32f)
    val selectedShape = RoundedCornerShape(
        topStart = 14.dp,
        topEnd = 14.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp,
    )
    val unselectedShape = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp,
        bottomStart = 8.dp,
        bottomEnd = 8.dp,
    )
    Box(
        modifier = Modifier
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = borderColor,
                        shape = selectedShape,
                    )
                } else {
                    Modifier
                },
            )
            .background(
                color = if (isSelected) activeColor else inactiveColor,
                shape = if (isSelected) selectedShape else unselectedShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text = variant.selection.candidate.associationForm,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 1.dp)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(color = activeColor),
            )
        }
    }
}

@Composable
private fun MnemonicImageCarousel(
    imageVariants: List<MnemonicImageVariantUiState>,
    selectedImageId: String?,
    onSelectImage: (String) -> Unit,
) {
    val selectedIndex = imageVariants.indexOfFirst { imageVariant -> imageVariant.id == selectedImageId }
        .takeIf { index -> index >= 0 }
        ?: 0
    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { imageVariants.size },
    )

    LaunchedEffect(selectedIndex, imageVariants.size) {
        if (pagerState.currentPage != selectedIndex) {
            pagerState.scrollToPage(selectedIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage, imageVariants) {
        imageVariants.getOrNull(pagerState.currentPage)?.let { imageVariant ->
            if (imageVariant.id != selectedImageId) {
                onSelectImage(imageVariant.id)
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
    ) {
        val pageWidth = (maxWidth * 0.72f).coerceIn(minimumValue = 208.dp, maximumValue = 320.dp)
        val sidePadding = ((maxWidth - pageWidth) / 2).coerceAtLeast(12.dp)

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSize = PageSize.Fixed(pageWidth),
            contentPadding = PaddingValues(horizontal = sidePadding),
            pageSpacing = 10.dp,
            beyondViewportPageCount = 2,
        ) { page ->
            val imageVariant = imageVariants[page]
            val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).absoluteValue.coerceIn(minimumValue = 0f, maximumValue = 1f)
            val scale = 1f - (pageOffset * 0.10f)
            val alpha = 1f - (pageOffset * 0.30f)
            val previewShape = RoundedCornerShape(18.dp)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .zIndex(1f - pageOffset)
                    .shadow(
                        elevation = if (page == pagerState.currentPage) 12.dp else 4.dp,
                        shape = previewShape,
                    )
                    .clip(previewShape)
                    .background(
                        color = MainTheme.colors.common.separator.copy(
                            alpha = if (page == pagerState.currentPage) 0.12f else 0.09f,
                        ),
                        shape = previewShape,
                    )
                    .border(
                        width = if (page == pagerState.currentPage) 1.5.dp else 1.dp,
                        color = if (page == pagerState.currentPage) {
                            MainTheme.colors.common.separator.copy(alpha = 0.40f)
                        } else {
                            MainTheme.colors.common.separator.copy(alpha = 0.22f)
                        },
                        shape = previewShape,
                    )
                    .clickable { onSelectImage(imageVariant.id) },
                contentAlignment = Alignment.Center,
            ) {
                MnemonicImagePreview(
                    imagePath = imageVariant.imagePath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun MnemonicVariantDetails(
    variant: MnemonicVariantUiState,
    modifier: Modifier = Modifier,
) {
    val selection = variant.selection
    val outerBorderColor = MainTheme.colors.common.separator.copy(alpha = 0.32f)
    val outerShape = RoundedCornerShape(16.dp)
    val sections = buildList {
        add(
            MnemonicDetailSectionUi(
                title = "Word & Transcription",
                contentLines = listOf(
                    selection.word,
                    selection.transcription,
                ),
            ),
        )
        add(
            MnemonicDetailSectionUi(
                title = "Translations",
                contentLines = selection.translations,
            ),
        )
        add(
            MnemonicDetailSectionUi(
                title = "Usage Example",
                contentLines = listOf(selection.usageExample),
            ),
        )
        add(
            MnemonicDetailSectionUi(
                title = "Association Form",
                contentLines = listOf(selection.candidate.associationForm),
            ),
        )
        add(
            MnemonicDetailSectionUi(
                title = "Scene",
                contentLines = listOf(selection.candidate.scene),
            ),
        )
        add(
            MnemonicDetailSectionUi(
                title = "Sound Mapping",
                contentLines = listOf(selection.candidate.soundMapping),
            ),
        )
        add(
            MnemonicDetailSectionUi(
                title = "Meaning",
                contentLines = listOf(selection.candidate.meaningMapping),
            ),
        )
        if (variant.requestComment.isNotBlank()) {
            add(
                MnemonicDetailSectionUi(
                    title = "Request Comment",
                    contentLines = listOf(variant.requestComment),
                ),
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = outerBorderColor,
                shape = outerShape,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            sections.forEachIndexed { index, section ->
                MnemonicDetailSection(
                    section = section,
                    shape = when (index) {
                        0 -> RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 2.dp,
                            bottomEnd = 2.dp,
                        )

                        sections.lastIndex -> RoundedCornerShape(
                            topStart = 2.dp,
                            topEnd = 2.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp,
                        )

                        else -> RoundedCornerShape(2.dp)
                    },
                )
            }
        }
    }
}

@Composable
private fun MnemonicDetailSection(
    section: MnemonicDetailSectionUi,
    shape: RoundedCornerShape,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MainTheme.colors.common.separator.copy(alpha = 0.08f),
                shape = shape,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.labelMedium,
            color = MainTheme.colors.common.separator,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        section.contentLines.forEachIndexed { index, line ->
            if (section.title == "Translations") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            } else {
                Text(
                    text = line,
                    style = if (section.title == "Word & Transcription" && index == 0) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

private data class MnemonicDetailSectionUi(
    val title: String,
    val contentLines: List<String>,
)
