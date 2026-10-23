package com.velvet.superkeyboard

/**
 * Lightweight offline writing helpers. These are deterministic text transformations,
 * not a generative AI model; no text leaves the device.
 */
object OfflineWritingAssistant {
    fun improve(text: String, mode: String): String {
        val clean = text.trim().replace(Regex("\\s+"), " ")
        if (clean.isEmpty()) return text
        return when (mode) {
            "Fix grammar" -> fixGrammar(clean)
            "Make concise" -> concise(clean)
            "Professional tone" -> professional(clean)
            "Friendly tone" -> friendly(clean)
            "Expand slightly" -> expand(clean)
            "Sentence case" -> clean.replaceFirstChar { it.uppercase() }
            else -> clean
        }
    }

    private fun fixGrammar(text: String): String {
        var result = text
            .replace(Regex("\\bi\\b"), "I")
            .replace(Regex("\\s+([,.!?])"), "$1")
            .replace(Regex("\\b(teh|adn|taht|thier|recieve|becuase|helo)\\b", RegexOption.IGNORE_CASE)) {
                OfflinePredictionEngine.correct(it.value)
            }
            .replace(Regex("\\b(i am|i have|i will)\\b", RegexOption.IGNORE_CASE)) {
                it.value.replaceFirstChar { c -> c.uppercase() }
            }
        result = result.replaceFirstChar { it.uppercase() }
        if (result.lastOrNull() !in listOf('.', '!', '?')) result += "."
        return result
    }

    private fun concise(text: String): String {
        val fillers = listOf("basically ", "actually ", "just ", "really ", "very ")
        var result = text
        fillers.forEach { result = result.replace(it, "", ignoreCase = true) }
        return result.replace(Regex("\\s+"), " ").trim()
    }

    private fun professional(text: String): String {
        var result = fixGrammar(text)
        val replacements = listOf(
            Regex("\\bcan't\\b", RegexOption.IGNORE_CASE) to "cannot",
            Regex("\\bdon't\\b", RegexOption.IGNORE_CASE) to "do not",
            Regex("\\bguys\\b", RegexOption.IGNORE_CASE) to " everyone",
            Regex("\\bhey\\b", RegexOption.IGNORE_CASE) to "Hello"
        )
        replacements.forEach { (pattern, replacement) -> result = pattern.replace(result, replacement) }
        return result
    }

    private fun friendly(text: String): String {
        val result = fixGrammar(text).removeSuffix(".")
        return if (result.endsWith("!")) result else "$result 😊"
    }

    private fun expand(text: String): String {
        val base = fixGrammar(text).removeSuffix(".")
        return "$base. Please let me know if you would like any additional details."
    }
}
