package az.simplesoft.tooliva.core.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CacheCleaningStateMachineTest {
    @Test
    fun wrongTargetCannotOpenStorage() {
        val decision = CacheCleaningStateMachine.decide(
            CacheCleaningStep.APP_INFO,
            listOf(CacheCleaningNode("Storage", null, true)),
            targetAppConfirmed = false,
        )
        assertEquals(CacheCleaningAction.NONE, decision.action)
    }

    @Test
    fun storageControlCanBeOpenedOnlyAfterTargetConfirmation() {
        val decision = CacheCleaningStateMachine.decide(
            CacheCleaningStep.APP_INFO,
            listOf(CacheCleaningNode("Storage & cache", null, true)),
            targetAppConfirmed = true,
        )
        assertEquals(CacheCleaningAction.OPEN_STORAGE, decision.action)
    }

    @Test
    fun clearDataNeverMatchesClearCache() {
        val decision = CacheCleaningStateMachine.decide(
            CacheCleaningStep.STORAGE,
            listOf(CacheCleaningNode("Clear data", null, true)),
            targetAppConfirmed = true,
        )
        assertNotEquals(CacheCleaningAction.CLICK_CLEAR_CACHE, decision.action)
        assertEquals(CacheCleaningAction.FAIL, decision.action)
    }

    @Test
    fun onlyExactClearCacheLabelCanBeClicked() {
        val decision = CacheCleaningStateMachine.decide(
            CacheCleaningStep.STORAGE,
            listOf(CacheCleaningNode("Clear cache", null, true)),
            targetAppConfirmed = true,
        )
        assertEquals(CacheCleaningAction.CLICK_CLEAR_CACHE, decision.action)
    }
}
