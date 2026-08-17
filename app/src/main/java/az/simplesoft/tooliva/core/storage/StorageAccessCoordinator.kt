package az.simplesoft.tooliva.core.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

class StorageAccessCoordinator(context: Context) {
    private val appContext = context.applicationContext

    fun currentState(): StorageAccessState {
        val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val granted = supported && Environment.isExternalStorageManager()
        return StorageAccessState(
            fullStorageSupported = supported,
            allFilesAccessGranted = granted,
        )
    }

    fun allFilesSettingsIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${appContext.packageName}")
        }
    }
}

