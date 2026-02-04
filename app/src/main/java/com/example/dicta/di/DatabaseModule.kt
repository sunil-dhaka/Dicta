package com.example.dicta.di

import android.content.Context
import androidx.room.Room
import com.example.dicta.data.local.DictaDatabase
import com.example.dicta.data.local.dao.RecordingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): DictaDatabase = Room.databaseBuilder(
        context,
        DictaDatabase::class.java,
        "dicta_database"
    ).build()

    @Provides
    @Singleton
    fun provideRecordingDao(database: DictaDatabase): RecordingDao = database.recordingDao()
}
