package com.darblee.flingaid.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

class PreferenceStore(private val context: Context)
{
    // Wrap the private variables in a "companion object" so they are not initialized more than once
    companion object {
        private val Context.datastore : DataStore<Preferences> by preferencesDataStore(name = "GAME_SETTING_KEY")
        private val GAME_MUSIC_KEY = booleanPreferencesKey("gameMusicFlag")
    }

    // 'suspend' will pause the co-routine thread to allow other thread to perform task
    suspend fun saveGameMusicFlag(gameMusicFlag: Boolean) {
        context.datastore.edit { pref ->
            pref[GAME_MUSIC_KEY] = gameMusicFlag
        }
    }

    // Get the data as a stand-alone method instead of Flow<boolean> method
    suspend fun getGameMusicOnFlag() : Boolean {
        val preferences = context.datastore.data.first()
        return preferences[GAME_MUSIC_KEY] ?: false
    }
}
