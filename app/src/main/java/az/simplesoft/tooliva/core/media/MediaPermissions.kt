package az.simplesoft.tooliva.core.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

fun requiredMediaPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
    )
} else {
    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

fun hasRequiredMediaPermissions(context: Context): Boolean = requiredMediaPermissions().all { permission ->
    context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}
