package az.simplesoft.tooliva.core.storage

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

data class ApkMetadata(
    val label: String?,
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
)

object ApkMetadataReader {
    fun read(context: Context, entry: StorageEntry): ApkMetadata? {
        if (entry.category != StorageCategory.APK || entry.ref.scheme != "file") return null
        val path = entry.ref.path ?: return null
        val flags = PackageManager.GET_META_DATA
        val packageInfo: PackageInfo = runCatching {
            context.packageManager.getPackageArchiveInfo(path, flags)
        }.getOrNull() ?: return null
        val applicationInfo = packageInfo.applicationInfo?.apply { sourceDir = path; publicSourceDir = path }
        val label = applicationInfo?.let { context.packageManager.getApplicationLabel(it).toString() }
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
        }
        return ApkMetadata(label, packageInfo.packageName, packageInfo.versionName, versionCode)
    }
}
