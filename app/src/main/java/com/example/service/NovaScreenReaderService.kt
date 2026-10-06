package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.NovaPresets
import com.example.model.ScreenSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ScreenCaptureBridge {
    private val _serviceConnected = MutableStateFlow(false)
    val serviceConnected: StateFlow<Boolean> = _serviceConnected.asStateFlow()

    private val _isAnalyzingNow = MutableStateFlow(false)
    val isAnalyzingNow: StateFlow<Boolean> = _isAnalyzingNow.asStateFlow()

    private val _latestSnapshot = MutableStateFlow<ScreenSnapshot?>(NovaPresets.sampleScreenContexts.first())
    val latestSnapshot: StateFlow<ScreenSnapshot?> = _latestSnapshot.asStateFlow()

    internal var activeServiceInstance: NovaScreenReaderService? = null

    internal fun onServiceStateChanged(connected: Boolean, instance: NovaScreenReaderService?) {
        activeServiceInstance = instance
        _serviceConnected.value = connected
    }

    fun setAnalyzingIndicator(active: Boolean) {
        _isAnalyzingNow.value = active
    }

    fun selectSampleScreenSnapshot(snapshot: ScreenSnapshot) {
        _latestSnapshot.value = snapshot
    }

    fun clearActiveSnapshot() {
        _latestSnapshot.value = null
    }

    fun isAccessibilityPermissionGranted(context: Context): Boolean {
        if (_serviceConnected.value) return true
        val expectedComponent = ComponentName(context, NovaScreenReaderService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':')
            .mapNotNull { ComponentName.unflattenFromString(it) }
            .any { it.packageName == expectedComponent.packageName && it.className == expectedComponent.className }
    }

    fun captureOnDemandSnapshot(strictRedaction: Boolean = true): ScreenSnapshot? {
        val service = activeServiceInstance
        if (service != null) {
            _isAnalyzingNow.value = true
            try {
                val liveSnapshot = service.extractCurrentWindowSnapshot(strictRedaction)
                if (liveSnapshot != null) {
                    _latestSnapshot.value = liveSnapshot
                    return liveSnapshot
                }
            } finally {
                _isAnalyzingNow.value = false
            }
        }
        return _latestSnapshot.value
    }
}

class NovaScreenReaderService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        ScreenCaptureBridge.onServiceStateChanged(true, this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // PRIVACY GUARANTEE:
        // NOVA never secretly monitors, logs, or transmits screen events in the background.
        // Screen content is ONLY traversed when the user explicitly triggers a screen read action.
    }

    override fun onInterrupt() {
        ScreenCaptureBridge.setAnalyzingIndicator(false)
    }

    override fun onDestroy() {
        ScreenCaptureBridge.onServiceStateChanged(false, null)
        super.onDestroy()
    }

    fun extractCurrentWindowSnapshot(strictRedaction: Boolean = true): ScreenSnapshot? {
        val rootNode: AccessibilityNodeInfo = rootInActiveWindow ?: return null
        val pkgName = rootNode.packageName?.toString() ?: "unknown.package"

        // Do not read banking, UPI, or password manager apps
        if (SensitiveContentFilter.isSensitivePackage(pkgName)) {
            return ScreenSnapshot(
                packageName = pkgName,
                screenTitle = "Protected Banking / Credential Screen",
                visibleTextSummary = "[PRIVACY SHIELD ACTIVE] NOVA AI blocked reading from $pkgName because it appears to be a sensitive banking, payment, or credential application.",
                uiElements = listOf("[PrivacyShield] Sensitive app protected"),
                redactedSensitiveCount = 1,
                isSampleSimulation = false
            )
        }

        val textSnippets = mutableListOf<String>()
        val uiElementLabels = mutableListOf<String>()
        var redactedCount = 0

        fun traverse(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > 28) return

            if (node.isPassword) {
                redactedCount++
                uiElementLabels.add("[PasswordField] [REDACTED SENSITIVE FIELD]")
                return
            }

            val classNameSimple = node.className?.toString()?.substringAfterLast('.') ?: "View"
            val rawText = node.text?.toString()?.trim().orEmpty()
            val contentDesc = node.contentDescription?.toString()?.trim().orEmpty()

            if (rawText.isNotEmpty()) {
                val (sanitized, redactions) = SensitiveContentFilter.sanitizeText(rawText, strictRedaction)
                redactedCount += redactions
                if (sanitized.isNotEmpty() && !textSnippets.contains(sanitized)) {
                    textSnippets.add(sanitized)
                    uiElementLabels.add("[$classNameSimple] ${sanitized.take(90)}")
                }
            } else if (contentDesc.isNotEmpty()) {
                val (sanitizedDesc, redactions) = SensitiveContentFilter.sanitizeText(contentDesc, strictRedaction)
                redactedCount += redactions
                if (sanitizedDesc.isNotEmpty()) {
                    uiElementLabels.add("[$classNameSimple Icon/Desc] ${sanitizedDesc.take(70)}")
                }
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i), depth + 1)
            }
        }

        traverse(rootNode, 0)

        val summary = if (textSnippets.isEmpty()) {
            "No readable text nodes found on the active window ($pkgName)."
        } else {
            textSnippets.take(35).joinToString("\n")
        }

        val titleGuess = textSnippets.firstOrNull()?.take(48) ?: "Active Screen ($pkgName)"

        return ScreenSnapshot(
            packageName = pkgName,
            screenTitle = titleGuess,
            visibleTextSummary = summary,
            uiElements = uiElementLabels.take(30),
            redactedSensitiveCount = redactedCount,
            isSampleSimulation = false
        )
    }
}
