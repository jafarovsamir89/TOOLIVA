package az.simplesoft.tooliva.core.storage.index

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StorageIndexDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneration(generation: StorageIndexGenerationEntity)

    @Query("SELECT * FROM storage_index_generations WHERE generationId = :generationId LIMIT 1")
    suspend fun generation(generationId: String): StorageIndexGenerationEntity?

    @Query(
        """
        UPDATE storage_index_generations
        SET completedAtMillis = :completedAtMillis,
            status = :status,
            filesDiscovered = :filesDiscovered,
            foldersVisited = :foldersVisited,
            indexedBytes = :indexedBytes,
            warningCount = :warningCount
        WHERE generationId = :generationId
        """,
    )
    suspend fun finishGeneration(
        generationId: String,
        completedAtMillis: Long,
        status: String,
        filesDiscovered: Long,
        foldersVisited: Long,
        indexedBytes: Long,
        warningCount: Int,
    )

    @Query("SELECT * FROM storage_index_generations WHERE accessMode = :accessMode AND status = 'COMPLETED' ORDER BY completedAtMillis DESC LIMIT 1")
    suspend fun lastSuccessfulGeneration(accessMode: String): StorageIndexGenerationEntity?

    @Query("SELECT * FROM storage_index_generations WHERE accessMode = :accessMode ORDER BY startedAtMillis DESC LIMIT 1")
    suspend fun latestGeneration(accessMode: String): StorageIndexGenerationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<StorageIndexEntity>)

    @Query("SELECT * FROM storage_index_entries WHERE stableKey IN (:stableKeys)")
    suspend fun findEntries(stableKeys: List<String>): List<StorageIndexEntity>

    @Query("UPDATE storage_index_entries SET scanGeneration = :generationId WHERE stableKey = :stableKey")
    suspend fun markSeen(stableKey: String, generationId: String)

    @Query(
        "DELETE FROM storage_index_entries WHERE accessMode = :accessMode AND volumeId = :volumeId AND scanGeneration != :generationId",
    )
    suspend fun deleteStaleEntries(accessMode: String, volumeId: String, generationId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScope(scope: StorageIndexScopeEntity)

    @Query("DELETE FROM storage_index_entries WHERE accessMode = :accessMode AND uriRef IN (:refs)")
    suspend fun deleteEntriesByRefs(accessMode: String, refs: List<String>)

    @Query(
        """
        SELECT e.*
        FROM storage_index_entries e
        INNER JOIN storage_index_scopes s
          ON s.accessMode = e.accessMode
         AND s.volumeId = e.volumeId
         AND s.activeGeneration = e.scanGeneration
        WHERE e.accessMode = :accessMode
          AND e.isDirectory = 0
          AND e.sizeBytes >= :minimumSizeBytes
          AND (:category IS NULL OR e.category = :category)
          AND (
                :searchQuery = ''
                OR e.displayName LIKE '%' || :searchQuery || '%' COLLATE NOCASE
                OR COALESCE(e.canonicalPath, e.uriRef, '') LIKE '%' || :searchQuery || '%' COLLATE NOCASE
          )
          AND (:modifiedAfterMillis IS NULL OR e.modifiedTimeMillis >= :modifiedAfterMillis)
          AND (:modifiedBeforeMillis IS NULL OR e.modifiedTimeMillis <= :modifiedBeforeMillis)
          AND (:parentPath IS NULL OR e.parentPath LIKE :parentPath || '%')
        ORDER BY
          CASE WHEN :sortOrder = 'SIZE' THEN e.sizeBytes END DESC,
          CASE WHEN :sortOrder = 'NEWEST' THEN e.modifiedTimeMillis END DESC,
          CASE WHEN :sortOrder = 'OLDEST' THEN e.modifiedTimeMillis END ASC,
          CASE WHEN :sortOrder = 'NAME' THEN e.displayName END COLLATE NOCASE ASC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun queryFiles(
        accessMode: String,
        minimumSizeBytes: Long,
        category: String?,
        searchQuery: String,
        modifiedAfterMillis: Long?,
        modifiedBeforeMillis: Long?,
        parentPath: String?,
        sortOrder: String,
        limit: Int,
        offset: Int,
    ): List<StorageIndexEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM storage_index_entries e
        INNER JOIN storage_index_scopes s
          ON s.accessMode = e.accessMode
         AND s.volumeId = e.volumeId
         AND s.activeGeneration = e.scanGeneration
        WHERE e.accessMode = :accessMode
          AND e.isDirectory = 0
          AND e.sizeBytes >= :minimumSizeBytes
          AND (:category IS NULL OR e.category = :category)
          AND (:searchQuery = '' OR e.displayName LIKE '%' || :searchQuery || '%' COLLATE NOCASE)
        """,
    )
    suspend fun countFiles(
        accessMode: String,
        minimumSizeBytes: Long,
        category: String?,
        searchQuery: String,
    ): Int

    @Query(
        """
        SELECT e.category AS category, COUNT(*) AS fileCount, SUM(e.sizeBytes) AS totalBytes
        FROM storage_index_entries e
        INNER JOIN storage_index_scopes s
          ON s.accessMode = e.accessMode
         AND s.volumeId = e.volumeId
         AND s.activeGeneration = e.scanGeneration
        WHERE e.accessMode = :accessMode
          AND e.isDirectory = 0
          AND e.sizeBytes >= :minimumSizeBytes
        GROUP BY e.category
        ORDER BY totalBytes DESC
        """,
    )
    suspend fun categorySummaries(
        accessMode: String,
        minimumSizeBytes: Long,
    ): List<StorageCategorySummaryRow>
}
