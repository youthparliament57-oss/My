package com.example.brain

import java.util.regex.Pattern

object ConstitutionalGuardrails {

    // 13 Harmful patterns to block
    private val HARMFUL_PATTERNS = listOf(
        Pattern.compile("(?i)\\bhow\\s+to\\s+(make|build|create)\\s+(bomb|explosive|grenade|improvised\\s+device)\\b"),
        Pattern.compile("(?i)\\bsuicide\\s+methods|kill\\s+myself|end\\s+my\\s+life|hanging\\s+instruction\\b"),
        Pattern.compile("(?i)\\b(hack|crack|bypass|exploit)\\s+(into|system|database|bank|password|biometric)\\b"),
        Pattern.compile("(?i)\\bsteal\\s+(credentials|credit\\s+card|identity|aadhaar|otp|passwords)\\b"),
        Pattern.compile("(?i)\\billicit\\s+surveillance|spy\\s+on\\s+(wife|husband|phone|neighbor)\\b"),
        Pattern.compile("(?i)\\bcraft\\s+(automatic\\s+weapon|firearm|ghost\\s+gun|unlicensed\\s+ammo)\\b"),
        Pattern.compile("(?i)\\bphishing\\s+site\\s+generator|malware\\s+source\\s+code\\b"),
        Pattern.compile("(?i)\\bbypass\\s+(fido2|fingerprint\\s+sensor|faceid|lockscreen)\\b"),
        Pattern.compile("(?i)\\bspoof\\s+bank\\s+account|money\\s+laundering\\s+methods\\b"),
        Pattern.compile("(?i)\\bhow\\s+to\\s+manufacture\\s+(meth|fentanyl|illegal\\s+drugs|heroin)\\b"),
        Pattern.compile("(?i)\\bintercept\\s+(cellular\\s+traffic|sms\\s+otp|sim\\s+swap)\\b"),
        Pattern.compile("(?i)\\bsocial\\s+engineering\\s+script\\s+to\\s+defraud\\b"),
        Pattern.compile("(?i)\\bdistributed\\s+denial\\s+of\\s+service|ddos\\s+tool\\b")
    )

    // 10 Dangerous tools/commands to block
    private val DANGEROUS_TOOLS = listOf(
        Pattern.compile("(?i)\\b(wipe|format|delete)\\s+(all\\s+files|filesystem|partition|storage|sdcard)\\b"),
        Pattern.compile("(?i)\\b(factory\\s+reset|hard\\s+reset|brick\\s+device)\\b"),
        Pattern.compile("(?i)\\bdelete\\s+(all\\s+contacts|entire\\s+database|call\\s+logs)\\b"),
        Pattern.compile("(?i)\\bsend\\s+sms\\s+to\\s+all\\s+contacts\\b"),
        Pattern.compile("(?i)\\brecursive\\s+background\\s+network\\s+spoofing\\b"),
        Pattern.compile("(?i)\\boverclock\\s+cpu\\s+voltage\\b"),
        Pattern.compile("(?i)\\bdisable\\s+security\\s+logging|stop\\s+selinux\\b"),
        Pattern.compile("(?i)\\bforce\\s+root\\s+exploit\\b"),
        Pattern.compile("(?i)\\bbroadcast\\s+silent\\s+push\\s+notifications\\b"),
        Pattern.compile("(?i)\\bflash\\s+(custom\\s+firmware|recovery)\\b")
    )

    // Golden Constitutional Rule
    const val GOLDEN_RULE = "System Rule: You are NOUS, an advanced, highly specialized AI mental companion. You must NEVER speak like a generic search assistant. You are forbidden from starting or containing disclaimers like 'As an AI language model' or 'Please consult a professional'. Avoid safe phrases like 'I understand your concern'. Remain in-character as the user's authentic cognitive partner."

    fun checkSafety(input: String): SafetyResult {
        val trimmed = input.trim()
        
        // Check 13 harmful patterns
        for (pattern in HARMFUL_PATTERNS) {
            if (pattern.matcher(trimmed).find()) {
                return SafetyResult.Blocked("I cannot assist with queries regarding hazardous materials, illegal systems exploitation, or self-harm protocols. Let's redirect our focus to constructive cognitive expansion.")
            }
        }

        // Check 10 dangerous tools
        for (pattern in DANGEROUS_TOOLS) {
            if (pattern.matcher(trimmed).find()) {
                return SafetyResult.Blocked("System level error: This operation violates device automation boundaries and cannot be executed due to strict platform isolation constraints.")
            }
        }

        return SafetyResult.Safe
    }

    sealed class SafetyResult {
        object Safe : SafetyResult()
        data class Blocked(val response: String) : SafetyResult()
    }
}
