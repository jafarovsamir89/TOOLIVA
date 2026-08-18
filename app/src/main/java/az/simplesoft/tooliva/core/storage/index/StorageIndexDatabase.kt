package az.simplesoft.tooliva.core.storage.index

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StorageIndexEntity::class,
        StorageIndexScopeEntity::class,
        StorageIndexGenerationEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class StorageIndexDatabase : RoomDatabase() {
    abstract fun storageIndexDao(): StorageIndexDao

    companion object {
        @Volatile
        private var instance: StorageIndexDatabase? = null

        fun getInstance(context: Context): StorageIndexDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StorageIndexDatabase::class.java,
                    "tooliva_storage_index.db",
                ).build().also { instance = it }
            }
    }
}
