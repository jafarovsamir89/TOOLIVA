package az.simplesoft.tooliva.core.files

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

data class SafSource(val uri: String, val label: String)

private val Context.safSourceDataStore: DataStore<Preferences> by preferencesDataStore(name = "tooliva_saf_sources")

class SafSourceStore(context: Context) {
    private val appContext = context.applicationContext
    private val key = stringPreferencesKey("sources")

    val sources: Flow<List<SafSource>> = appContext.safSourceDataStore.data.map { preferences ->
        runCatching {
            val array = JSONArray(preferences[key].orEmpty())
            buildList {
                for (index in 0 until array.length()) {
                    val value = array.getJSONObject(index)
                    add(SafSource(value.getString("uri"), value.getString("label")))
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun add(source: SafSource) {
        appContext.safSourceDataStore.edit { preferences ->
            val current = decode(preferences[key].orEmpty()).filterNot { it.uri == source.uri }
            preferences[key] = encode(current + source)
        }
    }

    suspend fun remove(uri: String) {
        appContext.safSourceDataStore.edit { preferences -> preferences[key] = encode(decode(preferences[key].orEmpty()).filterNot { it.uri == uri }) }
    }

    private fun decode(raw: String): List<SafSource> = runCatching {
        val array = JSONArray(raw)
        buildList { for (index in 0 until array.length()) { val item = array.getJSONObject(index); add(SafSource(item.getString("uri"), item.getString("label"))) } }
    }.getOrDefault(emptyList())

    private fun encode(items: List<SafSource>): String = JSONArray().apply { items.forEach { put(JSONObject().apply { put("uri", it.uri); put("label", it.label) }) } }.toString()
}
