package az.simplesoft.tooliva.feature.doctor

import android.content.Context

class HardwareTestResultStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("hardware_test_results", Context.MODE_PRIVATE)

    fun read(): Map<HardwareTestId, HardwareTestResultRecord> = HardwareTestId.values().mapNotNull { id ->
        val raw = preferences.getString(id.name, null) ?: return@mapNotNull null
        val parts = raw.split('|')
        val status = runCatching { HardwareTestStatus.valueOf(parts.first()) }.getOrNull() ?: return@mapNotNull null
        id to HardwareTestResultRecord(status, parts.getOrNull(1)?.toLongOrNull() ?: 0L)
    }.toMap()

    fun write(id: HardwareTestId, status: HardwareTestStatus) {
        preferences.edit().putString(id.name, "${status.name}|${System.currentTimeMillis()}").apply()
    }

    fun reset() = preferences.edit().clear().apply()
}
