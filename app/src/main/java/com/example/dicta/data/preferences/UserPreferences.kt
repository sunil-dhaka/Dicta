package com.example.dicta.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dicta.domain.model.AsrModelType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val MODEL_DOWNLOADED = booleanPreferencesKey("model_downloaded")
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.ONBOARDING_COMPLETED] ?: false
    }

    val selectedModel: Flow<AsrModelType> = context.dataStore.data.map { preferences ->
        val modelName = preferences[Keys.SELECTED_MODEL] ?: AsrModelType.VOSK_SMALL_EN_US.name
        AsrModelType.valueOf(modelName)
    }

    val modelDownloaded: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.MODEL_DOWNLOADED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setSelectedModel(model: AsrModelType) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SELECTED_MODEL] = model.name
        }
    }

    suspend fun setModelDownloaded(downloaded: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.MODEL_DOWNLOADED] = downloaded
        }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
