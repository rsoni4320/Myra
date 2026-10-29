package com.example.commands

import java.util.Locale

sealed class ParsedVoiceCommand {
    data class LaunchApp(val appKey: String, val rawAppQuery: String) : ParsedVoiceCommand()
    data class WebSearch(val searchQuery: String) : ParsedVoiceCommand()
    data class OpenUrl(val url: String) : ParsedVoiceCommand()
    data class SetTimer(val durationSeconds: Int, val rawText: String) : ParsedVoiceCommand()
    data class SetAlarm(val hour: Int, val minute: Int, val rawText: String) : ParsedVoiceCommand()
    data class Scroll(val scrollUp: Boolean) : ParsedVoiceCommand()
    data class Tap(val label: String) : ParsedVoiceCommand()
    object GoBack : ParsedVoiceCommand()
    object StopSpeaking : ParsedVoiceCommand()
    object UnlockPhone : ParsedVoiceCommand()
    data class AnswerInLanguage(val targetLanguage: String, val prompt: String) : ParsedVoiceCommand()
    data class Conversational(val text: String) : ParsedVoiceCommand()
}

class VoiceCommandParser {

    private val appSynonyms = mapOf(
        "youtube" to listOf("youtube", "yt", "यूट्यूब"),
        "calculator" to listOf("calculator", "calc", "कैलकुलेटर", "calci"),
        "gallery" to listOf("gallery", "photos", "photo", "images", "गैलरी", "फ़ोटो"),
        "chrome" to listOf("chrome", "browser", "google chrome", "क्रोम"),
        "settings" to listOf("settings", "setting", "फोन सेटिंग्स", "सेटिंग्स"),
        "whatsapp" to listOf("whatsapp", "व्हाट्सएप", "whats app"),
        "maps" to listOf("maps", "map", "google maps", "मैप्स", "नक्शा"),
        "camera" to listOf("camera", "कैमरा"),
        "gmail" to listOf("gmail", "email", "mail", "जीमेल", "ईमेल")
    )

    fun parse(input: String): ParsedVoiceCommand {
        val raw = input.trim()
        if (raw.isEmpty()) return ParsedVoiceCommand.Conversational("")
        val lower = raw.lowercase(Locale.ROOT)

        // 1. Stop Speaking / Interruption Commands
        if (isStopCommand(lower)) {
            return ParsedVoiceCommand.StopSpeaking
        }

        // 2. Answer in specific language (e.g. "Answer this question in Hindi", "Hindi me jawab do")
        val langCommand = matchLanguageCommand(raw, lower)
        if (langCommand != null) {
            return langCommand
        }

        // 3. Web Search Commands (e.g. "Search Google for Kotlin tutorials", "Google par search karo ...", "Search for ...")
        val searchCommand = matchSearchCommand(raw, lower)
        if (searchCommand != null) {
            return searchCommand
        }

        // 4. Timer Commands (e.g. "Set a timer for one minute", "1 minute ka timer lagao")
        val timerCommand = matchTimerCommand(raw, lower)
        if (timerCommand != null) {
            return timerCommand
        }

        // 5. Alarm Commands (e.g. "Set an alarm for 7 AM", "Subah 7 baje ka alarm lagao")
        val alarmCommand = matchAlarmCommand(raw, lower)
        if (alarmCommand != null) {
            return alarmCommand
        }

        // 6. Direct URL Commands (e.g. "Open https://...")
        val urlCommand = matchUrlCommand(raw, lower)
        if (urlCommand != null) {
            return urlCommand
        }

        // 6.5 Accessibility Automation Commands (Scroll, Tap, Go Back)
        val automationCommand = matchAutomationCommand(raw, lower)
        if (automationCommand != null) {
            return automationCommand
        }

        // 7. App Launch Commands (English, Hindi, Hinglish)
        // e.g. "Open YouTube", "YouTube kholo", "Calculator open karo", "Open Gallery", etc.
        val appCommand = matchAppLaunchCommand(raw, lower)
        if (appCommand != null) {
            return appCommand
        }

        // 8. Fallback to general conversational AI
        return ParsedVoiceCommand.Conversational(raw)
    }

    private fun isStopCommand(lower: String): Boolean {
        return lower in listOf(
            "stop speaking",
            "stop",
            "shut up",
            "silence",
            "quiet",
            "pause speaking",
            "bolna band karo",
            "chup ho jao",
            "chup raho",
            "ruk jao",
            "band karo",
            "ruk ja",
            "शांत रहो"
        ) || lower.startsWith("stop speaking") || lower.startsWith("bolna band karo")
    }

    private fun matchLanguageCommand(raw: String, lower: String): ParsedVoiceCommand? {
        val answerInHindiPattern = Regex("(?i)^(answer\\s+(?:this\\s+)?(?:question\\s+)?in\\s+hindi|hindi\\s+m(?:e|ein)\\s+jawab\\s+do|hindi\\s+me\\s+batao)(?:\\s*[:,-]?\\s*(.*))?$")
        val matchHindi = answerInHindiPattern.find(raw)
        if (matchHindi != null) {
            val content = matchHindi.groupValues.getOrNull(2)?.trim().orEmpty()
            return ParsedVoiceCommand.AnswerInLanguage("Hindi", content.ifEmpty { raw })
        }

        val answerInEnglishPattern = Regex("(?i)^(answer\\s+(?:this\\s+)?(?:question\\s+)?in\\s+english|english\\s+m(?:e|ein)\\s+jawab\\s+do|english\\s+me\\s+batao)(?:\\s*[:,-]?\\s*(.*))?$")
        val matchEnglish = answerInEnglishPattern.find(raw)
        if (matchEnglish != null) {
            val content = matchEnglish.groupValues.getOrNull(2)?.trim().orEmpty()
            return ParsedVoiceCommand.AnswerInLanguage("English", content.ifEmpty { raw })
        }

        return null
    }

    private fun matchSearchCommand(raw: String, lower: String): ParsedVoiceCommand? {
        val googleSearchPrefixes = listOf(
            "search google for",
            "search on google for",
            "google search for",
            "search the web for",
            "search web for",
            "search for",
            "google for",
            "google par search karo",
            "google pe search karo",
            "par search karo",
            "search karo"
        )

        for (prefix in googleSearchPrefixes) {
            if (lower.startsWith(prefix)) {
                val query = raw.substring(prefix.length).trim().trim(':', '-', ',')
                if (query.isNotEmpty()) {
                    return ParsedVoiceCommand.WebSearch(query)
                }
            }
        }

        // Suffix patterns like: "... google par search karo" or "... search karo"
        val suffixPattern = Regex("(?i)(.*?)\\s+(?:google\\s+par|google\\s+pe|internet\\s+par|web\\s+par)?\\s*search\\s+karo$")
        val suffixMatch = suffixPattern.find(raw)
        if (suffixMatch != null) {
            val query = suffixMatch.groupValues[1].trim()
            if (query.isNotEmpty() && !query.equals("open", ignoreCase = true)) {
                return ParsedVoiceCommand.WebSearch(query)
            }
        }

        return null
    }

    private fun matchTimerCommand(raw: String, lower: String): ParsedVoiceCommand? {
        if (!lower.contains("timer")) return null

        // "Set a timer for one minute", "Set timer for 5 minutes", "timer for 30 seconds"
        val timerEnglish = Regex("(?i)(?:set\\s+(?:a\\s+)?timer\\s+for\\s+)(\\w+|\\d+)\\s*(second|minute|sec|min|hour|घंटे|मिनट|सेकंड)s?")
        val match = timerEnglish.find(lower)
        if (match != null) {
            val valueStr = match.groupValues[1]
            val unitStr = match.groupValues[2]
            val num = parseNumber(valueStr)
            val seconds = when {
                unitStr.startsWith("sec") || unitStr.startsWith("सेकंड") -> num
                unitStr.startsWith("hour") || unitStr.startsWith("घंटे") -> num * 3600
                else -> num * 60
            }
            return ParsedVoiceCommand.SetTimer(seconds, raw)
        }

        // Hindi: "1 minute ka timer lagao", "5 minute ka timer"
        val timerHindi = Regex("(?i)(\\w+|\\d+)\\s*(minute|second|min|sec|मिनट|सेकंड)\\s*k?a?\\s*timer(?:\\s*(?:lagao|set\\s*karo))?")
        val matchHindi = timerHindi.find(lower)
        if (matchHindi != null) {
            val valueStr = matchHindi.groupValues[1]
            val unitStr = matchHindi.groupValues[2]
            val num = parseNumber(valueStr)
            val seconds = when {
                unitStr.startsWith("sec") || unitStr.startsWith("सेकंड") -> num
                unitStr.startsWith("hour") || unitStr.startsWith("घंटे") -> num * 3600
                else -> num * 60
            }
            return ParsedVoiceCommand.SetTimer(seconds, raw)
        }

        return ParsedVoiceCommand.SetTimer(60, raw)
    }

    private fun matchAlarmCommand(raw: String, lower: String): ParsedVoiceCommand? {
        if (!lower.contains("alarm") && !lower.contains("अलार्म")) return null

        // "Set an alarm for 7 AM", "Alarm for 6:30 PM", "Subah 7 baje ka alarm lagao"
        val alarmPattern = Regex("(?i)(?:set\\s+(?:an\\s+)?alarm\\s+for\\s+|alarm\\s+for\\s+|baje\\s+ka\\s+alarm\\s*)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|सुबह|शाम|रात)?")
        val match = alarmPattern.find(lower)
        if (match != null) {
            val hourVal = match.groupValues[1].toIntOrNull() ?: 7
            val minVal = match.groupValues[2].toIntOrNull() ?: 0
            val amPm = match.groupValues[3].lowercase()

            val finalHour = when {
                (amPm == "pm" || amPm == "शाम" || amPm == "रात") && hourVal < 12 -> hourVal + 12
                (amPm == "am" || amPm == "सुबह") && hourVal == 12 -> 0
                else -> hourVal
            }
            return ParsedVoiceCommand.SetAlarm(finalHour, minVal, raw)
        }

        return ParsedVoiceCommand.SetAlarm(7, 0, raw)
    }

    private fun matchUrlCommand(raw: String, lower: String): ParsedVoiceCommand? {
        if (lower.startsWith("open http://") || lower.startsWith("open https://")) {
            val url = raw.substring(5).trim()
            return ParsedVoiceCommand.OpenUrl(url)
        }
        if (lower.startsWith("https://") || lower.startsWith("http://")) {
            return ParsedVoiceCommand.OpenUrl(raw)
        }
        return null
    }

    private fun matchAppLaunchCommand(raw: String, lower: String): ParsedVoiceCommand? {
        // Look for prefixes like "open", "launch", "start", "run", "kholo", "chalao"
        val englishPrefixes = listOf("open", "launch", "start", "run")
        val hindiSuffixes = listOf("kholo", "chalao", "open karo", "khol do", "kholna", "खोलो", "चलाओ")

        // 1. Check Hindi / Hinglish suffixes: e.g. "YouTube kholo", "Calculator open karo"
        for (suffix in hindiSuffixes) {
            if (lower.endsWith(suffix)) {
                val candidate = lower.removeSuffix(suffix).trim()
                val resolvedApp = resolveAppKey(candidate)
                if (resolvedApp != null) {
                    return ParsedVoiceCommand.LaunchApp(resolvedApp, candidate)
                }
            }
        }

        // 2. Check English prefixes: e.g. "Open YouTube", "Launch Camera"
        for (prefix in englishPrefixes) {
            if (lower.startsWith("$prefix ")) {
                val candidate = lower.removePrefix("$prefix ").trim()
                val resolvedApp = resolveAppKey(candidate)
                if (resolvedApp != null) {
                    return ParsedVoiceCommand.LaunchApp(resolvedApp, candidate)
                }
            }
        }

        // 3. Direct app mentions if exact match: e.g. "youtube", "calculator", "chrome"
        val directApp = resolveAppKey(lower)
        if (directApp != null) {
            return ParsedVoiceCommand.LaunchApp(directApp, lower)
        }

        return null
    }

    private fun matchAutomationCommand(raw: String, lower: String): ParsedVoiceCommand? {
        // Unlock phone / password
        if (lower.contains("unlock phone") || lower.contains("enter password") || lower.contains("phone unlock") || lower.contains("password dalo") || lower.contains("phone kholo password") || lower.contains("unlock kardo")) {
            return ParsedVoiceCommand.UnlockPhone
        }

        // Go back
        if (lower.contains("go back") || lower.contains("wapas jao") || lower.contains("peeche jao") || lower.contains("back jao")) {
            return ParsedVoiceCommand.GoBack
        }

        // Scroll up
        if (lower.contains("scroll up") || lower.contains("upar scroll") || lower.contains("upar jao") || lower.contains("scroll upper")) {
            return ParsedVoiceCommand.Scroll(scrollUp = true)
        }

        // Scroll down
        if (lower.contains("scroll down") || lower.contains("neche scroll") || lower.contains("neeche scroll") || lower.contains("neche jao")) {
            return ParsedVoiceCommand.Scroll(scrollUp = false)
        }

        // Tap / Click on element or video
        val tapPattern = Regex("(?i)(?:tap\\s+(?:on\\s+)?|click\\s+(?:on\\s+)?|par\\s+tap\\s+karo|par\\s+click\\s+karo)\\s+(.*)")
        val matchTap = tapPattern.find(raw)
        if (matchTap != null) {
            val label = matchTap.groupValues[1].trim().removeSuffix("karo").trim()
            if (label.isNotEmpty()) {
                return ParsedVoiceCommand.Tap(label)
            }
        }

        if (lower.contains("pehli video") || lower.contains("first video")) {
            return ParsedVoiceCommand.Tap("video")
        }
        if (lower.contains("doosri video") || lower.contains("second video")) {
            return ParsedVoiceCommand.Tap("video")
        }

        return null
    }

    fun resolveAppKey(query: String): String? {
        val cleaned = query.trim().lowercase(Locale.ROOT)
            .replace(Regex("^(the|app|application|my)\\s+"), "")
            .replace(Regex("\\s+(app|application)$"), "")
            .trim()

        for ((key, synonyms) in appSynonyms) {
            if (cleaned == key || synonyms.any { cleaned.contains(it) }) {
                return key
            }
        }
        return null
    }

    private fun parseNumber(text: String): Int {
        return text.toIntOrNull() ?: when (text.lowercase(Locale.ROOT)) {
            "one", "ek", "एक" -> 1
            "two", "do", "दो" -> 2
            "three", "teen", "तीन" -> 3
            "four", "char", "चार" -> 4
            "five", "panch", "पाँच", "पांच" -> 5
            "ten", "das", "दस" -> 10
            "fifteen", "pandrah" -> 15
            "twenty", "bees" -> 20
            "thirty", "tees" -> 30
            else -> 1
        }
    }
}
