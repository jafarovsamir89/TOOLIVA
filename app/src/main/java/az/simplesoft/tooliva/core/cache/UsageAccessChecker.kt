package az.simplesoft.tooliva.core.cache

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings

class UsageAccessChecker(private val context: Context) {
    fun isGranted(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }

    fun settingsIntent() = android.content.Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
}
