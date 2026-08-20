package az.simplesoft.tooliva.core.notifications

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationHistoryDaoTest {
    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        NotificationHistoryDatabase::class.java,
    ).allowMainThreadQueries().build()
    private val dao = database.notificationHistoryDao()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun activeKeyAndRetentionQueriesPreservePinnedRows() = runBlocking {
        val now = System.currentTimeMillis()
        val old = entity("key-old", now - TimeUnit.DAYS.toMillis(40), pinned = false)
        val pinned = entity("key-pinned", now - TimeUnit.DAYS.toMillis(40), pinned = true)
        dao.insert(old)
        dao.insert(pinned)

        assertNotNull(dao.findActive("key-old"))
        dao.deleteExpired(now - TimeUnit.DAYS.toMillis(30))

        assertEquals(null, dao.findActive("key-old"))
        assertNotNull(dao.findActive("key-pinned"))
        assertEquals(1, dao.observeAppCounts(0L, 0).first().single().count)
    }

    @Test
    fun searchAndPackageFilterAreAppliedByDao() = runBlocking {
        dao.insert(entity("key-a", 1000L, packageName = "com.example.mail", title = "Receipt"))
        dao.insert(entity("key-b", 2000L, packageName = "com.example.chat", title = "Hello"))

        assertEquals(1, dao.observeHistory("receipt", 0L, null, 0).first().size)
        assertEquals("com.example.chat", dao.observeHistory("", 0L, "com.example.chat", 0).first().single().packageName)
    }

    private fun entity(key: String, postedAt: Long, packageName: String = "com.example.mail", title: String = "Message", pinned: Boolean = false) = NotificationHistoryEntity(
        notificationKeyHash = key,
        packageName = packageName,
        appLabelSnapshot = packageName.substringAfterLast('.'),
        postedAtMillis = postedAt,
        title = title,
        text = "Local text",
        createdAtMillis = postedAt,
        isPinned = pinned,
    )
}
