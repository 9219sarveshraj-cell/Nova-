package com.example

import com.example.model.NovaLanguage
import com.example.model.ResponseDepth
import com.example.service.NovaCommandRouter
import com.example.service.SensitiveContentFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun commandRouter_detectsHinglishScreenReadCommands() {
        val parsed = NovaCommandRouter.parseUserCommand("Screen par kya likha hai?", null)
        assertTrue(parsed.requiresScreenRead)
        assertTrue(parsed.isCustomCommandMatch)
    }

    @Test
    fun commandRouter_detectsHindiAndStepByStepOverrides() {
        val hindiCmd = NovaCommandRouter.parseUserCommand("NOVA, Hindi mein jawab do.", null)
        assertEquals(NovaLanguage.HINDI, hindiCmd.overrideLanguage)

        val stepsCmd = NovaCommandRouter.parseUserCommand("NOVA, mujhe step by step samjhao.", "Quantum Computing")
        assertEquals(ResponseDepth.STEP_BY_STEP, stepsCmd.overrideDepth)
    }

    @Test
    fun sensitiveContentFilter_redactsOtpAndDetectsBankingPackages() {
        val (sanitized, redactions) = SensitiveContentFilter.sanitizeText(
            rawText = "Your login OTP is 482910. Do not share this verification code: 9912.",
            strictRedaction = true
        )
        assertTrue(redactions >= 1)
        assertFalse(sanitized.contains("482910"))
        assertTrue(SensitiveContentFilter.isSensitivePackage("com.example.upi.bankapp"))
        assertFalse(SensitiveContentFilter.isSensitivePackage("com.news.techdigest.reader"))
    }
}
