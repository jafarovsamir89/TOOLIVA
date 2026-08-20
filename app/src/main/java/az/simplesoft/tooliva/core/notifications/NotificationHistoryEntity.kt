package az.simplesoft.tooliva.core.notifications

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_history",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["postedAtMillis"]),
        Index(value = ["notificationKeyHash", "removedAtMillis"]),
    ],
)
data class NotificationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val notificationKeyHash: String,
    val packageName: String,
    val appLabelSnapshot: String,
    val postedAtMillis: Long,
    val removedAtMillis: Long? = null,
    val title: String? = null,
    val text: String? = null,
    val bigText: String? = null,
    val subText: String? = null,
    val conversationTitle: String? = null,
    val category: String? = null,
    val channelId: String? = null,
    val isOngoing: Boolean = false,
    val isClearable: Boolean = true,
    val groupKey: String? = null,
    val notificationId: Int? = null,
    val tag: String? = null,
    val createdAtMillis: Long,
    val isPinned: Boolean = false,
    val isReadInHistory: Boolean = false,
)

data class NotificationHistoryAppCount(
    val packageName: String,
    val appLabelSnapshot: String,
    val count: Int,
)

data class NotificationSnapshot(
    val notificationKeyHash: String,
    val packageName: String,
    val appLabelSnapshot: String,
    val postedAtMillis: Long,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val subText: String?,
    val conversationTitle: String?,
    val category: String?,
    val channelId: String?,
    val isOngoing: Boolean,
    val isClearable: Boolean,
    val groupKey: String?,
    val notificationId: Int?,
    val tag: String?,
)

enum class NotificationHistoryRange(val label: String, val days: Int?) {
    ALL("All", null),
    TODAY("Today", 1),
    SEVEN_DAYS("7 days", 7),
    THIRTY_DAYS("30 days", 30),
    PINNED("Pinned", null),
}

enum class NotificationRetention(val label: String, val days: Int?) {
    ONE_DAY("1 day", 1),
    SEVEN_DAYS("7 days", 7),
    THIRTY_DAYS("30 days", 30),
    NINETY_DAYS("90 days", 90),
    UNTIL_DELETED("Until I delete it", null),
}
