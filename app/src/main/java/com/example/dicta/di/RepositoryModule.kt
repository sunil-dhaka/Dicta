package com.example.dicta.di

import com.example.dicta.data.repository.ModelRepositoryImpl
import com.example.dicta.data.repository.RecordingRepositoryImpl
import com.example.dicta.domain.repository.ModelRepository
import com.example.dicta.domain.repository.RecordingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRecordingRepository(
        impl: RecordingRepositoryImpl
    ): RecordingRepository

    @Binds
    @Singleton
    abstract fun bindModelRepository(
        impl: ModelRepositoryImpl
    ): ModelRepository
}
