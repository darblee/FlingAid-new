package com.darblee.flingaid

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.darblee.flingaid.ui.theme.ColorThemeOption
import kotlinx.coroutines.flow.first

class PreferenceStore(private val context: Context)
{
    // Wrap the private variables in a "companion object" so they are not initialized more than once
    companion object {
        private val Context.datastore : DataStore<Preferences> by preferencesDataStore(name = "GAME_SETTING_KEY")
        private val GAME_MUSIC_KEY = booleanPreferencesKey("gameMusicFlag")
        private val COLOR_MODE_KEY = stringPreferencesKey("ColorMode")
        private val PLAYER_NAME_KEY = stringPreferencesKey("PlayerName")
    }

    // 'suspend' will pause the co-routine thread to allow other thread to perform task
    suspend fun saveGameMusicFlagToSetting(gameMusicFlag: Boolean) {
        context.datastore.edit { pref ->
            pref[GAME_MUSIC_KEY] = gameMusicFlag
        }
    }

    // Get the data as a stand-alone method instead of Flow<boolean> method
    suspend fun readGameMusicOnFlagFromSetting() : Boolean {
        val preferences = context.datastore.data.first()
        return preferences[GAME_MUSIC_KEY] ?: false
    }

    // 'suspend' will pause the co-routine thread to allow other thread to perform task
    suspend fun saveColorModeToSetting(colorMode: ColorThemeOption) {
        context.datastore.edit { pref ->
            pref[COLOR_MODE_KEY] = colorMode.toString()
        }
    }

    // Get the data as a stand-alone method instead of Flow<boolean> method
    suspend fun readColorModeFromSetting(): ColorThemeOption {
        val preferences = context.datastore.data.first()

        val colorModeString = preferences[COLOR_MODE_KEY]

        if (colorModeString == ColorThemeOption.Light.toString())
            return (ColorThemeOption.Light)

        if (colorModeString == ColorThemeOption.Dark.toString())
            return (ColorThemeOption.Dark)

        return ColorThemeOption.System
    }

    // 'suspend' will pause the co-routine thread to allow other thread to perform task
    suspend fun savePlayerNameToSetting(playerName: String) {
        context.datastore.edit { pref ->
            pref[PLAYER_NAME_KEY] = playerName
        }
    }

    // Get the data as a stand-alone method instead of Flow<boolean> method
    suspend fun readPlayerNameFomSetting() : String {
        val preferences = context.datastore.data.first()
        return preferences[PLAYER_NAME_KEY] ?: ""
    }
}
