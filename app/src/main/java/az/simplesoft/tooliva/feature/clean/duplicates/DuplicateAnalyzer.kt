package az.simplesoft.tooliva.feature.clean.duplicates

import az.simplesoft.tooliva.core.storage.FullStorageProvider
import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.core.storage.StorageScanEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import kotlin.coroutines.coroutineContext

class DuplicateAnalyzer(private val storage: FullStorageProvider) {
    fun analyze(): Flow<DuplicateAnalysisEvent> = flow {
        val startedAt = System.nanoTime()
        emit(DuplicateAnalysisEvent.Started)
        val bySize = LinkedHashMap<Long, MutableList<StorageEntry>>()
        var filesChecked = 0L

        emit(DuplicateAnalysisEvent.Progress(DuplicateAnalysisProgress(DuplicateAnalysisStage.METADATA)))
        for (root in storage.storageRoots()) {
            coroutineContext.ensureActive()
            storage.search(root) { entry ->
                !entry.isDirectory && entry.sizeBytes > 0L && !isToolivaFile(entry)
            }.collect { event ->
                coroutineContext.ensureActive()
                when (event) {
                    is StorageScanEvent.EntryFound -> {
                        filesChecked++
                        bySize.getOrPut(event.entry.sizeBytes) { mutableListOf() }.add(event.entry)
                        emit(DuplicateAnalysisEvent.Progress(DuplicateAnalysisProgress(
                            stage = DuplicateAnalysisStage.METADATA,
                            filesChecked = filesChecked,
                        )))
                    }
                    is StorageScanEvent.Progress -> emit(DuplicateAnalysisEvent.Progress(DuplicateAnalysisProgress(
                        stage = DuplicateAnalysisStage.METADATA,
                        filesChecked = filesChecked,
                    )))
                    else -> Unit
                }
            }
        }

        val candidates = bySize.values.filter { it.size > 1 }
        val candidateFiles = candidates.sumOf { it.size.toLong() }
        emit(DuplicateAnalysisEvent.Progress(DuplicateAnalysisProgress(
            stage = DuplicateAnalysisStage.HASHING,
            filesChecked = filesChecked,
            candidateFiles = candidateFiles,
        )))

        val byHash = LinkedHashMap<HashKey, MutableList<StorageEntry>>()
        var filesHashed = 0L
        var bytesHashed = 0L
        for (candidateGroup in candidates) {
            for (entry in candidateGroup) {
                coroutineContext.ensureActive()
                when (val result = FileHasher.hash(entry)) {
                    is FileHashResult.Valid -> {
                        byHash.getOrPut(HashKey(entry.sizeBytes, result.hash)) { mutableListOf() }.add(entry)
                        bytesHashed += result.bytesRead
                    }
                    is FileHashResult.Invalid -> Unit
                }
                filesHashed++
                emit(DuplicateAnalysisEvent.Progress(DuplicateAnalysisProgress(
                    stage = DuplicateAnalysisStage.HASHING,
                    filesChecked = filesChecked,
                    candidateFiles = candidateFiles,
                    filesHashed = filesHashed,
                    bytesHashed = bytesHashed,
                )))
            }
        }

        val confirmed = mutableListOf<DuplicateGroup>()
        var groupIndex = 0
        val hashGroups = byHash.entries.filter { it.value.size > 1 }
        emit(DuplicateAnalysisEvent.Progress(DuplicateAnalysisProgress(
            stage = DuplicateAnalysisStage.VERIFYING,
            filesChecked = filesChecked,
            candidateFiles = candidateFiles,
            filesHashed = filesHashed,
            bytesHashed = bytesHashed,
        )))
        for ((hashKey, entries) in hashGroups) {
            coroutineContext.ensureActive()
            val reference = File(entries.first().path)
            val verified = entries.drop(1).all { ExactFileVerifier.verify(reference, File(it.path)) }
            if (verified) {
                val group = DuplicateGroup(
                    sessionId = "group-${++groupIndex}",
                    fileSizeBytes = entries.first().sizeBytes,
                    hash = "${hashKey.sizeBytes}:${hashKey.hash}",
                    entries = entries.distinctBy(StorageEntry::path),
                )
                confirmed += group
                emit(DuplicateAnalysisEvent.GroupConfirmed(group))
            }
            emit(DuplicateAnalysisEvent.Progress(DuplicateAnalysisProgress(
                stage = DuplicateAnalysisStage.VERIFYING,
                filesChecked = filesChecked,
                candidateFiles = candidateFiles,
                filesHashed = filesHashed,
                bytesHashed = bytesHashed,
                groupsConfirmed = confirmed.size,
            )))
        }

        emit(DuplicateAnalysisEvent.Completed(DuplicateAnalysisSummary(
            groups = confirmed,
            filesChecked = filesChecked,
            candidateFiles = candidateFiles,
            filesHashed = filesHashed,
            bytesHashed = bytesHashed,
            durationMillis = (System.nanoTime() - startedAt) / 1_000_000L,
        )))
    }.flowOn(Dispatchers.IO)

    private fun isToolivaFile(entry: StorageEntry): Boolean {
        val path = entry.path.replace('\\', '/')
        return path.contains("/Android/data/az.simplesoft.tooliva/") ||
            path.contains("/Android/media/az.simplesoft.tooliva/") ||
            entry.name.startsWith(".tooliva-", ignoreCase = true)
    }

    private data class HashKey(val sizeBytes: Long, val hash: String)
}
