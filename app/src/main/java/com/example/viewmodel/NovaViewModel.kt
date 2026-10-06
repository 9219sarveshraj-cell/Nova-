package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.NovaDatabase
import com.example.data.local.NovaPreferencesRepository
import com.example.data.local.NovaUserSettings
import com.example.data.remote.NovaAiRepository
import com.example.model.AiModelOption
import com.example.model.AssistantState
import com.example.model.MessageSourceBadge
import com.example.model.NovaLanguage
import com.example.model.ResponseDepth
import com.example.model.ScreenSnapshot
import com.example.model.ThemePreference
import com.example.service.NovaCommandRouter
import com.example.service.NovaFloatingOverlayService
import com.example.service.NovaVoiceManager
import com.example.service.ScreenCaptureBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class NovaViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Context = application.applicationContext
    private val database = NovaDatabase.getInstance(appContext)
    private val chatDao = database.chatDao()
    private val preferencesRepo = NovaPreferencesRepository(appContext)
    private val aiRepository = NovaAiRepository()

    val settingsState: StateFlow<NovaUserSettings> = preferencesRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NovaUserSettings()
    )

    val allSessions: StateFlow<List<ChatSessionEntity>> = chatDao.getAllSessions().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    val currentMessages: StateFlow<List<ChatMessageEntity>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId == null) flowOf(emptyList()) else chatDao.getMessagesForSession(sessionId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _attachScreenContextToNextPrompt = MutableStateFlow(false)
    val attachScreenContextToNextPrompt: StateFlow<Boolean> = _attachScreenContextToNextPrompt.asStateFlow()

    private val _attachedBitmap = MutableStateFlow<Bitmap?>(null)
    val attachedBitmap: StateFlow<Bitmap?> = _attachedBitmap.asStateFlow()

    private val _attachedImageLabel = MutableStateFlow<String?>(null)
    val attachedImageLabel: StateFlow<String?> = _attachedImageLabel.asStateFlow()

    private val _speakingMessageText = MutableStateFlow<String?>(null)
    val speakingMessageText: StateFlow<String?> = _speakingMessageText.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val voiceManager = NovaVoiceManager(
        context = appContext,
        onFinalSpeechResult = { spokenText ->
            viewModelScope.launch {
                sendMessage(rawPrompt = spokenText, forceIncludeScreen = false)
            }
        }
    )

    val isListening: StateFlow<Boolean> = voiceManager.isListening
    val partialTranscript: StateFlow<String> = voiceManager.partialTranscript
    val audioLevel: StateFlow<Float> = voiceManager.audioLevel
    val isSpeaking: StateFlow<Boolean> = voiceManager.isSpeaking
    val voiceStatusMessage: StateFlow<String?> = voiceManager.voiceStatusMessage

    val isScreenServiceConnected: StateFlow<Boolean> = ScreenCaptureBridge.serviceConnected
    val isAnalyzingScreenNow: StateFlow<Boolean> = ScreenCaptureBridge.isAnalyzingNow
    val activeScreenSnapshot: StateFlow<ScreenSnapshot?> = ScreenCaptureBridge.latestSnapshot
    val isFloatingOverlayRunning: StateFlow<Boolean> = NovaFloatingOverlayService.isOverlayRunning

    val assistantState: StateFlow<AssistantState> = combine(
        isListening,
        isAnalyzingScreenNow,
        isThinking,
        isSpeaking
    ) { listening, analyzing, thinking, speaking ->
        when {
            listening -> AssistantState.LISTENING
            analyzing -> AssistantState.ANALYZING_SCREEN
            thinking -> AssistantState.THINKING
            speaking -> AssistantState.SPEAKING
            else -> AssistantState.IDLE
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AssistantState.IDLE
    )

    init {
        viewModelScope.launch {
            ensureInitialSessionExists()
        }
    }

    private suspend fun ensureInitialSessionExists() {
        val existing = chatDao.getAllSessions().first()
        if (existing.isEmpty()) {
            val newSessionId = chatDao.insertSession(
                ChatSessionEntity(
                    title = "Welcome to NOVA AI",
                    languageCode = "hinglish",
                    previewText = "Namaste! Main NOVA AI Assistant hoon...",
                    messageCount = 1
                )
            )
            _currentSessionId.value = newSessionId
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = newSessionId,
                    isFromUser = false,
                    content = "Namaste! Main **NOVA AI Assistant** hoon — aapka smart voice, screen-aware aur multimodal personal AI assistant.\n\n" +
                        "• **Voice & Chat:** Hindi, Hinglish, ya English mein kuch bhi poochhein.\n" +
                        "• **Screen Understanding:** *\"Screen par kya likha hai?\"*, *\"Is page ko samjhao\"*, *\"Is question ka answer batao\"*, ya *\"Is error ko samjhao\"* bolein ya tap karein.\n" +
                        "• **Image / Screenshot Analysis:** Gallery ya Camera se screenshot/photo attach karke diagram, text, ya error samjhein.\n" +
                        "• **Privacy Shield:** Passwords, OTPs, aur banking PINs hamesha on-device redact rehte hain.",
                    languageBadge = "Hinglish",
                    sourceBadge = MessageSourceBadge.GENERAL_KNOWLEDGE.label
                )
            )
        } else if (_currentSessionId.value == null) {
            _currentSessionId.value = existing.first().id
        }
    }

    fun isApiKeyConfigured(): Boolean = aiRepository.isApiKeyConfigured()

    fun isInternetConnected(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isAccessibilityGranted(): Boolean {
        return ScreenCaptureBridge.isAccessibilityPermissionGranted(appContext)
    }

    fun isOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(appContext)
    }

    fun sendMessage(rawPrompt: String, forceIncludeScreen: Boolean = false) {
        val trimmed = rawPrompt.trim()
        if (trimmed.isEmpty() && _attachedBitmap.value == null) return

        viewModelScope.launch {
            val currentSettings = settingsState.value
            var sessionId = _currentSessionId.value
            if (sessionId == null) {
                sessionId = chatDao.insertSession(
                    ChatSessionEntity(
                        title = trimmed.take(36).ifEmpty { "Image Analysis" },
                        languageCode = currentSettings.language.code
                    )
                )
                _currentSessionId.value = sessionId
            }

            val priorMessages = chatDao.getMessagesForSessionOnce(sessionId)
            val lastMessageText = priorMessages.lastOrNull()?.content

            // Parse custom commands (Hindi / Hinglish / English)
            val parsedCommand = NovaCommandRouter.parseUserCommand(trimmed, lastMessageText)
            val shouldAttachScreen = forceIncludeScreen ||
                parsedCommand.requiresScreenRead ||
                _attachScreenContextToNextPrompt.value

            var screenSnapshotToUse: ScreenSnapshot? = null
            if (shouldAttachScreen) {
                ScreenCaptureBridge.setAnalyzingIndicator(true)
                delay(320) // Visible indicator pulse so user clearly sees screen analysis active
                screenSnapshotToUse = ScreenCaptureBridge.captureOnDemandSnapshot(
                    strictRedaction = currentSettings.strictPrivacyRedactionEnabled
                )
                ScreenCaptureBridge.setAnalyzingIndicator(false)
            }

            val bitmapToUse = _attachedBitmap.value
            val imageLabelToUse = _attachedImageLabel.value
            val effectiveLanguage = parsedCommand.overrideLanguage ?: currentSettings.language
            val effectiveDepth = parsedCommand.overrideDepth ?: currentSettings.responseDepth

            // Save User message to Room DB
            val displayUserText = trimmed.ifEmpty { "Analyze this attached image and explain what is visible." }
            val userMsg = ChatMessageEntity(
                sessionId = sessionId,
                isFromUser = true,
                content = displayUserText,
                languageBadge = effectiveLanguage.shortBadge,
                sourceBadge = if (parsedCommand.isCustomCommandMatch) {
                    MessageSourceBadge.CUSTOM_COMMAND.label
                } else {
                    MessageSourceBadge.GENERAL_KNOWLEDGE.label
                },
                attachedScreenTitle = screenSnapshotToUse?.screenTitle,
                attachedScreenSnippet = screenSnapshotToUse?.visibleTextSummary,
                hasAttachedImage = bitmapToUse != null,
                attachedImageDescription = imageLabelToUse
            )
            chatDao.insertMessage(userMsg)

            // Clear one-shot image attachment after sending
            _attachedBitmap.value = null
            _attachedImageLabel.value = null
            _attachScreenContextToNextPrompt.value = false

            // Update session title if first user message
            val currentSession = chatDao.getSessionById(sessionId)
            if (currentSession != null) {
                val updatedTitle = if (currentSession.title == "Welcome to NOVA AI" || currentSession.messageCount <= 1) {
                    displayUserText.take(40)
                } else {
                    currentSession.title
                }
                chatDao.updateSession(
                    currentSession.copy(
                        title = updatedTitle,
                        previewText = displayUserText.take(80),
                        messageCount = priorMessages.size + 1,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            // Generate AI reply
            _isThinking.value = true
            val aiResult = aiRepository.generateAssistantReply(
                userPrompt = parsedCommand.cleanedPromptForAi.ifEmpty { displayUserText },
                conversationHistory = priorMessages,
                settings = currentSettings,
                attachedScreen = screenSnapshotToUse,
                attachedBitmap = bitmapToUse,
                isInternetAvailable = isInternetConnected(),
                overrideLanguage = effectiveLanguage,
                overrideDepth = effectiveDepth
            )
            _isThinking.value = false

            val assistantMsg = ChatMessageEntity(
                sessionId = sessionId,
                isFromUser = false,
                content = aiResult.text,
                languageBadge = effectiveLanguage.shortBadge,
                sourceBadge = aiResult.sourceBadge.label,
                attachedScreenTitle = screenSnapshotToUse?.screenTitle,
                attachedScreenSnippet = screenSnapshotToUse?.visibleTextSummary,
                hasAttachedImage = bitmapToUse != null,
                attachedImageDescription = imageLabelToUse,
                isError = aiResult.isError,
                retryPrompt = displayUserText
            )
            chatDao.insertMessage(assistantMsg)

            // Update session preview with assistant answer
            chatDao.getSessionById(sessionId)?.let { latestSession ->
                chatDao.updateSession(
                    latestSession.copy(
                        previewText = aiResult.text.replace("*", "").take(90),
                        messageCount = priorMessages.size + 2,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            // Auto-speak via TTS if enabled and not an error
            if (currentSettings.ttsAutoReadEnabled && !aiResult.isError) {
                speakMessageAloud(aiResult.text, effectiveLanguage, currentSettings.speechRate)
            }
        }
    }

    fun retryPrompt(promptText: String) {
        sendMessage(rawPrompt = promptText, forceIncludeScreen = false)
    }

    fun triggerScreenReadCommand(commandPhrase: String) {
        _attachScreenContextToNextPrompt.value = true
        sendMessage(rawPrompt = commandPhrase, forceIncludeScreen = true)
    }

    fun toggleAttachScreenContext() {
        val next = !_attachScreenContextToNextPrompt.value
        _attachScreenContextToNextPrompt.value = next
        if (next) {
            ScreenCaptureBridge.captureOnDemandSnapshot(
                strictRedaction = settingsState.value.strictPrivacyRedactionEnabled
            )
        }
    }

    fun refreshLiveScreenCapture() {
        viewModelScope.launch {
            ScreenCaptureBridge.setAnalyzingIndicator(true)
            delay(280)
            ScreenCaptureBridge.captureOnDemandSnapshot(
                strictRedaction = settingsState.value.strictPrivacyRedactionEnabled
            )
            ScreenCaptureBridge.setAnalyzingIndicator(false)
            _attachScreenContextToNextPrompt.value = true
            _statusBannerMessage.value = "Screen snapshot refreshed with privacy redaction active."
        }
    }

    fun selectSampleScreenSnapshot(snapshot: ScreenSnapshot) {
        ScreenCaptureBridge.selectSampleScreenSnapshot(snapshot)
        _attachScreenContextToNextPrompt.value = true
        _statusBannerMessage.value = "Loaded inspector context: ${snapshot.screenTitle}"
    }

    fun detachScreenContext() {
        _attachScreenContextToNextPrompt.value = false
    }

    fun attachImageFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(appContext.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.isMutableRequired = true
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(appContext.contentResolver, uri)
                    }
                }
                _attachedBitmap.value = bitmap
                _attachedImageLabel.value = "Selected Image (${bitmap.width}×${bitmap.height}px)"
            } catch (e: Exception) {
                _statusBannerMessage.value = "Could not load selected image: ${e.message}"
            }
        }
    }

    fun attachCameraBitmap(bitmap: Bitmap?) {
        if (bitmap == null) return
        _attachedBitmap.value = bitmap
        _attachedImageLabel.value = "Captured Photo (${bitmap.width}×${bitmap.height}px)"
    }

    fun clearAttachedImage() {
        _attachedBitmap.value = null
        _attachedImageLabel.value = null
    }

    fun toggleVoiceListening() {
        if (!settingsState.value.voiceInputEnabled) {
            _statusBannerMessage.value = "Voice input is disabled in Settings. Enable it to use the microphone."
            return
        }
        if (isListening.value) {
            voiceManager.stopListening()
        } else {
            voiceManager.startListening(settingsState.value.language)
        }
    }

    fun toggleSpeakMessage(text: String) {
        if (isSpeaking.value && _speakingMessageText.value == text) {
            stopSpeaking()
        } else {
            speakMessageAloud(
                text = text,
                language = settingsState.value.language,
                speechRate = settingsState.value.speechRate
            )
        }
    }

    private fun speakMessageAloud(text: String, language: NovaLanguage, speechRate: Float) {
        _speakingMessageText.value = text
        voiceManager.speak(text, language, speechRate)
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
        _speakingMessageText.value = null
    }

    fun clearStatusBannerMessage() {
        _statusBannerMessage.value = null
        voiceManager.clearVoiceStatusMessage()
    }

    fun createNewChatSession() {
        viewModelScope.launch {
            stopSpeaking()
            val lang = settingsState.value.language
            val newId = chatDao.insertSession(
                ChatSessionEntity(
                    title = "New Conversation",
                    languageCode = lang.code,
                    previewText = "Ask NOVA anything in Hindi, Hinglish, or English",
                    messageCount = 0
                )
            )
            _currentSessionId.value = newId
        }
    }

    fun selectSession(sessionId: Long) {
        stopSpeaking()
        _currentSessionId.value = sessionId
    }

    fun clearCurrentSessionMessages() {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            stopSpeaking()
            chatDao.clearMessagesForSession(sessionId)
            chatDao.getSessionById(sessionId)?.let { session ->
                chatDao.updateSession(
                    session.copy(
                        previewText = "Conversation cleared",
                        messageCount = 0,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            _statusBannerMessage.value = "Current chat history cleared."
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            chatDao.clearMessagesForSession(sessionId)
            chatDao.deleteSessionById(sessionId)
            if (_currentSessionId.value == sessionId) {
                val remaining = chatDao.getAllSessions().first()
                if (remaining.isNotEmpty()) {
                    _currentSessionId.value = remaining.first().id
                } else {
                    ensureInitialSessionExists()
                }
            }
        }
    }

    fun deleteAllConversationHistory() {
        viewModelScope.launch {
            stopSpeaking()
            chatDao.deleteAllMessages()
            chatDao.deleteAllSessions()
            _currentSessionId.value = null
            ensureInitialSessionExists()
            _statusBannerMessage.value = "All conversation history permanently deleted."
        }
    }

    // Settings updates
    fun setLanguage(language: NovaLanguage) {
        viewModelScope.launch { preferencesRepo.setLanguage(language) }
    }

    fun setResponseDepth(depth: ResponseDepth) {
        viewModelScope.launch { preferencesRepo.setResponseDepth(depth) }
    }

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch { preferencesRepo.setThemePreference(theme) }
    }

    fun setSelectedModel(model: AiModelOption) {
        viewModelScope.launch { preferencesRepo.setSelectedModel(model) }
    }

    fun setVoiceInputEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setVoiceInputEnabled(enabled)
            if (!enabled) voiceManager.stopListening()
        }
    }

    fun setTtsAutoReadEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setTtsAutoReadEnabled(enabled)
            if (!enabled) stopSpeaking()
        }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch { preferencesRepo.setSpeechRate(rate) }
    }

    fun setWebSearchGroundingEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.setWebSearchGroundingEnabled(enabled) }
    }

    fun setStrictPrivacyRedactionEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.setStrictPrivacyRedactionEnabled(enabled) }
    }

    fun setShowScreenReadingIndicator(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.setShowScreenReadingIndicator(enabled) }
    }

    fun toggleFloatingAssistantOverlay(enable: Boolean) {
        viewModelScope.launch {
            if (enable && !Settings.canDrawOverlays(appContext)) {
                _statusBannerMessage.value = "Please grant 'Display over other apps' permission first to activate the Floating Assistant."
                preferencesRepo.setFloatingAssistantEnabled(false)
                return@launch
            }
            preferencesRepo.setFloatingAssistantEnabled(enable)
            val serviceIntent = Intent(appContext, NovaFloatingOverlayService::class.java)
            if (enable) {
                appContext.startService(serviceIntent)
            } else {
                appContext.stopService(serviceIntent)
            }
        }
    }

    override fun onCleared() {
        voiceManager.shutdown()
        super.onCleared()
    }
}
