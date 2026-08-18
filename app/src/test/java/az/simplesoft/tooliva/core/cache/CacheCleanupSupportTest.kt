package az.simplesoft.tooliva.core.cache

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CacheCleanupSupportTest {
    @Test
    fun apiBelowThirtyIsUnsupported() {
        assertEquals(
            CacheCleanupAvailability.UNSUPPORTED,
            CacheCleanupSupport.availability(29, fullStorageGranted = true, intentResolvable = true),
        )
    }

    @Test
    fun apiThirtyWithFullAccessAndSystemActivityIsAvailable() {
        assertEquals(
            CacheCleanupAvailability.AVAILABLE,
            CacheCleanupSupport.availability(30, fullStorageGranted = true, intentResolvable = true),
        )
    }

    @Test
    fun missingAccessOrActivityIsNotAvailable() {
        assertEquals(
            CacheCleanupAvailability.PERMISSION_REQUIRED,
            CacheCleanupSupport.availability(30, fullStorageGranted = false, intentResolvable = true),
        )
        assertEquals(
            CacheCleanupAvailability.UNSUPPORTED,
            CacheCleanupSupport.availability(30, fullStorageGranted = true, intentResolvable = false),
        )
    }

    @Test
    fun resultMappingIsHonest() {
        assertEquals(CacheCleanupResult.SUCCESS, CacheCleanupSupport.mapResult(Activity.RESULT_OK))
        assertEquals(CacheCleanupResult.CANCELED, CacheCleanupSupport.mapResult(Activity.RESULT_CANCELED))
        assertEquals(CacheCleanupResult.FAILED, CacheCleanupSupport.mapResult(-5))
    }

    @Test
    fun repeatedLaunchIsBlockedUntilSystemReturns() {
        assertTrue(CacheCleanupSupport.canBeginLaunch(CacheCleanupAvailability.AVAILABLE, awaitingResult = false))
        assertFalse(CacheCleanupSupport.canBeginLaunch(CacheCleanupAvailability.AVAILABLE, awaitingResult = true))
        assertFalse(CacheCleanupSupport.canBeginLaunch(CacheCleanupAvailability.PERMISSION_REQUIRED, awaitingResult = false))
    }
}
