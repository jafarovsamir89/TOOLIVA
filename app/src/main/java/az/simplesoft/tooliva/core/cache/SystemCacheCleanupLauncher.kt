package az.simplesoft.tooliva.core.cache

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.content.pm.PackageManager

enum class CacheCleanupAvailability {
    AVAILABLE,
    PERMISSION_REQUIRED,
    UNSUPPORTED,
}

enum class CacheCleanupResult {
    SUCCESS,
    CANCELED,
    FAILED,
}

object CacheCleanupSupport {
    fun availability(apiLevel: Int, fullStorageGranted: Boolean, intentResolvable: Boolean): CacheCleanupAvailability = when {
        apiLevel < Build.VERSION_CODES.R -> CacheCleanupAvailability.UNSUPPORTED
        !fullStorageGranted -> CacheCleanupAvailability.PERMISSION_REQUIRED
        !intentResolvable -> CacheCleanupAvailability.UNSUPPORTED
        else -> CacheCleanupAvailability.AVAILABLE
    }

    fun mapResult(resultCode: Int): CacheCleanupResult = when (resultCode) {
        android.app.Activity.RESULT_OK -> CacheCleanupResult.SUCCESS
        android.app.Activity.RESULT_CANCELED -> CacheCleanupResult.CANCELED
        else -> CacheCleanupResult.FAILED
    }

    fun canBeginLaunch(availability: CacheCleanupAvailability, awaitingResult: Boolean): Boolean =
        availability == CacheCleanupAvailability.AVAILABLE && !awaitingResult
}

/** Small adapter for Android's user-mediated global app-cache cleanup action. */
class SystemCacheCleanupLauncher(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager

    fun availability(): CacheCleanupAvailability {
        val intent = actionIntent() ?: return CacheCleanupAvailability.UNSUPPORTED
        val resolvable = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
        return CacheCleanupSupport.availability(
            apiLevel = Build.VERSION.SDK_INT,
            fullStorageGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager(),
            intentResolvable = resolvable,
        )
    }

    fun actionIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return Intent(StorageManager.ACTION_CLEAR_APP_CACHE)
    }

    fun launchIntent(): Intent? = actionIntent()?.takeIf {
        packageManager.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY) != null
    }

    fun launchForResult(activity: Activity, requestCode: Int): LaunchOutcome {
        val intent = launchIntent() ?: return LaunchOutcome.Unsupported
        return try {
            activity.startActivityForResult(intent, requestCode)
            LaunchOutcome.Started
        } catch (_: ActivityNotFoundException) {
            LaunchOutcome.Unsupported
        } catch (_: SecurityException) {
            LaunchOutcome.Failed
        }
    }

    sealed interface LaunchOutcome {
        data object Started : LaunchOutcome
        data object Unsupported : LaunchOutcome
        data object Failed : LaunchOutcome
    }
}
