package az.simplesoft.tooliva.core.storage.index

import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.core.storage.StorageProvider
import az.simplesoft.tooliva.core.storage.StorageScanEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageIndexRepositoryTest {
    private lateinit var database: StorageIndexDatabase
    private lateinit var repository: StorageIndexRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, StorageIndexDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = StorageIndexRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun indexesFiftyThousandRecordsAndSupportsIncrementalQueries() = runBlocking {
        val firstRun = syntheticEntries(50_000)
        val initial = repository.index(FakeStorageProvider(firstRun))

        assertEquals(StorageIndexRunStatus.COMPLETED, initial.status)
        assertEquals(50_000L, initial.filesDiscovered)
        assertEquals(50_000, repository.count(StorageIndexQuery(StorageAccessMode.FULL)))
        assertEquals(5_000, repository.query(StorageIndexQuery(StorageAccessMode.FULL)).size)
        val summaries = repository.categorySummaries(StorageAccessMode.FULL)
        assertEquals(500, summaries.single { it.category == StorageCategory.APK }.fileCount)
        assertTrue(summaries.single { it.category == StorageCategory.APK }.totalBytes > 0L)

        val largest = repository.query(
            StorageIndexQuery(
                accessMode = StorageAccessMode.FULL,
                minimumSizeBytes = 49_000L,
                sortOrder = az.simplesoft.tooliva.core.storage.StorageSortOrder.SIZE,
                limit = 10,
            ),
        )
        assertEquals(10, largest.size)
        assertTrue(largest.zipWithNext().all { (left, right) -> left.sizeBytes >= right.sizeBytes })

        val apkMatches = repository.query(
            StorageIndexQuery(
                accessMode = StorageAccessMode.FULL,
                category = StorageCategory.APK,
                searchQuery = "ToolivaE2E",
                limit = 20,
            ),
        )
        assertTrue(apkMatches.isNotEmpty())
        assertTrue(apkMatches.all { it.category == StorageCategory.APK && it.displayName.contains("ToolivaE2E") })

        val changed = firstRun.mapIndexed { index, entry ->
            if (index == 1_001) entry.copy(sizeBytes = 999_999L, modifiedAtMillis = entry.modifiedAtMillis + 1_000L) else entry
        }.dropLast(100)
        val second = repository.index(FakeStorageProvider(changed))

        assertEquals(StorageIndexRunStatus.COMPLETED, second.status)
        assertEquals(49_900, repository.count(StorageIndexQuery(StorageAccessMode.FULL)))
        val changedResult = repository.query(
            StorageIndexQuery(
                accessMode = StorageAccessMode.FULL,
                minimumSizeBytes = 999_999L,
                limit = 10,
            ),
        )
        assertEquals(1, changedResult.size)
        assertEquals("file-01001.bin", changedResult.single().displayName)
        assertEquals(999_999L, changedResult.single().sizeBytes)
    }

    @Test
    fun canceledGenerationDoesNotReplaceLastSuccessfulIndex() = runBlocking {
        val stable = syntheticEntries(100)
        repository.index(FakeStorageProvider(stable))

        val job = launch {
            try {
                repository.index(FakeStorageProvider(syntheticEntries(50_000), delayEvery = 32))
            } catch (_: CancellationException) {
                // Expected: repository records CANCELED and keeps the old active scope.
            }
        }
        withTimeout(5_000L) {
            while (!job.isActive) delay(1)
            delay(20)
            job.cancel()
            job.join()
        }

        assertEquals(100, repository.count(StorageIndexQuery(StorageAccessMode.FULL)))
        assertTrue(repository.lastSuccessfulScan(StorageAccessMode.FULL) != null)
    }

    private fun syntheticEntries(count: Int): List<StorageEntry> = List(count) { index ->
        val isApk = index % 100 == 0
        val extension = if (isApk) "apk" else "bin"
        val name = if (isApk) "ToolivaE2E-$index.apk" else "file-${index.toString().padStart(5, '0')}.$extension"
        StorageEntry(
            ref = Uri.parse("file:///synthetic/$name"),
            name = name,
            path = "/synthetic/$name",
            category = if (isApk) StorageCategory.APK else StorageCategory.OTHER,
            sizeBytes = index.toLong() + 1L,
            modifiedAtMillis = 1_700_000_000_000L + index,
            mimeType = if (isApk) "application/vnd.android.package-archive" else "application/octet-stream",
            extension = extension,
            volumeId = "synthetic-volume",
        )
    }

    private class FakeStorageProvider(
        private val entries: List<StorageEntry>,
        private val delayEvery: Int = 0,
    ) : StorageProvider {
        override val accessMode: StorageAccessMode = StorageAccessMode.FULL

        override fun scan(minBytes: Long): Flow<StorageScanEvent> = flow {
            emit(StorageScanEvent.Started)
            emit(StorageScanEvent.RootStarted("synthetic-volume"))
            var indexedBytes = 0L
            entries.forEachIndexed { index, entry ->
                emit(StorageScanEvent.EntryFound(entry))
                indexedBytes += entry.sizeBytes
                if (delayEvery > 0 && index % delayEvery == 0) delay(1)
                if ((index + 1) % StorageIndexRepository.BATCH_SIZE == 0) {
                    emit(StorageScanEvent.Progress(index + 1L, 0L, indexedBytes))
                }
            }
            emit(StorageScanEvent.RootCompleted("synthetic-volume", true))
            emit(StorageScanEvent.Completed(setOf("synthetic-volume")))
        }
    }
}
