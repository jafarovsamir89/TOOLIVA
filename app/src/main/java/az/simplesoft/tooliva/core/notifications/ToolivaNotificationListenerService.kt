package az.simplesoft.tooliva.core.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ToolivaNotificationListenerService : NotificationListenerService() {
    private val serviceJob = SupervisorJob()
    private val persistenceScope = CoroutineScope(serviceJob + Dispatchers.IO.limitedParallelism(1))
    private val repository by lazy { NotificationHistoryRepository(applicationContext) }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val label = runCatching {
            packageManager.getApplicationInfo(sbn.packageName, 0).loadLabel(packageManager).toString()
        }.getOrDefault(sbn.packageName)
        val snapshot = runCatching { NotificationSnapshotExtractor.from(sbn, label) }.getOrNull() ?: return
        persistenceScope.launch {
            runCatching { repository.persistPosted(snapshot) }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val key = sbn.key
        persistenceScope.launch {
            runCatching { repository.markRemoved(key, System.currentTimeMillis()) }
        }
    }

    override fun onDestroy() {
        persistenceScope.cancel()
        super.onDestroy()
    }
}
