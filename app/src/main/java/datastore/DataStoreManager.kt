package com.example.gulcinmobile.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {
    companion object {
        val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
    }

    suspend fun saveLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_LANGUAGE] = languageCode
        }
    }

    suspend fun getLanguage(): String {
        return context.dataStore.data
            .map { preferences -> preferences[SELECTED_LANGUAGE] ?: "tr" }
            .first()
    }
}
