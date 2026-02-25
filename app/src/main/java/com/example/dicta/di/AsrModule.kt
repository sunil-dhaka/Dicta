package com.example.dicta.di

import android.content.Context
import com.example.dicta.asr.AsrEngine
import com.example.dicta.asr.moonshine.MoonshineEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AsrModule {

    @Provides
    @Singleton
    fun provideAsrEngine(
        @ApplicationContext context: Context
    ): AsrEngine = MoonshineEngine(context)
}
