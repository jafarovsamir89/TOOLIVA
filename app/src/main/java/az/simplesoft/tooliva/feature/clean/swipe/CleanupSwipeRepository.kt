package az.simplesoft.tooliva.feature.clean.swipe

import android.content.Context
import az.simplesoft.tooliva.core.media.ScreenshotMediaRepository
import az.simplesoft.tooliva.core.storage.FullStorageProvider
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.core.storage.StorageScanEvent
import az.simplesoft.tooliva.core.storage.StorageScanScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed interface CleanupSwipeLoadEvent {
    data object Started : CleanupSwipeLoadEvent
    data class Entry(val entry: StorageEntry) : CleanupSwipeLoadEvent
    data class Progress(val filesChecked: Long) : CleanupSwipeLoadEvent
    data class Warning(val path: String) : CleanupSwipeLoadEvent
    data object Completed : CleanupSwipeLoadEvent
}

class CleanupSwipeRepository(context: Context) {
    private val appContext = context.applicationContext
    private val storage = FullStorageProvider(appContext)

    fun scan(category: CleanupSwipeCategory): Flow<CleanupSwipeLoadEvent> = flow {
        emit(CleanupSwipeLoadEvent.Started)
        if (category == CleanupSwipeCategory.SCREENSHOTS) {
            ScreenshotMediaRepository(appContext).scanAll(fullStorageAccess = true).collect { screenshot ->
                emit(
                    CleanupSwipeLoadEvent.Entry(
                        StorageEntry(
                            ref = screenshot.uri,
                            name = screenshot.displayName,
                            path = screenshot.relativePath ?: screenshot.uri.toString(),
                            category = StorageCategory.IMAGE,
                            sizeBytes = screenshot.sizeBytes,
                            modifiedAtMillis = screenshot.ageTimestampMillis,
                            mimeType = screenshot.mimeType,
                            extension = screenshot.displayName.substringAfterLast('.', "").lowercase().takeIf { it.isNotBlank() },
                        ),
                    ),
                )
            }
        } else {
            val scope = if (category == CleanupSwipeCategory.DOWNLOADS) StorageScanScope.DOWNLOADS else StorageScanScope.ALL_STORAGE
            storage.scan(0L, scope).collect { event ->
                when (event) {
                    StorageScanEvent.Started -> Unit
                    is StorageScanEvent.EntryFound -> if (matches(category, event.entry)) emit(CleanupSwipeLoadEvent.Entry(event.entry))
                    is StorageScanEvent.Progress -> emit(CleanupSwipeLoadEvent.Progress(event.visitedFiles))
                    is StorageScanEvent.Warning -> emit(CleanupSwipeLoadEvent.Warning(event.path))
                    StorageScanEvent.Completed -> Unit
                }
            }
        }
        emit(CleanupSwipeLoadEvent.Completed)
    }

    private fun matches(category: CleanupSwipeCategory, entry: StorageEntry): Boolean = when (category) {
        CleanupSwipeCategory.IMAGES -> entry.category == StorageCategory.IMAGE
        CleanupSwipeCategory.VIDEOS -> entry.category == StorageCategory.VIDEO
        CleanupSwipeCategory.DOWNLOADS -> !entry.isDirectory
        CleanupSwipeCategory.LARGE_FILES -> entry.sizeBytes >= 100L * 1024L * 1024L
        CleanupSwipeCategory.SCREENSHOTS -> false
    }
}
