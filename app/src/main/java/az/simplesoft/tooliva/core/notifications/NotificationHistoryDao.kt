package az.simplesoft.tooliva.core.notifications

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationHistoryDao {
    @Query(
        """
        SELECT * FROM notification_history
        WHERE (:queryText = '' OR lower(appLabelSnapshot) LIKE '%' || lower(:queryText) || '%'
            OR lower(packageName) LIKE '%' || lower(:queryText) || '%'
            OR lower(coalesce(title, '')) LIKE '%' || lower(:queryText) || '%'
            OR lower(coalesce(text, '')) LIKE '%' || lower(:queryText) || '%'
            OR lower(coalesce(bigText, '')) LIKE '%' || lower(:queryText) || '%')
          AND (:startMillis <= 0 OR postedAtMillis >= :startMillis)
          AND (:packageFilter IS NULL OR packageName = :packageFilter)
          AND (:pinnedOnly = 0 OR isPinned = 1)
        ORDER BY postedAtMillis DESC
        """,
    )
    fun observeHistory(
        queryText: String,
        startMillis: Long,
        packageFilter: String?,
        pinnedOnly: Int,
    ): Flow<List<NotificationHistoryEntity>>

    @Query(
        """
        SELECT packageName, appLabelSnapshot, COUNT(*) AS count
        FROM notification_history
        WHERE (:startMillis <= 0 OR postedAtMillis >= :startMillis)
          AND (:pinnedOnly = 0 OR isPinned = 1)
        GROUP BY packageName, appLabelSnapshot
        ORDER BY count DESC, appLabelSnapshot COLLATE NOCASE ASC
        """,
    )
    fun observeAppCounts(startMillis: Long, pinnedOnly: Int): Flow<List<NotificationHistoryAppCount>>

    @Query("SELECT * FROM notification_history WHERE notificationKeyHash = :keyHash AND removedAtMillis IS NULL ORDER BY id DESC LIMIT 1")
    suspend fun findActive(keyHash: String): NotificationHistoryEntity?

    @Insert
    suspend fun insert(entity: NotificationHistoryEntity): Long

    @Update
    suspend fun update(entity: NotificationHistoryEntity)

    @Query("UPDATE notification_history SET removedAtMillis = :removedAtMillis WHERE notificationKeyHash = :keyHash AND removedAtMillis IS NULL")
    suspend fun markRemoved(keyHash: String, removedAtMillis: Long)

    @Query("UPDATE notification_history SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Delete
    suspend fun delete(entity: NotificationHistoryEntity)

    @Query("DELETE FROM notification_history WHERE packageName = :packageName")
    suspend fun deleteForPackage(packageName: String)

    @Query("DELETE FROM notification_history")
    suspend fun deleteAll()

    @Query("DELETE FROM notification_history WHERE postedAtMillis < :cutoffMillis AND isPinned = 0")
    suspend fun deleteExpired(cutoffMillis: Long)
}
