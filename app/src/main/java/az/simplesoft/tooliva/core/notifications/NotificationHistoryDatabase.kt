package az.simplesoft.tooliva.core.notifications

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [NotificationHistoryEntity::class], version = 1, exportSchema = false)
abstract class NotificationHistoryDatabase : RoomDatabase() {
    abstract fun notificationHistoryDao(): NotificationHistoryDao

    companion object {
        @Volatile private var instance: NotificationHistoryDatabase? = null

        fun get(context: Context): NotificationHistoryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NotificationHistoryDatabase::class.java,
                    "notification_history.db",
                ).build().also { instance = it }
            }
    }
}
