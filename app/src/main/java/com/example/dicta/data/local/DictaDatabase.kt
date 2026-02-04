package com.example.dicta.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.dicta.data.local.dao.RecordingDao
import com.example.dicta.data.local.entity.RecordingEntity

@Database(
    entities = [RecordingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DictaDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
}
