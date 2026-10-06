package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.model.NovaLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class NovaVoiceManager(
    private val context: Context,
    private val onFinalSpeechResult: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _voiceStatusMessage = MutableStateFlow<String?>(null)
    val voiceStatusMessage: StateFlow<String?> = _voiceStatusMessage.asStateFlow()

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _isSpeaking.value = false
                        }
                    })
                }
            }
        } catch (_: Exception) {
            isTtsInitialized = false
        }
    }

    fun startListening(language: NovaLanguage) {
        stopSpeaking()
        _voiceStatusMessage.value = null

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceStatusMessage.value = "On-device SpeechRecognizer service is unavailable on this emulator/device. Use the quick voice command chips or keyboard mic."
            _isListening.value = false
            return
        }

        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _partialTranscript.value = ""
                    _voiceStatusMessage.value = null
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
                    _audioLevel.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                    _audioLevel.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _audioLevel.value = 0f
                    val friendly = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Tap the mic and speak clearly."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out. Tap the mic to speak again."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for voice input."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error during speech recognition."
                        else -> "Voice recognition paused (code $error). You can also tap any voice command preset."
                    }
                    _voiceStatusMessage.value = friendly
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _audioLevel.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val bestText = matches?.firstOrNull()?.trim().orEmpty()
                    _partialTranscript.value = ""
                    if (bestText.isNotEmpty()) {
                        onFinalSpeechResult(bestText)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (partial.isNotEmpty()) {
                        _partialTranscript.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.speechLocaleTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            recognizer.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            _voiceStatusMessage.value = "Could not start microphone listener: ${e.message}"
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
        _isListening.value = false
        _audioLevel.value = 0f
    }

    fun speak(text: String, language: NovaLanguage, speechRate: Float = 1.0f) {
        if (!isTtsInitialized || text.isBlank()) return
        val engine = tts ?: return

        val cleanText = text
            .replace(Regex("[*#_`~•]"), "")
            .replace(Regex("\\[REDACTED[^]]*]"), "redacted field")
            .trim()

        val targetLocale = when (language) {
            NovaLanguage.HINDI -> Locale("hi", "IN")
            NovaLanguage.HINGLISH -> Locale("en", "IN")
            NovaLanguage.ENGLISH -> Locale.US
        }

        val langResult = engine.setLanguage(targetLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.language = Locale.US
        }

        engine.setSpeechRate(speechRate.coerceIn(0.6f, 1.6f))
        val utteranceId = UUID.randomUUID().toString()
        _isSpeaking.value = true
        engine.speak(cleanText.take(3800), TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _isSpeaking.value = false
    }

    fun clearVoiceStatusMessage() {
        _voiceStatusMessage.value = null
    }

    fun shutdown() {
        stopListening()
        stopSpeaking()
        try {
            speechRecognizer?.destroy()
            tts?.shutdown()
        } catch (_: Exception) {
        }
    }
}
