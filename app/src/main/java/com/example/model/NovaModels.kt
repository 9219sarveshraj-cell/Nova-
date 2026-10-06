package com.example.model

import kotlinx.serialization.Serializable

enum class AssistantState(val statusLabel: String, val subLabel: String) {
    IDLE("NOVA Ready", "Tap mic, type, or ask about your screen"),
    LISTENING("Listening...", "Speak in Hindi, Hinglish, or English"),
    ANALYZING_SCREEN("Reading Screen...", "Scanning visible UI & redacting private fields"),
    THINKING("Synthesizing...", "Processing context with NOVA AI Engine"),
    SPEAKING("Speaking...", "Reading response aloud via TTS"),
    ERROR("Attention Needed", "Check connection or retry request")
}

enum class NovaLanguage(
    val code: String,
    val displayName: String,
    val shortBadge: String,
    val speechLocaleTag: String,
    val systemPromptDirective: String
) {
    HINGLISH(
        code = "hinglish",
        displayName = "Hinglish (Hindi + English)",
        shortBadge = "Hinglish",
        speechLocaleTag = "en-IN",
        systemPromptDirective = "Respond naturally in conversational Hinglish (a friendly, clear mix of Hindi written in Roman script and English), matching the user's tone. If the user writes in pure English or pure Devanagari Hindi, adapt smoothly."
    ),
    ENGLISH(
        code = "en",
        displayName = "English",
        shortBadge = "EN",
        speechLocaleTag = "en-US",
        systemPromptDirective = "Respond clearly in English. Even if the user gives a command in Hindi or Hinglish, understand it accurately and reply in clear, natural English unless explicitly asked to reply in Hindi."
    ),
    HINDI(
        code = "hi",
        displayName = "हिंदी (Hindi)",
        shortBadge = "हिंदी",
        speechLocaleTag = "hi-IN",
        systemPromptDirective = "Respond clearly in natural Hindi (Devanagari script) using simple, easy-to-understand words, while keeping technical terms easy to grasp."
    );

    companion object {
        fun fromCode(code: String): NovaLanguage =
            entries.firstOrNull { it.code == code } ?: HINGLISH
    }
}

enum class ResponseDepth(
    val id: String,
    val label: String,
    val promptInstruction: String
) {
    CONCISE(
        id = "concise",
        label = "Concise",
        promptInstruction = "Give a concise, direct, high-clarity answer by default (2-5 bullet points or a short paragraph). Avoid unnecessary fluff unless the user explicitly asks for detail."
    ),
    DETAILED(
        id = "detailed",
        label = "Detailed",
        promptInstruction = "Provide a comprehensive, well-structured explanation with examples, analogies, and clear breakdowns so difficult topics become effortless to understand."
    ),
    STEP_BY_STEP(
        id = "step_by_step",
        label = "Step-by-Step",
        promptInstruction = "Break the solution down into numbered, easy-to-follow steps (Step 1, Step 2, etc.), explaining the 'why' behind each step in simple language."
    );

    companion object {
        fun fromId(id: String): ResponseDepth =
            entries.firstOrNull { it.id == id } ?: CONCISE
    }
}

enum class ThemePreference(val id: String, val label: String) {
    DARK("dark", "Futuristic Dark"),
    LIGHT("light", "Solar Light"),
    SYSTEM("system", "System Default");

    companion object {
        fun fromId(id: String): ThemePreference =
            entries.firstOrNull { it.id == id } ?: DARK
    }
}

enum class AiModelOption(
    val modelId: String,
    val displayName: String,
    val description: String
) {
    GEMINI_FLASH(
        modelId = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash (Default)",
        description = "Fastest multimodal reasoning for chat, voice, screen & image analysis"
    ),
    GEMINI_PRO(
        modelId = "gemini-3.1-pro-preview",
        displayName = "Gemini 3.1 Pro Preview",
        description = "Deepest reasoning for complex coding, STEM, and detailed problem solving"
    ),
    GEMINI_FLASH_LITE(
        modelId = "gemini-3.1-flash-lite-preview",
        displayName = "Gemini 3.1 Flash Lite",
        description = "Ultra-lightweight model optimized for low-latency responses"
    );

    companion object {
        fun fromModelId(id: String): AiModelOption =
            entries.firstOrNull { it.modelId == id } ?: GEMINI_FLASH
    }
}

enum class MessageSourceBadge(val label: String) {
    GENERAL_KNOWLEDGE("General AI Knowledge"),
    LIVE_WEB_SEARCH("Live Web Search Grounded"),
    SCREEN_CONTEXT("Screen Context Analyzed"),
    IMAGE_ANALYSIS("Screenshot / Image Analyzed"),
    CUSTOM_COMMAND("NOVA Voice Command")
}

@Serializable
data class ScreenSnapshot(
    val packageName: String,
    val screenTitle: String,
    val visibleTextSummary: String,
    val uiElements: List<String>,
    val redactedSensitiveCount: Int,
    val capturedAtMillis: Long = System.currentTimeMillis(),
    val isSampleSimulation: Boolean = false
)

data class CustomCommandPreset(
    val id: String,
    val phrase: String,
    val subtitle: String,
    val category: String,
    val requiresScreenContext: Boolean = false,
    val forcesHindi: Boolean = false,
    val forcesStepByStep: Boolean = false
)

object NovaPresets {
    val customCommands = listOf(
        CustomCommandPreset(
            id = "cmd_screen_what",
            phrase = "Screen par kya likha hai?",
            subtitle = "Reads visible on-screen text & summarizes UI elements safely",
            category = "Screen Reading",
            requiresScreenContext = true
        ),
        CustomCommandPreset(
            id = "cmd_screen_explain",
            phrase = "Is page ko samjhao.",
            subtitle = "Explains the current screen or article in simple language",
            category = "Screen Reading",
            requiresScreenContext = true
        ),
        CustomCommandPreset(
            id = "cmd_screen_answer",
            phrase = "Is question ka answer batao.",
            subtitle = "Finds and solves the question visible on your screen or image",
            category = "Screen Reading",
            requiresScreenContext = true
        ),
        CustomCommandPreset(
            id = "cmd_screen_error",
            phrase = "Is error ko samjhao.",
            subtitle = "Diagnoses on-screen error messages and gives a fix",
            category = "Screen Reading",
            requiresScreenContext = true
        ),
        CustomCommandPreset(
            id = "cmd_nova_read",
            phrase = "NOVA, screen read karo.",
            subtitle = "Captures a fresh snapshot of permitted on-screen text",
            category = "Voice Command",
            requiresScreenContext = true
        ),
        CustomCommandPreset(
            id = "cmd_nova_explain",
            phrase = "NOVA, isko explain karo.",
            subtitle = "Explains the current topic or screen in easy terms",
            category = "Voice Command",
            requiresScreenContext = false
        ),
        CustomCommandPreset(
            id = "cmd_nova_answer",
            phrase = "NOVA, iska answer batao.",
            subtitle = "Gives a direct, accurate answer to the active question",
            category = "Voice Command",
            requiresScreenContext = false
        ),
        CustomCommandPreset(
            id = "cmd_nova_translate",
            phrase = "NOVA, translate karo.",
            subtitle = "Translates screen text or last message between Hindi & English",
            category = "Voice Command",
            requiresScreenContext = false
        ),
        CustomCommandPreset(
            id = "cmd_nova_summarize",
            phrase = "NOVA, summarize karo.",
            subtitle = "Condenses long text or screen content into key bullet points",
            category = "Voice Command",
            requiresScreenContext = false
        ),
        CustomCommandPreset(
            id = "cmd_nova_steps",
            phrase = "NOVA, mujhe step by step samjhao.",
            subtitle = "Breaks down the explanation into numbered steps",
            category = "Voice Command",
            requiresScreenContext = false,
            forcesStepByStep = true
        ),
        CustomCommandPreset(
            id = "cmd_nova_hindi",
            phrase = "NOVA, Hindi mein jawab do.",
            subtitle = "Switches response output to clear Devanagari Hindi",
            category = "Voice Command",
            requiresScreenContext = false,
            forcesHindi = true
        )
    )

    val sampleScreenContexts = listOf(
        ScreenSnapshot(
            packageName = "com.android.ide.logcat.viewer",
            screenTitle = "Compiler & Crash Log Inspector",
            visibleTextSummary = "FATAL EXCEPTION: main\nProcess: com.sample.shopapp, PID: 18492\njava.lang.NullPointerException: Attempt to invoke virtual method 'int java.lang.String.length()' on a null object reference at com.sample.shopapp.CheckoutActivity.validateCoupon(CheckoutActivity.kt:84)\nStatus: Build failed with 1 unhandled exception.",
            uiElements = listOf(
                "[Toolbar] Crash Log Viewer",
                "[TextView] java.lang.NullPointerException at CheckoutActivity.kt:84",
                "[Button] Copy Stacktrace",
                "[Button] Re-run Debug Build",
                "[PasswordField] [REDACTED SENSITIVE FIELD]"
            ),
            redactedSensitiveCount = 1,
            isSampleSimulation = true
        ),
        ScreenSnapshot(
            packageName = "com.edu.examportal.quiz",
            screenTitle = "Science & Tech Practice Quiz — Q14",
            visibleTextSummary = "Question 14 of 20:\nWhy does the sky appear blue during a clear day, whereas sunsets appear red-orange?\nOptions:\n(A) Total Internal Reflection of sunlight in clouds\n(B) Rayleigh Scattering of shorter blue wavelengths by atmospheric molecules\n(C) Gravitational lensing by Earth's magnetic field\n(D) Absorption of blue light by ozone layer",
            uiElements = listOf(
                "[Header] Practice Quiz - Physics (Optics)",
                "[QuestionCard] Why does the sky appear blue during a clear day?",
                "[RadioButton] Option A: Total Internal Reflection",
                "[RadioButton] Option B: Rayleigh Scattering of shorter wavelengths",
                "[RadioButton] Option C: Gravitational lensing",
                "[RadioButton] Option D: Absorption by ozone",
                "[Button] Submit Answer"
            ),
            redactedSensitiveCount = 0,
            isSampleSimulation = true
        ),
        ScreenSnapshot(
            packageName = "com.news.techdigest.reader",
            screenTitle = "Understanding Quantum Computing & Qubits",
            visibleTextSummary = "Article: How Quantum Computers Differ from Classical Supercomputers.\nClassical computers process data in binary bits (0 or 1). Quantum computers use quantum bits (qubits) that can exist in a linear superposition of both 0 and 1 simultaneously, and leverage quantum entanglement to evaluate vast combinations in parallel for cryptography, molecular simulation, and optimization.",
            uiElements = listOf(
                "[Title] Understanding Quantum Computing & Qubits",
                "[Paragraph] Superposition and Entanglement overview",
                "[Chip] 4 min read",
                "[Button] Bookmark Article"
            ),
            redactedSensitiveCount = 0,
            isSampleSimulation = true
        )
    )
}
