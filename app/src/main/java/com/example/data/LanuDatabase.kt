package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    ServerEntity::class,
    ConnectionHistory::class,
    SettingsEntity::class,
    FavoriteServer::class,
    ExcludedAppEntity::class
  ],
  version = 3,
  exportSchema = false
)
abstract class LanuDatabase : RoomDatabase() {
  abstract fun lanuDao(): LanuDao

  companion object {
    @Volatile private var INSTANCE: LanuDatabase? = nilRef()

    private fun nilRef(): LanuDatabase? = null

    fun getDatabase(context: Context): LanuDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance =
          Room.databaseBuilder(
              context.applicationContext,
              LanuDatabase::class.java,
              "lanu_vpn_database"
            )
            .fallbackToDestructiveMigration()
            .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
