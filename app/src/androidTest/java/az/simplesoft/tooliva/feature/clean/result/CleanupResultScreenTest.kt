package az.simplesoft.tooliva.feature.clean.result

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import az.simplesoft.tooliva.core.media.CleanupResult
import az.simplesoft.tooliva.core.media.CleanupResultStatus
import az.simplesoft.tooliva.ui.theme.ToolivaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CleanupResultScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun completedResultShowsVerifiedCounters() {
        composeRule.setContent {
            ToolivaTheme {
                CleanupResultScreen(
                    result = CleanupResult(
                        status = CleanupResultStatus.COMPLETED,
                        requestedCount = 2,
                        requestedBytes = 3_000L,
                        removedFromActiveCount = 2,
                        removedFromActiveBytes = 3_000L,
                        trashedCount = 1,
                        trashedBytes = 1_000L,
                        freedCount = 1,
                        freedBytes = 2_000L,
                        missingBeforeCount = 0,
                        missingBeforeBytes = 0L,
                        failedCount = 0,
                        failedBytes = 0L,
                        unchangedCount = 0,
                        unchangedBytes = 0L,
                    ),
                    onDone = {},
                )
            }
        }

        composeRule.onNodeWithText("Cleanup complete").assertIsDisplayed()
        composeRule.onNodeWithText("2 files").assertIsDisplayed()
        composeRule.onNodeWithText("Moved to Trash").assertIsDisplayed()
        composeRule.onNodeWithText("Space freed").assertIsDisplayed()
    }

    @Test
    fun canceledResultExplainsThatNothingChanged() {
        composeRule.setContent {
            ToolivaTheme {
                CleanupResultScreen(
                    result = CleanupResult(
                        status = CleanupResultStatus.CANCELED,
                        requestedCount = 1,
                        requestedBytes = 1_000L,
                        removedFromActiveCount = 0,
                        removedFromActiveBytes = 0L,
                        trashedCount = 0,
                        trashedBytes = 0L,
                        freedCount = 0,
                        freedBytes = 0L,
                        missingBeforeCount = 0,
                        missingBeforeBytes = 0L,
                        failedCount = 0,
                        failedBytes = 0L,
                        unchangedCount = 1,
                        unchangedBytes = 1_000L,
                        note = "No file was changed because the system confirmation was canceled.",
                    ),
                    onDone = {},
                )
            }
        }

        composeRule.onNodeWithText("Cleanup canceled").assertIsDisplayed()
        composeRule.onNodeWithText("No file was changed because the system confirmation was canceled.").assertIsDisplayed()
    }

    @Test
    fun partialResultShowsMissingAndUnconfirmedItems() {
        composeRule.setContent {
            ToolivaTheme {
                CleanupResultScreen(
                    result = CleanupResult(
                        status = CleanupResultStatus.PARTIAL,
                        requestedCount = 3,
                        requestedBytes = 6_000L,
                        removedFromActiveCount = 1,
                        removedFromActiveBytes = 1_000L,
                        trashedCount = 1,
                        trashedBytes = 1_000L,
                        freedCount = 0,
                        freedBytes = 0L,
                        missingBeforeCount = 1,
                        missingBeforeBytes = 2_000L,
                        failedCount = 1,
                        failedBytes = 3_000L,
                        unchangedCount = 1,
                        unchangedBytes = 3_000L,
                    ),
                    onDone = {},
                )
            }
        }

        composeRule.onNodeWithText("Cleanup partially completed").assertIsDisplayed()
        composeRule.onNodeWithText("Already gone before action").assertIsDisplayed()
        composeRule.onNodeWithText("Still present / not confirmed").assertIsDisplayed()
    }
}
