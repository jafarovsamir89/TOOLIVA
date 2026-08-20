package az.simplesoft.tooliva.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppearanceMode { SYSTEM, DARK, LIGHT }

private val Context.toolivaPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "tooliva_preferences")

class ToolivaPreferences(private val context: Context) {
    private val appearanceKey = stringPreferencesKey("appearance_mode")

    val appearance: Flow<AppearanceMode> = context.toolivaPreferencesDataStore.data.map { preferences ->
        runCatching { AppearanceMode.valueOf(preferences[appearanceKey].orEmpty()) }.getOrDefault(AppearanceMode.SYSTEM)
    }

    suspend fun setAppearance(mode: AppearanceMode) {
        context.toolivaPreferencesDataStore.edit { it[appearanceKey] = mode.name }
    }
}
