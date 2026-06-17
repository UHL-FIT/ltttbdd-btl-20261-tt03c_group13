package com.example.eggtimer

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [NhatKy::class], version = 1, exportSchema = false)
abstract class TrungDatabase : RoomDatabase() {
    abstract fun eggDao(): EggDao
    companion object {
        @Volatile
        private var Instance: TrungDatabase? = null
        fun getDatabase(context: Context): TrungDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    TrungDatabase::class.java,
                    "trung_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
