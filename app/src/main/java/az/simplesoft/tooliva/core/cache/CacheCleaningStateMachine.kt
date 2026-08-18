package az.simplesoft.tooliva.core.cache

enum class CacheCleaningStep {
    APP_INFO,
    STORAGE,
    CLEAR_CACHE,
    COMPLETE,
    FAILED,
}

enum class CacheCleaningAction {
    NONE,
    OPEN_STORAGE,
    CLICK_CLEAR_CACHE,
    FAIL,
}

data class CacheCleaningNode(
    val text: String?,
    val contentDescription: String?,
    val isClickable: Boolean,
)

data class CacheCleaningDecision(
    val action: CacheCleaningAction,
    val reason: String,
)

object CacheCleaningStateMachine {
    private val storageLabels = setOf("storage", "storage & cache", "storage usage", "память", "память и кэш", "использование памяти", "хранилище", "хранилище и кэш", "использование хранилища")
    private val clearCacheLabels = setOf("clear cache", "очистить кэш", "очистить кеш")
    private val dangerousLabels = setOf(
        "clear storage",
        "clear data",
        "erase data",
        "delete data",
        "manage storage",
        "очистить хранилище",
        "очистить данные",
        "стереть данные",
        "управление хранилищем",
    )

    fun decide(step: CacheCleaningStep, nodes: List<CacheCleaningNode>, targetAppConfirmed: Boolean): CacheCleaningDecision {
        if (!targetAppConfirmed) return CacheCleaningDecision(CacheCleaningAction.NONE, "target_app_not_confirmed")
        return when (step) {
            CacheCleaningStep.APP_INFO -> findSafe(nodes, storageLabels)?.let {
                CacheCleaningDecision(CacheCleaningAction.OPEN_STORAGE, "storage_control_found")
            } ?: CacheCleaningDecision(CacheCleaningAction.NONE, "storage_control_not_found")
            CacheCleaningStep.STORAGE -> findSafe(nodes, clearCacheLabels)?.let {
                CacheCleaningDecision(CacheCleaningAction.CLICK_CLEAR_CACHE, "clear_cache_control_found")
            } ?: if (nodes.any { normalize(it.text) in dangerousLabels || normalize(it.contentDescription) in dangerousLabels }) {
                CacheCleaningDecision(CacheCleaningAction.FAIL, "dangerous_control_detected")
            } else {
                CacheCleaningDecision(CacheCleaningAction.NONE, "clear_cache_control_not_found")
            }
            CacheCleaningStep.CLEAR_CACHE -> CacheCleaningDecision(CacheCleaningAction.NONE, "waiting_for_result")
            CacheCleaningStep.COMPLETE, CacheCleaningStep.FAILED -> CacheCleaningDecision(CacheCleaningAction.NONE, "session_finished")
        }
    }

    private fun findSafe(nodes: List<CacheCleaningNode>, allowed: Set<String>): CacheCleaningNode? =
        nodes.firstOrNull { node ->
            node.isClickable && normalize(node.text) in allowed && normalize(node.text) !in dangerousLabels &&
                normalize(node.contentDescription) !in dangerousLabels
        }

    private fun normalize(value: String?): String = value.orEmpty().trim().lowercase()
}
