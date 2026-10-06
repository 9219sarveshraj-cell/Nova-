package com.example.service

import com.example.model.NovaLanguage
import com.example.model.ResponseDepth

data class ParsedNovaCommand(
    val cleanedPromptForAi: String,
    val requiresScreenRead: Boolean = false,
    val overrideLanguage: NovaLanguage? = null,
    val overrideDepth: ResponseDepth? = null,
    val isCustomCommandMatch: Boolean = false,
    val matchedCommandTitle: String? = null
)

object NovaCommandRouter {

    fun parseUserCommand(rawInput: String, lastAssistantOrUserText: String?): ParsedNovaCommand {
        val trimmed = rawInput.trim()
        val lower = trimmed.lowercase()

        val hasHindiOverride = lower.contains("hindi mein jawab do") ||
            lower.contains("hindi me jawab do") ||
            lower.contains("reply in hindi") ||
            lower.contains("हिंदी में जवाब")

        val hasStepByStepOverride = lower.contains("step by step") ||
            lower.contains("step-by-step") ||
            lower.contains("kadam dar kadam")

        val isScreenWhatCommand = lower.contains("screen par kya likha hai") ||
            lower.contains("screen pe kya likha hai") ||
            lower.contains("screen read karo") ||
            lower.contains("read screen") ||
            lower.contains("what is on my screen")

        val isPageExplainCommand = lower.contains("is page ko samjhao") ||
            lower.contains("explain this page") ||
            lower.contains("screen samjhao")

        val isQuestionAnswerCommand = lower.contains("is question ka answer batao") ||
            lower.contains("iska answer batao") ||
            lower.contains("solve this question")

        val isErrorExplainCommand = lower.contains("is error ko samjhao") ||
            lower.contains("explain this error") ||
            lower.contains("error fix batao")

        val isTranslateCommand = lower.contains("translate karo") ||
            lower.contains("isko translate karo")

        val isSummarizeCommand = lower.contains("summarize karo") ||
            lower.contains("iska summary batao")

        val isGenericExplainCommand = lower.contains("isko explain karo") ||
            lower.contains("mujhe step by step samjhao")

        val needsScreen = isScreenWhatCommand ||
            isPageExplainCommand ||
            isQuestionAnswerCommand ||
            isErrorExplainCommand

        val resolvedPrompt = when {
            isScreenWhatCommand ->
                "User Command: \"$trimmed\". Read the attached on-screen text and UI elements carefully, tell the user clearly what is written on the screen, and highlight the most important items."
            isPageExplainCommand ->
                "User Command: \"$trimmed\". Explain the content of this screen/page in simple, easy-to-understand language with key takeaways."
            isQuestionAnswerCommand ->
                "User Command: \"$trimmed\". Identify the question visible on the screen (or in the conversation/image), state the correct answer clearly first, and then briefly explain why it is correct."
            isErrorExplainCommand ->
                "User Command: \"$trimmed\". Analyze the error message or stacktrace visible on the screen, explain what caused the error in simple language, and provide exact step-by-step instructions to fix it."
            isTranslateCommand -> {
                val contextTarget = lastAssistantOrUserText?.takeIf { it.isNotBlank() }
                if (contextTarget != null && trimmed.length < 35) {
                    "Translate the following content into both natural Hindi (Devanagari) and clear English:\n\n\"$contextTarget\""
                } else {
                    "User Command: \"$trimmed\". Translate the provided text or on-screen content between Hindi and English clearly."
                }
            }
            isSummarizeCommand -> {
                val contextTarget = lastAssistantOrUserText?.takeIf { it.isNotBlank() }
                if (contextTarget != null && trimmed.length < 35) {
                    "Summarize the following into concise, high-impact bullet points in simple language:\n\n\"$contextTarget\""
                } else {
                    "User Command: \"$trimmed\". Summarize the visible screen content or topic into concise key bullet points."
                }
            }
            isGenericExplainCommand && trimmed.length < 40 && !lastAssistantOrUserText.isNullOrBlank() -> {
                "Explain the following topic step-by-step in very simple, beginner-friendly language:\n\n\"$lastAssistantOrUserText\""
            }
            else -> trimmed
        }

        val isMatched = needsScreen || isTranslateCommand || isSummarizeCommand ||
            isGenericExplainCommand || hasHindiOverride || hasStepByStepOverride

        return ParsedNovaCommand(
            cleanedPromptForAi = resolvedPrompt,
            requiresScreenRead = needsScreen,
            overrideLanguage = if (hasHindiOverride) NovaLanguage.HINDI else null,
            overrideDepth = if (hasStepByStepOverride) ResponseDepth.STEP_BY_STEP else null,
            isCustomCommandMatch = isMatched,
            matchedCommandTitle = if (isMatched) trimmed else null
        )
    }
}
