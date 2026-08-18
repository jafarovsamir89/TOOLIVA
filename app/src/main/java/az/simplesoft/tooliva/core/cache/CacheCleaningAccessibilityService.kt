package az.simplesoft.tooliva.core.cache

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent
import az.simplesoft.tooliva.MainActivity

/**
 * Narrow, user-started cache-cleaning service. It only handles the Android Settings
 * screen for the active package and has no behavior outside an active session.
 */
class CacheCleaningAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var store: CacheCleaningSessionStore
    private val timeoutCheck = object : Runnable {
        override fun run() {
            val session = if (::store.isInitialized) store.active() else null
            if (session != null && System.currentTimeMillis() - session.startedAtMillis >= SESSION_TIMEOUT_MILLIS) {
                failSession()
            }
            handler.postDelayed(this, SESSION_CHECK_MILLIS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        store = CacheCleaningSessionStore(this)
        handler.post(timeoutCheck)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!::store.isInitialized) store = CacheCleaningSessionStore(this)
        val session = store.active() ?: return
        val eventPackage = event.packageName?.toString() ?: return
        if (!isExpectedSettingsPackage(eventPackage, session.currentPackage)) {
            if (eventPackage == packageName) failSession()
            return
        }

        val root = rootInActiveWindow ?: return
        val currentPackage = session.currentPackage ?: run {
            completeSession()
            return
        }
        val appLabel = runCatching {
            packageManager.getApplicationInfo(currentPackage, 0).loadLabel(packageManager).toString()
        }.getOrNull() ?: run {
            failSession()
            return
        }
        val nodes = flatten(root)
        // The app label is required before the first Storage navigation. Once that
        // validated click succeeded, the session remains tied to the same Settings
        // task and the next screen is allowed to omit the title.
        val targetConfirmed = session.step != CacheCleaningStep.APP_INFO || nodes.any { node ->
            node.text?.toString()?.trim().equals(appLabel, ignoreCase = true) ||
                node.contentDescription?.toString()?.trim().equals(appLabel, ignoreCase = true)
        }
        val decision = CacheCleaningStateMachine.decide(
            step = session.step,
            nodes = nodes.map { it.toDecisionNode() },
            targetAppConfirmed = targetConfirmed,
        )

        when (decision.action) {
            CacheCleaningAction.OPEN_STORAGE -> {
                val node = findNode(nodes) { it.toDecisionNode().let { n ->
                    CacheCleaningStateMachine.decide(CacheCleaningStep.APP_INFO, listOf(n), true).action == CacheCleaningAction.OPEN_STORAGE
                } }
                if (node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
                    store.setStep(CacheCleaningStep.STORAGE)
                }
            }
            CacheCleaningAction.CLICK_CLEAR_CACHE -> {
                val node = findNode(nodes) { it.toDecisionNode().let { n ->
                    CacheCleaningStateMachine.decide(CacheCleaningStep.STORAGE, listOf(n), true).action == CacheCleaningAction.CLICK_CLEAR_CACHE
                } }
                if (node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
                    store.setStep(CacheCleaningStep.CLEAR_CACHE)
                    handler.postDelayed({ advanceAfterClear() }, ACTION_SETTLE_MILLIS)
                }
            }
            CacheCleaningAction.FAIL -> failSession()
            CacheCleaningAction.NONE -> Unit
        }
    }

    private fun advanceAfterClear() {
        if (!::store.isInitialized || store.active()?.step != CacheCleaningStep.CLEAR_CACHE) return
        val next = store.nextPackageAfterSuccess()
        if (next == null) {
            completeSession()
        } else {
            launchAppDetails(next)
        }
    }

    private fun completeSession() {
        store.complete()
        returnToTooliva()
    }

    private fun failSession() {
        if (!::store.isInitialized || store.active() == null) return
        store.markFailedAndComplete()
        returnToTooliva()
    }

    private fun launchAppDetails(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }.onFailure { failSession() }
    }

    private fun returnToTooliva() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        runCatching { startActivity(intent) }
    }

    private fun isExpectedSettingsPackage(eventPackage: String, targetPackage: String?): Boolean {
        val resolved = targetPackage?.let {
            packageManager.resolveActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$it")),
                0,
            )?.activityInfo?.packageName
        }
        return eventPackage == resolved || eventPackage in KNOWN_SETTINGS_PACKAGES
    }

    private fun flatten(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            result += node
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        visit(root)
        return result
    }

    private fun findNode(nodes: List<AccessibilityNodeInfo>, predicate: (AccessibilityNodeInfo) -> Boolean) = nodes.firstOrNull(predicate)

    private fun AccessibilityNodeInfo.toDecisionNode() = CacheCleaningNode(
        text = text?.toString(),
        contentDescription = contentDescription?.toString(),
        isClickable = isClickable && isEnabled,
    )

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val ACTION_SETTLE_MILLIS = 900L
        private const val SESSION_CHECK_MILLIS = 2_000L
        private const val SESSION_TIMEOUT_MILLIS = 60_000L
        private val KNOWN_SETTINGS_PACKAGES = setOf(
            "com.android.settings",
            "com.android.settings.intelligence",
            "com.miui.securitycenter",
            "com.miui.packageinstaller",
        )
    }
}
