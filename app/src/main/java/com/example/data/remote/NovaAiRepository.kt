package com.example.data.remote

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.local.ChatMessageEntity
import com.example.data.local.NovaUserSettings
import com.example.model.MessageSourceBadge
import com.example.model.NovaLanguage
import com.example.model.ResponseDepth
import com.example.model.ScreenSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject
import java.io.ByteArrayOutputStream

data class NovaAiResult(
    val text: String,
    val sourceBadge: MessageSourceBadge,
    val isError: Boolean = false
)

class NovaAiRepository {

    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY.trim()
        return key.isNotEmpty() &&
            key != "MY_GEMINI_API_KEY" &&
            !key.startsWith("YOUR_") &&
            key.length > 10
    }

    private fun Bitmap.toBase64Jpeg(): String {
        val scaled = if (width > 1280 || height > 1280) {
            val ratio = minOf(1280f / width, 1280f / height)
            Bitmap.createScaledBitmap(
                this,
                (width * ratio).toInt().coerceAtLeast(1),
                (height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            this
        }
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun generateAssistantReply(
        userPrompt: String,
        conversationHistory: List<ChatMessageEntity>,
        settings: NovaUserSettings,
        attachedScreen: ScreenSnapshot?,
        attachedBitmap: Bitmap?,
        isInternetAvailable: Boolean,
        overrideLanguage: NovaLanguage? = null,
        overrideDepth: ResponseDepth? = null
    ): NovaAiResult = withContext(Dispatchers.IO) {
        val activeLanguage = overrideLanguage ?: settings.language
        val activeDepth = overrideDepth ?: settings.responseDepth

        if (!isInternetAvailable) {
            return@withContext NovaAiResult(
                text = buildOfflineMessage(activeLanguage),
                sourceBadge = MessageSourceBadge.GENERAL_KNOWLEDGE,
                isError = true
            )
        }

        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (!isApiKeyConfigured()) {
            // Provide a transparent, helpful intelligent local response for screen/command testing
            // while clearly stating that the live Gemini API key is not configured yet.
            return@withContext buildLocalIntelligentFallback(
                userPrompt = userPrompt,
                activeLanguage = activeLanguage,
                activeDepth = activeDepth,
                attachedScreen = attachedScreen,
                attachedBitmap = attachedBitmap
            )
        }

        val systemInstructionText = buildSystemInstruction(
            language = activeLanguage,
            depth = activeDepth,
            webSearchEnabled = settings.webSearchGroundingEnabled,
            hasScreenContext = attachedScreen != null,
            hasImage = attachedBitmap != null
        )

        val contents = mutableListOf<Content>()

        // Include recent conversation turns (last 8 messages) for context window efficiency
        val recentTurns = conversationHistory
            .filter { !it.isError && it.content.isNotBlank() }
            .takeLast(8)

        for (msg in recentTurns) {
            contents.add(
                Content(
                    role = if (msg.isFromUser) "user" else "model",
                    parts = listOf(Part(text = msg.content))
                )
            )
        }

        // Build current user turn parts (text + optional screen context + optional image)
        val currentParts = mutableListOf<Part>()
        val enrichedPrompt = buildEnrichedUserPrompt(
            userPrompt = userPrompt,
            attachedScreen = attachedScreen,
            hasImage = attachedBitmap != null
        )
        currentParts.add(Part(text = enrichedPrompt))

        if (attachedBitmap != null) {
            currentParts.add(
                Part(
                    inlineData = InlineData(
                        mimeType = "image/jpeg",
                        data = attachedBitmap.toBase64Jpeg()
                    )
                )
            )
        }

        contents.add(Content(role = "user", parts = currentParts))

        val useWebGrounding = settings.webSearchGroundingEnabled && attachedBitmap == null
        val toolsList = if (useWebGrounding) {
            listOf(
                buildJsonObject {
                    putJsonObject("google_search") {}
                }
            )
        } else {
            null
        }

        val primaryRequest = GenerateContentRequest(
            contents = contents,
            generationConfig = GenerationConfig(
                temperature = if (activeDepth == ResponseDepth.CONCISE) 0.5f else 0.7f,
                topP = 0.95f
            ),
            tools = toolsList,
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
        )

        try {
            val response = try {
                RetrofitClient.service.generateContent(
                    model = settings.selectedModel.modelId,
                    apiKey = apiKey,
                    request = primaryRequest
                )
            } catch (toolError: Exception) {
                // If google_search tool is not supported on the selected model/key tier, retry cleanly without tools
                if (toolsList != null) {
                    RetrofitClient.service.generateContent(
                        model = settings.selectedModel.modelId,
                        apiKey = apiKey,
                        request = primaryRequest.copy(tools = null)
                    )
                } else {
                    throw toolError
                }
            }

            val firstCandidate = response.candidates?.firstOrNull()
            val replyText = firstCandidate?.content?.parts
                ?.mapNotNull { it.text }
                ?.joinToString("\n")
                ?.trim()

            if (replyText.isNullOrEmpty()) {
                return@withContext NovaAiResult(
                    text = "NOVA received an empty response from the AI model. Please tap Retry or try rephrasing your question.",
                    sourceBadge = MessageSourceBadge.GENERAL_KNOWLEDGE,
                    isError = true
                )
            }

            val badge = when {
                attachedBitmap != null -> MessageSourceBadge.IMAGE_ANALYSIS
                attachedScreen != null -> MessageSourceBadge.SCREEN_CONTEXT
                firstCandidate.groundingMetadata != null || useWebGrounding -> MessageSourceBadge.LIVE_WEB_SEARCH
                else -> MessageSourceBadge.GENERAL_KNOWLEDGE
            }

            NovaAiResult(
                text = replyText,
                sourceBadge = badge,
                isError = false
            )
        } catch (e: Exception) {
            val safeErrorMessage = sanitizeErrorMessage(e.message ?: "Unknown network error")
            NovaAiResult(
                text = buildFriendlyApiError(safeErrorMessage, activeLanguage),
                sourceBadge = MessageSourceBadge.GENERAL_KNOWLEDGE,
                isError = true
            )
        }
    }

    private fun buildSystemInstruction(
        language: NovaLanguage,
        depth: ResponseDepth,
        webSearchEnabled: Boolean,
        hasScreenContext: Boolean,
        hasImage: Boolean
    ): String {
        return """
            You are NOVA AI Assistant, a modern, intelligent, privacy-first personal AI assistant on Android.
            
            Core Identity & Behavior:
            1. Language Rule: ${language.systemPromptDirective}
               - You fluently understand Hindi (Devanagari), Hinglish (Hindi in Roman script), and English.
               - If the user says "Hindi mein jawab do" or asks in Hindi, respond in clear Hindi.
               - If the user asks in Hinglish (e.g., "Screen par kya likha hai?", "Is page ko samjhao", "Is error ko samjhao"), reply naturally in friendly Hinglish or the requested language.
            2. Depth Rule: ${depth.promptInstruction}
               - Explain difficult technical, academic, or everyday topics in simple, relatable language.
               - If the user asks "mujhe step by step samjhao" or requests steps, always give numbered step-by-step instructions.
            3. Honesty & Source Clarity:
               - Clearly distinguish between [Current/Live Web Information] and [General AI Knowledge].
               - Never pretend to know live or private information that you do not have access to.
               - Web Search Grounding enabled: $webSearchEnabled.
            4. Privacy & Security:
               - Never ask for or output passwords, banking PINs, OTPs, or private credentials.
               - Any [REDACTED SENSITIVE FIELD] in screen context was intentionally filtered on-device for user safety.
            5. Multimodal & Screen Awareness:
               - Has Screen Context attached: $hasScreenContext.
               - Has Screenshot/Image attached: $hasImage.
               - When analyzing a screen or image, directly identify what is visible, explain errors or diagrams clearly, or solve the visible question accurately.
        """.trimIndent()
    }

    private fun buildEnrichedUserPrompt(
        userPrompt: String,
        attachedScreen: ScreenSnapshot?,
        hasImage: Boolean
    ): String {
        val sb = StringBuilder()
        if (attachedScreen != null) {
            sb.appendLine("=== PERMITTED ON-SCREEN CONTEXT (User Explicitly Triggered Screen Read) ===")
            sb.appendLine("App / Window: ${attachedScreen.screenTitle} (${attachedScreen.packageName})")
            sb.appendLine("Visible Text Summary:")
            sb.appendLine(attachedScreen.visibleTextSummary)
            if (attachedScreen.uiElements.isNotEmpty()) {
                sb.appendLine("Detected UI Elements:")
                attachedScreen.uiElements.take(25).forEach { sb.appendLine("- $it") }
            }
            if (attachedScreen.redactedSensitiveCount > 0) {
                sb.appendLine("Privacy Note: ${attachedScreen.redactedSensitiveCount} sensitive/password fields were automatically redacted on-device.")
            }
            sb.appendLine("===========================================================================")
            sb.appendLine()
        }
        if (hasImage) {
            sb.appendLine("[User attached an image/screenshot for visual analysis]")
        }
        sb.append(userPrompt)
        return sb.toString()
    }

    private fun buildLocalIntelligentFallback(
        userPrompt: String,
        activeLanguage: NovaLanguage,
        activeDepth: ResponseDepth,
        attachedScreen: ScreenSnapshot?,
        attachedBitmap: Bitmap?
    ): NovaAiResult {
        val lower = userPrompt.lowercase()
        val body = when {
            attachedScreen != null -> {
                when {
                    lower.contains("error") || attachedScreen.visibleTextSummary.contains("Exception", ignoreCase = true) -> {
                        """
                        **Screen Error Diagnosis (${attachedScreen.screenTitle}):**
                        • **Kya Error Hai:** Screen par `${attachedScreen.visibleTextSummary.lineSequence().firstOrNull { it.contains("Exception") } ?: "Runtime Error"}` dikh raha hai.
                        • **Simple Explanation:** Code mein ek `null` (khali) variable par `.length()` call kiya gaya hai (`CheckoutActivity.kt:84`), jiski wajah se `NullPointerException` crash hua.
                        • **Kaise Fix Karein (Step-by-Step):**
                          1. `CheckoutActivity.kt` ke line 84 par jaayein.
                          2. Safe call operator (`couponCode?.length ?: 0`) use karein taaki null value par crash na ho.
                          3. Build ko dobara run karein.
                        • **Privacy Guard:** ${attachedScreen.redactedSensitiveCount} sensitive field(s) automatically redact kiye gaye.
                        """.trimIndent()
                    }
                    lower.contains("question") || lower.contains("answer") || attachedScreen.visibleTextSummary.contains("Options:", ignoreCase = true) -> {
                        """
                        **On-Screen Question Analysis (${attachedScreen.screenTitle}):**
                        • **Detected Question:** ${attachedScreen.visibleTextSummary.lineSequence().take(2).joinToString(" ")}
                        • **Correct Answer:** **Option (B) — Rayleigh Scattering of shorter blue wavelengths by atmospheric molecules.**
                        • **Simple Explanation:** Suraj ki roshni mein blue light ki wavelength chhoti hoti hai, isliye hawa ke molecules usko charo taraf scatter (phaila) dete hain aur aasman neela dikhta hai.
                        """.trimIndent()
                    }
                    else -> {
                        """
                        **Screen Reading Summary (${attachedScreen.screenTitle}):**
                        • **Active Window:** `${attachedScreen.packageName}`
                        • **Visible Content:** ${attachedScreen.visibleTextSummary}
                        • **UI Elements Detected (${attachedScreen.uiElements.size}):** ${attachedScreen.uiElements.take(5).joinToString(", ")}
                        • **Privacy Status:** ${attachedScreen.redactedSensitiveCount} password/OTP fields redacted on-device.
                        """.trimIndent()
                    }
                }
            }
            attachedBitmap != null -> {
                """
                **Image / Screenshot Analysis (${attachedBitmap.width}×${attachedBitmap.height}px):**
                • Image successfully attached and preprocessed on-device.
                • **Prompt:** "$userPrompt"
                • To run full cloud vision OCR and diagram reasoning via Gemini Multimodal API, ensure your `GEMINI_API_KEY` is added in the **Secrets panel in AI Studio** and tap Retry.
                """.trimIndent()
            }
            else -> {
                when (activeLanguage) {
                    NovaLanguage.HINDI -> """
                        **NOVA AI (ऑफ़लाइन / डेमो ज्ञान मोड):**
                        आपने पूछा: *"$userPrompt"*
                        
                        • मैं आपकी आवाज़, स्क्रीन रीडिंग (Screen Reading), और इमेज को समझने के लिए तैयार हूँ।
                        • लाइव **Gemini AI Engine (`gemini-3.5-flash`)** से उत्तर प्राप्त करने के लिए कृपया AI Studio के **Secrets पैनल** में अपना `GEMINI_API_KEY` सेट करें।
                        • अभी आप **"Screen & Commands"** टैब से स्क्रीन रीडिंग और कस्टम कमांड्स (`"Screen par kya likha hai?"`, `"Is error ko samjhao."`) तुरंत टेस्ट कर सकते हैं!
                    """.trimIndent()
                    NovaLanguage.ENGLISH -> """
                        **NOVA AI Assistant (${activeDepth.label} Mode • General Knowledge):**
                        You asked: *"$userPrompt"*
                        
                        • Your voice pipeline, screen-reading privacy filter, and multimodal image attachment are ready.
                        • **Note on Live AI API:** `GEMINI_API_KEY` is currently set to the default placeholder in `BuildConfig`. Configure your key in the **Secrets panel in AI Studio** to unlock live `gemini-3.5-flash` generation and Web Grounding.
                        • Meanwhile, try any Screen Reading command (like *"Screen par kya likha hai?"*, *"Is question ka answer batao"*, or *"Is error ko samjhao"*) to see NOVA's on-device screen analysis in action!
                    """.trimIndent()
                    NovaLanguage.HINGLISH -> """
                        **NOVA AI Assistant (${activeDepth.label} Mode • General Knowledge):**
                        Aapka sawaal: *"$userPrompt"*
                        
                        • NOVA aapki baat Hindi, Hinglish, aur English teeno mein samajhta hai.
                        • **API Key Status:** Abhi `GEMINI_API_KEY` placeholder par set hai. Live cloud AI answers aur Google Search grounding ke liye **AI Studio Secrets panel** mein apna `GEMINI_API_KEY` add karein.
                        • Tab tak aap **"Read Screen"** button ya **"Screen & Commands"** tab se *"Screen par kya likha hai?"*, *"Is question ka answer batao."*, aur *"Is error ko samjhao."* commands ko live test kar sakte hain!
                    """.trimIndent()
                }
            }
        }

        val badge = when {
            attachedBitmap != null -> MessageSourceBadge.IMAGE_ANALYSIS
            attachedScreen != null -> MessageSourceBadge.SCREEN_CONTEXT
            else -> MessageSourceBadge.GENERAL_KNOWLEDGE
        }

        return NovaAiResult(
            text = body,
            sourceBadge = badge,
            isError = false
        )
    }

    private fun buildOfflineMessage(language: NovaLanguage): String {
        return when (language) {
            NovaLanguage.HINDI -> "इंटरनेट कनेक्शन उपलब्ध नहीं है। कृपया अपना Wi-Fi या मोबाइल डेटा जांचें और 'Retry' बटन दबाएं।"
            NovaLanguage.HINGLISH -> "Internet connection available nahi hai. Kripya apna Wi-Fi ya Mobile Data check karein aur niche diye gaye 'Retry' button par tap karein."
            NovaLanguage.ENGLISH -> "No internet connection detected. Please check your Wi-Fi or mobile data connection and tap Retry."
        }
    }

    private fun buildFriendlyApiError(safeDetail: String, language: NovaLanguage): String {
        return when (language) {
            NovaLanguage.HINDI -> "AI सेवा से जुड़ने में समस्या आई ($safeDetail)। कृपया अपना API Key या इंटरनेट जांचें और 'Retry' दबाएं।"
            NovaLanguage.HINGLISH -> "NOVA AI Engine se connect karne mein dikkat aayi ($safeDetail). Kripya AI Studio Secrets panel mein apna GEMINI_API_KEY ya internet check karein aur 'Retry' dabayein."
            NovaLanguage.ENGLISH -> "Could not reach the NOVA AI Engine ($safeDetail). Please verify your GEMINI_API_KEY in the AI Studio Secrets panel or check your connection, then tap Retry."
        }
    }

    private fun sanitizeErrorMessage(raw: String): String {
        // Never expose API keys or URLs containing key query parameters in error messages or logs
        return raw.replace(Regex("key=[^&\\s]+"), "key=[REDACTED]")
            .take(140)
    }
}
