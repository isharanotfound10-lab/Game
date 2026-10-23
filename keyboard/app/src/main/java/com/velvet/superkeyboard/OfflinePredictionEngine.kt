package com.velvet.superkeyboard

/**
 * Small offline prediction engine. All data stays on-device; it intentionally
 * avoids collecting or uploading text typed into password fields.
 */
object OfflinePredictionEngine {
    private val words = listOf(
        "about","after","again","also","always","and","android","any","are","around","as","ask","at",
        "awesome","back","because","been","before","best","better","can","can't","change","check",
        "come","could","day","did","different","do","does","down","easy","even","every","everything",
        "feel","file","find","first","for","from","fun","get","give","go","good","great","had","has",
        "have","he","hello","help","here","hey","how","i","if","in","into","is","it","just","keyboard",
        "know","like","look","love","make","many","me","more","most","much","my","need","new","next",
        "nice","no","not","now","of","off","on","one","only","or","other","our","out","over","please",
        "pretty","really","right","say","see","should","so","some","something","still","super","take",
        "tell","than","thank","that","the","their","them","then","there","these","they","thing","think",
        "this","time","to","today","too","try","up","use","very","want","was","way","we","well","were",
        "what","when","where","which","who","why","will","with","work","would","yeah","yes","you","your"
    )
    private val nextWords = mapOf(
        "i" to listOf("am","can","don't","have","like","need","think","want","will"),
        "you" to listOf("are","can","have","know","like","need","should","want"),
        "how" to listOf("are","can","do","is","much","to"),
        "thank" to listOf("you"),
        "good" to listOf("job","morning","idea","luck"),
        "have" to listOf("a","been","fun","it","to"),
        "what" to listOf("about","are","do","is","you"),
        "it" to listOf("can","does","is","looks","was","will"),
        "the" to listOf("best","first","keyboard","new","same","way")
    )

    fun suggestions(prefix: String, previousWord: String = ""): List<String> {
        val clean = prefix.lowercase().trim()
        if (clean.isEmpty()) {
            return nextWords[previousWord.lowercase()].orEmpty().take(3)
        }
        val matches = words.asSequence()
            .filter { it.startsWith(clean) && it != clean }
            .sortedWith(compareBy<String>({ it.length }, { it }))
            .take(3)
            .toList()
        return if (matches.size < 3 && clean.length >= 2) {
            (matches + listOf(clean)).distinct().take(3)
        } else matches
    }

    fun correct(word: String): String {
        val lower = word.lowercase()
        val common = mapOf("teh" to "the", "adn" to "and", "taht" to "that", "thier" to "their", "recieve" to "receive", "becuase" to "because", "dont" to "don't", "cant" to "can't", "wont" to "won't")
        return common[lower] ?: word
    }
}
