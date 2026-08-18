package az.simplesoft.tooliva.core.storage.index

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "storage_index_entries",
    indices = [
        Index(value = ["accessMode", "volumeId", "scanGeneration"]),
        Index(value = ["category", "sizeBytes"]),
        Index(value = ["modifiedTimeMillis"]),
        Index(value = ["parentPath"]),
        Index(value = ["displayName"]),
    ],
)
data class StorageIndexEntity(
    @PrimaryKey val stableKey: String,
    @ColumnInfo(index = true) val accessMode: String,
    @ColumnInfo(index = true) val volumeId: String,
    val canonicalPath: String?,
    val uriRef: String?,
    val parentPath: String?,
    val displayName: String,
    val extension: String?,
    val mimeType: String?,
    val category: String,
    val sizeBytes: Long,
    val modifiedTimeMillis: Long,
    val isDirectory: Boolean,
    @ColumnInfo(index = true) val scanGeneration: String,
)

@Entity(
    tableName = "storage_index_scopes",
    primaryKeys = ["accessMode", "volumeId"],
)
data class StorageIndexScopeEntity(
    val accessMode: String,
    val volumeId: String,
    val activeGeneration: String,
    val lastSuccessfulAtMillis: Long,
)

@Entity(
    tableName = "storage_index_generations",
    indices = [Index(value = ["accessMode", "status", "completedAtMillis"])],
)
data class StorageIndexGenerationEntity(
    @PrimaryKey val generationId: String,
    val accessMode: String,
    val startedAtMillis: Long,
    val completedAtMillis: Long?,
    val status: String,
    val filesDiscovered: Long,
    val foldersVisited: Long,
    val indexedBytes: Long,
    val warningCount: Int,
)
