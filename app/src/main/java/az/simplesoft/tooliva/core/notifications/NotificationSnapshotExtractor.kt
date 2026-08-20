package az.simplesoft.tooliva.core.notifications

import android.app.Notification
import android.service.notification.StatusBarNotification
import java.security.MessageDigest

object NotificationSnapshotExtractor {
    fun from(
        statusBarNotification: StatusBarNotification,
        packageLabel: String,
    ): NotificationSnapshot {
        val notification = statusBarNotification.notification
        val extras = notification.extras
        return NotificationSnapshot(
            notificationKeyHash = hashKey(statusBarNotification.key),
            packageName = statusBarNotification.packageName,
            appLabelSnapshot = packageLabel.ifBlank { statusBarNotification.packageName },
            postedAtMillis = statusBarNotification.postTime,
            title = extras.safeText(Notification.EXTRA_TITLE),
            text = extras.safeText(Notification.EXTRA_TEXT),
            bigText = extras.safeText(Notification.EXTRA_BIG_TEXT),
            subText = extras.safeText(Notification.EXTRA_SUB_TEXT),
            conversationTitle = extras.safeText(Notification.EXTRA_CONVERSATION_TITLE),
            category = notification.category,
            channelId = notification.channelId,
            isOngoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0,
            isClearable = notification.flags and Notification.FLAG_NO_CLEAR == 0,
            groupKey = statusBarNotification.groupKey,
            notificationId = statusBarNotification.id,
            tag = statusBarNotification.tag,
        )
    }

    fun hashKey(key: String): String = MessageDigest.getInstance("SHA-256")
        .digest(key.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun android.os.Bundle?.safeText(key: String): String? = runCatching {
        this?.getCharSequence(key)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }.getOrNull()
}
