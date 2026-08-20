package az.simplesoft.tooliva.core.notifications

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class NotificationHistoryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = NotificationHistoryDatabase.get(appContext)
    private val dao = database.notificationHistoryDao()
    private val preferences = NotificationHistoryPreferences(appContext)

    fun isAccessGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return false
        return manager.isNotificationListenerAccessGranted(ComponentName(appContext, ToolivaNotificationListenerService::class.java))
    }

    fun accessIntent(): Intent {
        val component = ComponentName(appContext, ToolivaNotificationListenerService::class.java)
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val isXiaomiFamily = manufacturer.contains("xiaomi") || brand.contains("xiaomi") || brand.contains("redmi")
        val detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !isXiaomiFamily) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                component,
            )
        } else null
        return detail?.takeIf { appContext.packageManager.resolveActivity(it, 0) != null }
            ?: Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }

    fun observeHistory(
        query: String,
        range: NotificationHistoryRange,
        packageFilter: String?,
    ): Flow<List<NotificationHistoryEntity>> = dao.observeHistory(
        queryText = query.trim(),
        startMillis = rangeStart(range),
        packageFilter = packageFilter,
        pinnedOnly = if (range == NotificationHistoryRange.PINNED) 1 else 0,
    )

    fun observeAppCounts(range: NotificationHistoryRange): Flow<List<NotificationHistoryAppCount>> =
        dao.observeAppCounts(
            startMillis = rangeStart(range).takeIf { range != NotificationHistoryRange.PINNED } ?: 0L,
            pinnedOnly = if (range == NotificationHistoryRange.PINNED) 1 else 0,
        )

    suspend fun persistPosted(snapshot: NotificationSnapshot) {
        if (snapshot.packageName == appContext.packageName || preferences.paused) return
        if (snapshot.packageName in preferences.excludedPackages()) return
        if (snapshot.isOngoing && !preferences.includeOngoing) return

        val current = dao.findActive(snapshot.notificationKeyHash)
        if (current == null) {
            dao.insert(snapshot.toEntity())
        } else {
            dao.update(snapshot.toEntity(id = current.id, createdAtMillis = current.createdAtMillis, isPinned = current.isPinned, isRead = current.isReadInHistory))
        }
        pruneExpired()
    }

    suspend fun persistPosted(statusBarNotification: StatusBarNotification, packageLabel: String) {
        persistPosted(NotificationSnapshotExtractor.from(statusBarNotification, packageLabel))
    }

    suspend fun markRemoved(notificationKey: String, removedAtMillis: Long) {
        dao.markRemoved(NotificationSnapshotExtractor.hashKey(notificationKey), removedAtMillis)
    }

    suspend fun setPinned(id: Long, pinned: Boolean) = dao.setPinned(id, pinned)
    suspend fun delete(entity: NotificationHistoryEntity) = dao.delete(entity)
    suspend fun deleteForPackage(packageName: String) = dao.deleteForPackage(packageName)
    suspend fun deleteAll() = dao.deleteAll()
    suspend fun pruneExpired() {
        preferences.retention.days?.let { days ->
            dao.deleteExpired(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong()))
        }
    }

    fun preferences(): NotificationHistoryPreferences = preferences

    private fun rangeStart(range: NotificationHistoryRange): Long = when (range) {
        NotificationHistoryRange.ALL, NotificationHistoryRange.PINNED -> 0L
        NotificationHistoryRange.TODAY -> todayStartMillis()
        NotificationHistoryRange.SEVEN_DAYS -> System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        NotificationHistoryRange.THIRTY_DAYS -> System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
    }

    private fun todayStartMillis(): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun NotificationSnapshot.toEntity(
        id: Long = 0L,
        createdAtMillis: Long = System.currentTimeMillis(),
        isPinned: Boolean = false,
        isRead: Boolean = false,
    ) = NotificationHistoryEntity(
        id = id,
        notificationKeyHash = notificationKeyHash,
        packageName = packageName,
        appLabelSnapshot = appLabelSnapshot,
        postedAtMillis = postedAtMillis,
        title = title,
        text = text,
        bigText = bigText,
        subText = subText,
        conversationTitle = conversationTitle,
        category = category,
        channelId = channelId,
        isOngoing = isOngoing,
        isClearable = isClearable,
        groupKey = groupKey,
        notificationId = notificationId,
        tag = tag,
        createdAtMillis = createdAtMillis,
        isPinned = isPinned,
        isReadInHistory = isRead,
    )
}
