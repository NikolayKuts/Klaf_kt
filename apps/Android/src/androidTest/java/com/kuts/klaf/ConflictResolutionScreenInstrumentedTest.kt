package com.kuts.klaf

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kuts.domain.common.ConflictResolutionAction
import com.kuts.domain.common.ConflictResolutionDecision
import com.kuts.klaf.deckList.conflictResolution.ConflictDestination
import com.kuts.klaf.deckList.conflictResolution.ConflictResolutionScreen
import com.kuts.klaf.deckList.conflictResolution.ConflictResolutionUiModel
import com.kuts.klaf.deckList.conflictResolution.ConflictUiEntry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.RuleChain

private val actionLabels = mapOf(
    ConflictResolutionAction.ACCEPT_SERVER to "Accept server changes",
    ConflictResolutionAction.KEEP_LOCAL_DECK to "Keep my deck changes",
    ConflictResolutionAction.KEEP_LOCAL_CARD to "Keep my card changes",
    ConflictResolutionAction.RESCUE_MOVED_CARD to "Keep the moved card",
    ConflictResolutionAction.RESTORE_DELETED_DECK to "Restore my deck and cards",
    ConflictResolutionAction.KEEP_REMOVAL_RETAIN_SCHEDULE to "Keep card removal and review schedule",
    ConflictResolutionAction.KEEP_REMOVAL_DUE_NOW to "Keep card removal; make deck due now",
    ConflictResolutionAction.RETARGET_MOVED_CARD to "Move card to another deck",
)

/** Renders the production screen in the isolated package; no database or REST writes. */
@RunWith(AndroidJUnit4::class)
class ConflictResolutionScreenInstrumentedTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val isolatedActivity = RuleChain.outerRule(IsolatedStorageTestRule()).around(compose)

    @Test
    fun everyBulkActionHasItsOwnLabelAndDispatchesTheCorrectChoice() {
        val choices = mutableListOf<ConflictResolutionAction>()
        val targets = mutableListOf<Pair<String?, String?>>()
        render(
            model = ConflictResolutionUiModel(2L, emptyList(), actionLabels.keys, false),
            onAction = choices::add,
            onRetarget = { id, name -> targets += id to name },
        )

        actionLabels.forEach { (action, label) ->
            compose.onNodeWithText(label).performScrollTo().assertIsEnabled().performClick()
            compose.runOnIdle {
                if (action != ConflictResolutionAction.RETARGET_MOVED_CARD) assertEquals(action, choices.last())
            }
        }
        compose.runOnIdle {
            assertEquals(actionLabels.keys.filter { it != ConflictResolutionAction.RETARGET_MOVED_CARD }, choices)
            assertEquals(emptyList<Pair<String?, String?>>(), targets)
        }
        compose.onNodeWithText("Target deck").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("target" to null), targets) }
        compose.onNodeWithText("Create deck and move card").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("New deck name").performScrollTo().performTextInput("New target")
        compose.onNodeWithText("Create deck and move card").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(listOf("target" to null, null to "New target"), targets) }
    }

    @Test
    fun workingScreenDisablesEveryBulkResolutionAction() {
        render(ConflictResolutionUiModel(2L, emptyList(), actionLabels.keys, false), isWorking = true)

        actionLabels.values.forEach { label ->
            compose.onNodeWithText(label).performScrollTo().assertIsNotEnabled()
        }
    }

    @Test
    fun manualChoicesRequireEveryConflictAndKeepTheirOperationIds() {
        var submitted = emptyList<ConflictResolutionDecision>()
        val server = ConflictResolutionAction.ACCEPT_SERVER
        val localDeck = ConflictResolutionAction.KEEP_LOCAL_DECK
        val localCard = ConflictResolutionAction.KEEP_LOCAL_CARD
        render(
            model = ConflictResolutionUiModel(
                revision = 2L,
                conflicts = listOf(
                    ConflictUiEntry("deck-edit", "Rename deck", "Server deck", emptyList(), setOf(server, localDeck)),
                    ConflictUiEntry("card-edit", "Edit card", "Server card", emptyList(), setOf(server, localCard)),
                ),
                availableActions = setOf(server),
                manualSelectionAvailable = true,
            ),
            onSelected = { submitted = it },
        )

        compose.onNodeWithText("Apply selected choices").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("○ Keep my deck changes").performScrollTo().performClick()
        compose.onNodeWithText("Apply selected choices").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("○ Keep my card changes").performScrollTo().performClick()
        compose.onNodeWithText("Apply selected choices").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(listOf(
                ConflictResolutionDecision("deck-edit", localDeck),
                ConflictResolutionDecision("card-edit", localCard),
            ), submitted)
        }
    }

    private fun render(
        model: ConflictResolutionUiModel,
        isWorking: Boolean = false,
        onAction: (ConflictResolutionAction) -> Unit = {},
        onRetarget: (String?, String?) -> Unit = { _, _ -> },
        onSelected: (List<ConflictResolutionDecision>) -> Unit = {},
    ) {
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                ConflictResolutionScreen(
                    model = model,
                    destinations = listOf(ConflictDestination("target", "Target deck")),
                    isWorking = isWorking,
                    error = null,
                    onAction = onAction,
                    onRetarget = onRetarget,
                    onSelectedActions = onSelected,
                    onClose = {},
                )
            }
        }
    }
}
