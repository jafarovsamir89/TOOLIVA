package az.simplesoft.tooliva.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationHistoryRulesTest {
    @Test
    fun notificationKeyHashIsDeterministicAndDoesNotExposeRawKey() {
        val first = NotificationSnapshotExtractor.hashKey("package|42|tag")
        val second = NotificationSnapshotExtractor.hashKey("package|42|tag")

        assertEquals(first, second)
        assertEquals(64, first.length)
        assertNotEquals("package|42|tag", first)
    }

    @Test
    fun retentionOptionsKeepDefaultAndUntilDeletedSemanticsExplicit() {
        assertEquals(30, NotificationRetention.THIRTY_DAYS.days)
        assertEquals(null, NotificationRetention.UNTIL_DELETED.days)
        assertTrue(NotificationHistoryRange.entries.contains(NotificationHistoryRange.PINNED))
    }
}
