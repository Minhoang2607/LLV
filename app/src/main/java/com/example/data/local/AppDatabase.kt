package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AgencyConfig
import com.example.data.model.Leader
import com.example.data.model.ScheduleChangeLog
import com.example.data.model.ScheduleEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ScheduleEvent::class, Leader::class, AgencyConfig::class, ScheduleChangeLog::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lich_ban_giam_doc.db"
                )
                .fallbackToDestructiveMigration(true)
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.scheduleDao())
                    }
                }
            }

            suspend fun populateDatabase(dao: ScheduleDao) {
                dao.insertConfig(InitialData.DEFAULT_CONFIG)
                dao.insertLeaders(InitialData.DEFAULT_LEADERS)
                dao.insertEvents(InitialData.DEFAULT_EVENTS + InitialData.NEW_WEEK_EVENTS)
                dao.insertChangeLogs(InitialData.DEFAULT_CHANGE_LOGS)
            }
        }
    }
}
