package com.example.service

object SensitiveContentFilter {

    private val blockedPackageKeywords = listOf(
        "bank", "paytm", "phonepe", "gpay", "bhim", "upi", "wallet",
        "authenticator", "1password", "bitwarden", "keepass", "lastpass", "autofill"
    )

    private val otpOrPinRegex = Regex(
        pattern = "(?i)(otp|pin|cvv|passcode|password|verification\\s*code|secret\\s*code)[^0-9a-zA-Z]{0,15}[0-9]{4,8}",
        option = RegexOption.IGNORE_CASE
    )

    private val cardNumberRegex = Regex("\\b(?:\\d[ -]*?){13,19}\\b")

    fun isSensitivePackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val lower = packageName.lowercase()
        return blockedPackageKeywords.any { lower.contains(it) }
    }

    fun sanitizeText(rawText: String, strictRedaction: Boolean = true): Pair<String, Int> {
        if (rawText.isBlank()) return "" to 0
        if (!strictRedaction) return rawText.trim() to 0

        var redactionCount = 0
        var cleaned = rawText

        cleaned = otpOrPinRegex.replace(cleaned) {
            redactionCount++
            "[REDACTED OTP/PIN]"
        }

        cleaned = cardNumberRegex.replace(cleaned) {
            redactionCount++
            "[REDACTED CARD/ACCOUNT NUMBER]"
        }

        return cleaned.trim() to redactionCount
    }
}
