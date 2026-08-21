package az.simplesoft.tooliva.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

enum class ToolivaLanguage(val tag: String, val label: String) {
    ENGLISH("en", "English"),
    RUSSIAN("ru", "Русский"),
    AZERBAIJANI("az", "Azərbaycanca"),
    TURKISH("tr", "Türkçe"),
}

data class RecentFile(
    val path: String,
    val name: String,
    val openedAtMillis: Long,
    val sizeBytes: Long,
)

data class FavoriteFolder(
    val path: String,
    val name: String,
    val addedAtMillis: Long,
)

data class ScanHistoryRecord(
    val finishedAtMillis: Long,
    val usedBytes: Long,
    val totalBytes: Long,
    val largestCategory: String?,
    val largestCategoryBytes: Long,
    val filesChecked: Long,
)

private val Context.toolivaUserDataStore: DataStore<Preferences> by preferencesDataStore(name = "tooliva_user_data")

class ToolivaUserDataStore(context: Context) {
    private val appContext = context.applicationContext
    private val languageKey = stringPreferencesKey("language")
    private val recentKey = stringPreferencesKey("recent_files")
    private val favoritesKey = stringPreferencesKey("favorite_folders")
    private val scanHistoryKey = stringPreferencesKey("scan_history")

    val language: Flow<ToolivaLanguage> = appContext.toolivaUserDataStore.data.map { preferences ->
        runCatching { ToolivaLanguage.valueOf(preferences[languageKey].orEmpty()) }
            .getOrDefault(ToolivaLanguage.ENGLISH)
    }

    val recentFiles: Flow<List<RecentFile>> = appContext.toolivaUserDataStore.data.map { preferences ->
        decodeRecent(preferences[recentKey].orEmpty())
    }

    val favoriteFolders: Flow<List<FavoriteFolder>> = appContext.toolivaUserDataStore.data.map { preferences ->
        decodeFavorites(preferences[favoritesKey].orEmpty())
    }

    val scanHistory: Flow<List<ScanHistoryRecord>> = appContext.toolivaUserDataStore.data.map { preferences ->
        decodeHistory(preferences[scanHistoryKey].orEmpty())
    }

    suspend fun setLanguage(language: ToolivaLanguage) {
        appContext.toolivaUserDataStore.edit { it[languageKey] = language.name }
    }

    suspend fun recordOpenedFile(file: RecentFile) {
        appContext.toolivaUserDataStore.edit { preferences ->
            val updated = decodeRecent(preferences[recentKey].orEmpty())
                .filterNot { it.path == file.path }
                .plus(file)
                .sortedByDescending(RecentFile::openedAtMillis)
                .take(MAX_RECENT_FILES)
            preferences[recentKey] = encodeRecent(updated)
        }
    }

    suspend fun toggleFavorite(folder: FavoriteFolder): Boolean {
        var added = false
        appContext.toolivaUserDataStore.edit { preferences ->
            val current = decodeFavorites(preferences[favoritesKey].orEmpty()).toMutableList()
            val existing = current.indexOfFirst { it.path == folder.path }
            if (existing >= 0) {
                current.removeAt(existing)
            } else {
                current += folder
                added = true
            }
            preferences[favoritesKey] = encodeFavorites(current.sortedBy(FavoriteFolder::name))
        }
        return added
    }

    suspend fun recordScan(record: ScanHistoryRecord) {
        appContext.toolivaUserDataStore.edit { preferences ->
            val updated = decodeHistory(preferences[scanHistoryKey].orEmpty())
                .plus(record)
                .sortedByDescending(ScanHistoryRecord::finishedAtMillis)
                .take(MAX_SCAN_HISTORY)
            preferences[scanHistoryKey] = encodeHistory(updated)
        }
    }

    private fun encodeRecent(items: List<RecentFile>): String = JSONArray().apply {
        items.forEach { item ->
            put(JSONObject().apply {
                put("path", item.path)
                put("name", item.name)
                put("opened", item.openedAtMillis)
                put("size", item.sizeBytes)
            })
        }
    }.toString()

    private fun encodeFavorites(items: List<FavoriteFolder>): String = JSONArray().apply {
        items.forEach { item ->
            put(JSONObject().apply {
                put("path", item.path)
                put("name", item.name)
                put("added", item.addedAtMillis)
            })
        }
    }.toString()

    private fun encodeHistory(items: List<ScanHistoryRecord>): String = JSONArray().apply {
        items.forEach { item ->
            put(JSONObject().apply {
                put("finished", item.finishedAtMillis)
                put("used", item.usedBytes)
                put("total", item.totalBytes)
                put("category", item.largestCategory)
                put("categoryBytes", item.largestCategoryBytes)
                put("files", item.filesChecked)
            })
        }
    }.toString()

    private fun decodeRecent(raw: String): List<RecentFile> = decodeArray(raw) { value ->
        RecentFile(value.getString("path"), value.getString("name"), value.getLong("opened"), value.optLong("size"))
    }

    private fun decodeFavorites(raw: String): List<FavoriteFolder> = decodeArray(raw) { value ->
        FavoriteFolder(value.getString("path"), value.getString("name"), value.getLong("added"))
    }

    private fun decodeHistory(raw: String): List<ScanHistoryRecord> = decodeArray(raw) { value ->
        ScanHistoryRecord(
            finishedAtMillis = value.getLong("finished"),
            usedBytes = value.optLong("used"),
            totalBytes = value.optLong("total"),
            largestCategory = value.optString("category").takeIf { it.isNotBlank() && it != "null" },
            largestCategoryBytes = value.optLong("categoryBytes"),
            filesChecked = value.optLong("files"),
        )
    }

    private fun <T> decodeArray(raw: String, transform: (JSONObject) -> T): List<T> = runCatching {
        if (raw.isBlank()) return@runCatching emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                runCatching { add(transform(array.getJSONObject(index))) }
            }
        }
    }.getOrDefault(emptyList())

    private companion object {
        const val MAX_RECENT_FILES = 30
        const val MAX_SCAN_HISTORY = 30
    }
}
