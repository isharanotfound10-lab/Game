package com.velvet.superkeyboard

import android.app.AlertDialog
import org.json.JSONArray
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class SuperKeyboardService : InputMethodService() {
    private var shifted = false
    private var symbols = false
    private var unicode = false
    private var composingWord = ""
    private var suggestionsRow: LinearLayout? = null
    private val swipePath = mutableListOf<String>()
    private val recentClipboard: MutableList<String> by lazy {
        try {
            val json = JSONArray(prefs.getString("clipboard_history_json", "[]") ?: "[]")
            MutableList(json.length()) { index -> json.optString(index) }.filter { it.isNotBlank() }.toMutableList()
        } catch (_: Exception) { mutableListOf() }
    }
    private val learnedWords: MutableSet<String> by lazy {
        prefs.getStringSet("learned_words", emptySet())?.toMutableSet() ?: mutableSetOf()
    }
    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private var swipeMoved = false
    private var lastSpaceAt = 0L
    private val prefs by lazy { getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE) }
    private val themeName get() = prefs.getString("theme", "Purple")
    private val bg: Int get() = if (themeName == "AMOLED") Color.BLACK else Color.rgb(7, 5, 17)
    private val keyColor: Int get() = when (themeName) {
        "Cyber Pink" -> Color.rgb(57,17,43); "Ocean" -> Color.rgb(13,37,54)
        "Emerald" -> Color.rgb(15,43,35); "AMOLED" -> Color.rgb(18,18,22)
        else -> Color.rgb(30,22,48)
    }
    private val themeAccent: Int get() = when (themeName) {
        "Cyber Pink" -> Color.rgb(236,56,188); "Ocean" -> Color.rgb(54,190,255)
        "Emerald" -> Color.rgb(65,220,155); "AMOLED" -> Color.rgb(190,150,255)
        else -> Color.rgb(160,80,255)
    }
    private val sensitiveField: Boolean get() {
        val type = currentInputEditorInfo?.inputType ?: InputType.TYPE_CLASS_TEXT
        val variation = type and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            (type and InputType.TYPE_NUMBER_VARIATION_PASSWORD) == InputType.TYPE_NUMBER_VARIATION_PASSWORD
    }

    override fun onCreateInputView(): View = buildKeyboard()
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting); composingWord = ""
    }

    private fun buildKeyboard(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(6), dp(7), dp(6), dp(8))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(bg, if (themeName == "AMOLED") Color.BLACK else Color.rgb(20,7,38), Color.rgb(8,5,18)))
                .apply { cornerRadius = dp(18).toFloat() }
        }
        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        toolbar.addView(tool("Aa", "Text tools") { showTextTools() }, weight())
        toolbar.addView(tool("▤", "Paste clipboard") { pasteClipboard() }, weight())
        toolbar.addView(tool("☺", "Emoji picker") { showEmojiPicker() }, weight())
        toolbar.addView(tool("GIF", "Search GIFs online") { searchGifs() }, weight())
        toolbar.addView(tool("⌘", "Symbols") { symbols = !symbols; unicode = false; refreshKeyboard() }, weight())
        toolbar.addView(tool("Ω", "Unicode laboratory") { unicode = !unicode; symbols = false; refreshKeyboard() }, weight())
        toolbar.addView(tool("⚙", "Settings") {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }, weight())
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(42)))
        suggestionsRow = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        root.addView(suggestionsRow, LinearLayout.LayoutParams(-1, dp(30)))
        refreshSuggestions()

        val rows: List<List<String>> = when {
            unicode -> listOf(
                listOf("→","⇒","⇢","⟿","⤳","⤷","↔","↕","↗","↘"),
                listOf("∞","≠","≤","≥","±","÷","×","√","∑","π"),
                listOf("★","☆","♡","♥","◆","◇","●","○","▲","△"),
                listOf("꧁","༺","༻","𒐫","⌫","ABC","SPACE","↵"))
            symbols -> listOf(
                listOf("1","2","3","4","5","6","7","8","9","0"),
                listOf("@","#","$","%","&","-","+","(",")","/"),
                listOf("=","*","\"","'",":",";","!","?","⌫"),
                listOf("ABC","🌐",",","SPACE",".","↵"))
            else -> {
                val numberRow = if (prefs.getBoolean("number_row", true)) listOf("1","2","3","4","5","6","7","8","9","0") else emptyList()
                listOfNotNull(numberRow.takeIf { it.isNotEmpty() }) + letterRows() + listOf(listOf("?123","🌐",",","SPACE",".","↵"))
            }
        }
        rows.forEachIndexed { index, row ->
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
            row.forEach { key ->
                val label = when {
                    key == "SPACE" -> prefs.getString("spacebar_label", "English (US)") ?: "English (US)"
                    key.length == 1 && shifted && key[0].isLetter() -> key.uppercase()
                    else -> key
                }
                val special = index == rows.lastIndex || key in listOf("⇧","⌫","?123","ABC","↵")
                val button = key(label, special)
                button.tag = key
                if (!symbols && !unicode && key.length == 1 && key[0].isLetter()) {
                    button.setOnTouchListener { _, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                swipePath.clear()
                                swipePath.add(key.lowercase())
                                swipeStartX = event.rawX
                                swipeStartY = event.rawY
                                swipeMoved = false
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val dx = event.rawX - swipeStartX
                                val dy = event.rawY - swipeStartY
                                if (kotlin.math.sqrt(dx * dx + dy * dy) > dp(14)) swipeMoved = true
                                if (swipeMoved) {
                                    val nearest = findNearestLetter(root, event.rawX, event.rawY)
                                    if (nearest != null && swipePath.lastOrNull() != nearest) swipePath.add(nearest)
                                }
                            }
                            MotionEvent.ACTION_UP -> {
                                if (swipeMoved && swipePath.size >= 3) {
                                    val candidate = OfflinePredictionEngine.swipeCandidate(swipePath.joinToString(""))
                                    if (candidate.isNotEmpty()) {
                                        rememberWord(candidate)
                                        currentInputConnection?.commitText(candidate + " ", 1)
                                        composingWord = ""
                                        refreshSuggestions()
                                    } else {
                                        if (prefs.getBoolean("key_haptics", false)) button.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                        handleKey(key)
                                    }
                                } else {
                                    if (prefs.getBoolean("key_haptics", false)) button.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                    handleKey(key)
                                }
                                swipePath.clear()
                            }
                            MotionEvent.ACTION_CANCEL -> swipePath.clear()
                        }
                        true
                    }
                } else button.setOnClickListener {
                    if (prefs.getBoolean("key_haptics", false)) button.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    handleKey(key)
                }
                if (key == "⌫") button.setOnLongClickListener {
                    if (composingWord.isNotEmpty()) { composingWord = ""; currentInputConnection?.setComposingText("",1); refreshSuggestions() }
                    else currentInputConnection?.deleteSurroundingText(1000,0)
                    true
                }
                val heightExtra = when (prefs.getString("key_size", "Normal")) { "Compact" -> -5; "Tall" -> 6; else -> 0 }
                val lp = LinearLayout.LayoutParams(0, dp((if (key == "SPACE" || key == "↵") 47 else 43) + heightExtra), if (key == "SPACE") 4.1f else 1f)
                lp.setMargins(dp(2),dp(3),dp(2),dp(3)); line.addView(button,lp)
            }
            val heightExtra = when (prefs.getString("key_size", "Normal")) { "Compact" -> -5; "Tall" -> 6; else -> 0 }
            root.addView(line, LinearLayout.LayoutParams(-1,dp((if (index == rows.lastIndex) 51 else 47) + heightExtra)))
        }
        return root
    }
    private fun letterRows(): List<List<String>> = when (prefs.getString("layout", "QWERTY")) {
        "Dvorak" -> listOf(
            listOf("'",",",".","p","y","f","g","c","r","l"),
            listOf("a","o","e","u","i","d","h","t","n","s"),
            listOf("⇧",";","q","j","k","x","b","m","w","v","z","⌫"))
        "Colemak" -> listOf(
            listOf("q","w","f","p","g","j","l","u","y",";"),
            listOf("a","r","s","t","d","h","n","e","i","o"),
            listOf("⇧","z","x","c","v","b","k","m",",",".","⌫"))
        else -> listOf(
            listOf("q","w","e","r","t","y","u","i","o","p"),
            listOf("a","s","d","f","g","h","j","k","l"),
            listOf("⇧","z","x","c","v","b","n","m","⌫"))
    }
    private fun refreshKeyboard() { setInputView(buildKeyboard()) }

    private fun handleKey(key: String) {
        val ic = currentInputConnection ?: return
        when (key) {
            "⇧" -> { shifted = !shifted; refreshKeyboard() }
            "⌫" -> if (composingWord.isNotEmpty()) {
                composingWord = composingWord.dropLast(1)
                ic.setComposingText(composingWord,1); refreshSuggestions()
            } else {
                val selected = ic.getSelectedText(0)
                if (!selected.isNullOrEmpty()) ic.commitText("",1) else ic.deleteSurroundingText(1,0)
            }
            "SPACE" -> {
                if (composingWord.isNotEmpty()) {
                    val corrected = if (prefs.getBoolean("auto_correct",true) && !sensitiveField) OfflinePredictionEngine.correct(composingWord) else composingWord
                    rememberWord(corrected)
                    ic.commitText("$corrected ",1)
                    composingWord = ""
                    lastSpaceAt = 0L
                } else {
                    val now = System.currentTimeMillis()
                    val before = ic.getTextBeforeCursor(1, 0)?.toString().orEmpty()
                    if (prefs.getBoolean("double_space_period", true) && before == " " && now - lastSpaceAt < 500L) {
                        ic.deleteSurroundingText(1, 0)
                        ic.commitText(". ", 1)
                        lastSpaceAt = 0L
                    } else {
                        ic.commitText(" ",1)
                        lastSpaceAt = now
                    }
                }
                refreshSuggestions()
            }
            "↵" -> {
                if (composingWord.isNotEmpty()) { ic.commitText(composingWord,1); composingWord = "" }
                val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                if (action != null && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) ic.performEditorAction(action)
                else ic.commitText("\n",1)
                refreshSuggestions()
            }
            "🌐" -> switchToNextInputMethod(false)
            "?123" -> { symbols = true; unicode = false; refreshKeyboard() }
            "ABC" -> { symbols = false; unicode = false; refreshKeyboard() }
            else -> {
                if (!symbols && !unicode && key.length == 1 && key[0].isLetter() && !sensitiveField) {
                    val before = ic.getTextBeforeCursor(3, 0)?.toString().orEmpty()
                    val sentenceStart = before.isEmpty() || before.endsWith(". ") || before.endsWith("? ") || before.endsWith("! ") || before.endsWith("\n")
                    val uppercase = shifted || (prefs.getBoolean("auto_capitalize", true) && composingWord.isEmpty() && sentenceStart)
                    composingWord += if (uppercase) key.uppercase() else key.lowercase()
                    ic.setComposingText(composingWord,1); refreshSuggestions()
                } else {
                    if (composingWord.isNotEmpty()) { ic.commitText(composingWord,1); composingWord = "" }
                    ic.commitText(if (shifted && key.length == 1) key.uppercase() else key,1)
                    refreshSuggestions()
                }
                if (shifted) { shifted = false; refreshKeyboard() }
            }
        }
    }

    private fun findNearestLetter(container: ViewGroup, rawX: Float, rawY: Float): String? {
        var bestKey: String? = null
        var bestDistance = Float.MAX_VALUE
        fun scan(group: ViewGroup) {
            for (i in 0 until group.childCount) {
                val child = group.getChildAt(i)
                if (child is ViewGroup) scan(child)
                if (child is Button) {
                    val candidate = child.tag as? String ?: continue
                    if (candidate.length != 1 || !candidate[0].isLetter()) continue
                    val location = IntArray(2)
                    child.getLocationOnScreen(location)
                    val centerX = location[0] + child.width / 2f
                    val centerY = location[1] + child.height / 2f
                    val dx = rawX - centerX
                    val dy = rawY - centerY
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (distance < bestDistance) { bestDistance = distance; bestKey = candidate.lowercase() }
                }
            }
        }
        scan(container)
        return if (bestDistance <= dp(58)) bestKey else null
    }

    private fun refreshSuggestions() {
        val row = suggestionsRow ?: return
        row.removeAllViews()
        val matches = if (sensitiveField) emptyList() else {
            val personal = if (prefs.getBoolean("personalized_learning", true)) learnedWords
                .filter { it.startsWith(composingWord.lowercase()) && it != composingWord.lowercase() }
                .sortedBy { it.length }.take(3) else emptyList()
            (personal + OfflinePredictionEngine.suggestions(composingWord, previousWord())).distinct().take(3)
        }
        val labels = if (composingWord.isNotEmpty()) matches else listOf("✦", "Offline typing", "Tools")
        labels.take(3).forEachIndexed { index, value ->
            row.addView(TextView(this).apply {
                text = value; textSize = 13f; gravity = Gravity.CENTER
                setTextColor(if (index == 0) Color.WHITE else Color.rgb(225,214,245))
                setPadding(dp(4),0,dp(4),0)
                setOnClickListener {
                    if (composingWord.isNotEmpty() && !sensitiveField) {
                        currentInputConnection?.commitText("$value ",1); composingWord = ""; refreshSuggestions()
                    } else if (index == 2) showTextTools()
                }
            },weight())
        }
    }
    private fun previousWord(): String = currentInputConnection?.getTextBeforeCursor(80,0)?.toString().orEmpty().trimEnd().substringAfterLast(' ').trim()

    private fun pasteClipboard() {
        val clip = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val text = if (clip.hasPrimaryClip()) clip.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString() else null
        if (!text.isNullOrEmpty()) {
            rememberClipboard(text)
            currentInputConnection?.commitText(text,1)
        } else Toast.makeText(this,"Clipboard is empty",Toast.LENGTH_SHORT).show()
    }

    private fun showEmojiPicker() {
        val groups = linkedMapOf(
            "Smileys" to listOf("😀","😃","😄","😁","😅","😂","🙂","😉","😊","😍","🥰","😎","😭","🥹","😡","🤔","🤗"),
            "Gestures" to listOf("👍","👎","👏","🙌","🙏","🤝","💪","👋","👌","✌️","🫶","❤️","💯","🔥","✨","🎉"),
            "Nature" to listOf("🐱","🐶","🦊","🐼","🐸","🦋","🌸","🌹","🌈","☀️","🌙","⭐","🍀","🌊"),
            "Food" to listOf("🍕","🍔","🍟","🍿","🍩","🍪","🍰","🍓","🍉","🍎","☕","🧋"),
            "Symbols" to listOf("❤️","💜","🖤","💙","💚","💛","🤍","💫","⚡","✅","❌","∞"))
        AlertDialog.Builder(this).setTitle("Choose emoji").setItems(groups.keys.toTypedArray()) { _, index ->
            val choices = groups.values.elementAt(index)
            AlertDialog.Builder(this).setTitle(groups.keys.elementAt(index)).setItems(choices.toTypedArray()) { _, item ->
                currentInputConnection?.commitText(choices[item],1)
            }.show()
        }.show()
    }

    private fun searchGifs() {
        val input = EditText(this).apply { hint = "Search GIFs" }
        AlertDialog.Builder(this).setTitle("GIF search")
            .setMessage("Opens Tenor in your browser; internet is required.")
            .setView(input).setNegativeButton("Cancel",null).setPositiveButton("Search") { _,_ ->
                val query = input.text.toString().trim()
                if (query.isNotEmpty()) try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://tenor.com/search/" + Uri.encode(query) + "-gifs")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) { Toast.makeText(this,"No browser available",Toast.LENGTH_SHORT).show() }
            }.show()
    }

    private fun showTextTools() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)?.toString()
        if (selected.isNullOrEmpty()) {
            val options = arrayOf("Insert date","Insert time","Insert saved signature","Paste clipboard","Insert saved snippet","Manage snippets","Recent clipboard")
            AlertDialog.Builder(this).setTitle("Text tools").setItems(options) { _,which ->
                when(which) {
                    0 -> ic.commitText(java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.getDefault()).format(java.util.Date()),1)
                    1 -> ic.commitText(java.text.SimpleDateFormat("HH:mm",java.util.Locale.getDefault()).format(java.util.Date()),1)
                    2 -> ic.commitText(prefs.getString("signature","") ?: "",1)
                    3 -> pasteClipboard()
                    4 -> chooseSnippet()
                    5 -> addSnippet()
                    6 -> chooseRecentClipboard()
                }
            }.show()
            return
        }
        val options = arrayOf("UPPERCASE","lowercase","Title Case","Reverse text","Remove extra spaces","Copy transformed text","Fix common typos")
        AlertDialog.Builder(this).setTitle("Text tools").setItems(options) { _,which ->
            val transformed = when(which) {
                0 -> selected.uppercase()
                1 -> selected.lowercase()
                2 -> selected.split(Regex("\\s+")).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                3 -> selected.reversed()
                4 -> selected.trim().split(Regex("\\s+")).joinToString(" ")
                else -> selected.split(Regex("\\b")).joinToString("") { OfflinePredictionEngine.correct(it) }
            }
            if (which == 5) {
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Transformed text",transformed))
                Toast.makeText(this,"Copied",Toast.LENGTH_SHORT).show()
            } else ic.commitText(transformed,1)
        }.setNegativeButton("Cancel",null).show()
    }

    private fun savedSnippets(): MutableSet<String> =
        (prefs.getStringSet("snippets", emptySet()) ?: emptySet()).toMutableSet()

    private fun chooseSnippet() {
        val snippets = savedSnippets().toList().sorted()
        if (snippets.isEmpty()) {
            Toast.makeText(this, "No snippets saved yet. Use Manage snippets first.", Toast.LENGTH_LONG).show()
            return
        }
        val names = snippets.map { it.substringBefore("\u0000") }
        AlertDialog.Builder(this).setTitle("Insert snippet").setItems(names.toTypedArray()) { _, index ->
            currentInputConnection?.commitText(snippets[index].substringAfter("\u0000", ""), 1)
        }.show()
    }

    private fun addSnippet() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
        }
        val title = EditText(this).apply { hint = "Snippet name" }
        val body = EditText(this).apply { hint = "Text to insert"; minLines = 2; gravity = Gravity.TOP }
        layout.addView(title)
        layout.addView(body)
        AlertDialog.Builder(this).setTitle("Add snippet").setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val name = title.text.toString().trim()
                val value = body.text.toString()
                if (name.isNotEmpty() && value.isNotEmpty()) {
                    val snippets = savedSnippets()
                    snippets.removeAll { it.substringBefore("\u0000") == name }
                    snippets.add(name + "\u0000" + value)
                    prefs.edit().putStringSet("snippets", snippets).apply()
                    Toast.makeText(this, "Snippet saved", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun rememberClipboard(value: String) {
        if (value.isBlank() || sensitiveField) return
        if (recentClipboard.firstOrNull() != value) recentClipboard.add(0, value)
        while (recentClipboard.size > 20) recentClipboard.removeAt(recentClipboard.lastIndex)
        val json = JSONArray()
        recentClipboard.forEach { json.put(it) }
        prefs.edit().putString("clipboard_history_json", json.toString()).apply()
    }

    private fun rememberWord(value: String) {
        if (sensitiveField || !prefs.getBoolean("personalized_learning", true)) return
        val word = value.lowercase().filter { it.isLetter() || it == 39.toChar() || it == '-' }
        if (word.length < 2 || word.length > 40) return
        learnedWords.add(word)
        while (learnedWords.size > 2000) learnedWords.remove(learnedWords.first())
        prefs.edit().putStringSet("learned_words", learnedWords).apply()
    }

    private fun chooseRecentClipboard() {
        if (recentClipboard.isEmpty()) {
            Toast.makeText(this, "No clipboard items saved yet", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this).setTitle("Recent clipboard (saved on device)")
            .setItems(recentClipboard.map { it.replace("\n", " ").take(70) }.toTypedArray()) { _, index ->
                currentInputConnection?.commitText(recentClipboard[index], 1)
            }.show()
    }

    private fun key(label: String, special: Boolean) = Button(this).apply {
        text = label; textSize = if (label.length > 3) 10.5f else 18f; isAllCaps = false
        setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif-medium",Typeface.NORMAL)
        background = glass(if (special) Color.rgb(74,37,105) else keyColor)
        stateListAnimator = null; minimumWidth = 0; minWidth = 0; minimumHeight = 0; minHeight = 0
        setPadding(0,0,0,0); elevation = if (prefs.getBoolean("animations",true)) dp(2).toFloat() else 0f
    }
    private fun tool(label: String, description: String, action: () -> Unit): View = TextView(this).apply {
        text = label; contentDescription = description; textSize = if (label == "GIF") 12f else 19f
        gravity = Gravity.CENTER; setTextColor(Color.WHITE)
        background = if (prefs.getBoolean("animations", true)) ShineGlassDrawable(Color.rgb(37,22,58), themeAccent, dp(12).toFloat()) else glass(Color.rgb(37,22,58))
        setOnClickListener { action() }; layoutParams = weight()
    }
    private fun glass(color: Int) = GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(lighten(color),color)).apply {
        cornerRadius = dp(12).toFloat()
        setStroke(dp(1),Color.argb(75,Color.red(themeAccent),Color.green(themeAccent),Color.blue(themeAccent)))
    }
    private fun lighten(color: Int) = Color.rgb((Color.red(color)+18).coerceAtMost(255),(Color.green(color)+10).coerceAtMost(255),(Color.blue(color)+24).coerceAtMost(255))
    private fun weight() = LinearLayout.LayoutParams(0,-1,1f)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
