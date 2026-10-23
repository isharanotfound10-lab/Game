package com.velvet.superkeyboard

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class SuperKeyboardService : InputMethodService() {
    private var shifted = false
    private var symbols = false
    private var unicode = false
    private val bg: Int get() = if (prefs.getString("theme", "Purple") == "AMOLED") Color.BLACK else Color.rgb(7, 5, 17)
    private val keyColor: Int get() = when (prefs.getString("theme", "Purple")) {
        "Cyber Pink" -> Color.rgb(57, 17, 43)
        "Ocean" -> Color.rgb(13, 37, 54)
        "Emerald" -> Color.rgb(15, 43, 35)
        "AMOLED" -> Color.rgb(18, 18, 22)
        else -> Color.rgb(30, 22, 48)
    }
    private val themeAccent: Int get() = when (prefs.getString("theme", "Purple")) {
        "Cyber Pink" -> Color.rgb(236, 56, 188)
        "Ocean" -> Color.rgb(54, 190, 255)
        "Emerald" -> Color.rgb(65, 220, 155)
        "AMOLED" -> Color.rgb(190, 150, 255)
        else -> Color.rgb(160, 80, 255)
    }
    private val prefs by lazy { getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE) }

    override fun onCreateInputView(): View = buildKeyboard()

    private fun buildKeyboard(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(7), dp(6), dp(8))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(bg, if (prefs.getString("theme", "Purple") == "AMOLED") Color.BLACK else Color.rgb(20, 7, 38), Color.rgb(8, 5, 18))
            ).apply { cornerRadius = dp(18).toFloat() }
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        toolbar.addView(tool("Aa", "Text tools") { showTextTools() }, weight())
        toolbar.addView(tool("▤", "Paste clipboard") { pasteClipboard() }, weight())
        toolbar.addView(tool("☺", "Insert emoji") { currentInputConnection?.commitText("😀", 1) }, weight())
        toolbar.addView(tool("⌘", "Symbols") {
            symbols = !symbols
            unicode = false
            setInputView(buildKeyboard())
        }, weight())
        toolbar.addView(tool("Ω", "Unicode laboratory") {
            unicode = !unicode
            symbols = false
            setInputView(buildKeyboard())
        }, weight())
        toolbar.addView(tool("⚙", "Settings") {
            val intent = android.content.Intent(this, SettingsActivity::class.java)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }, weight())
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(42)))

        val suggestions = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }
        listOf("Aa", "Clipboard", "Unicode").forEach { value ->
            suggestions.addView(TextView(this).apply {
                text = value
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(225, 214, 245))
                setPadding(dp(8), 0, dp(8), 0)
            }, weight())
        }
        root.addView(suggestions, LinearLayout.LayoutParams(-1, dp(28)))

        val rows: List<List<String>> = when {
            unicode -> listOf(
                listOf("→","⇒","⇢","⟿","⤳","⤷","↔","↕","↗","↘"),
                listOf("∞","≠","≤","≥","±","÷","×","√","∑","π"),
                listOf("★","☆","♡","♥","◆","◇","●","○","▲","△"),
                listOf("꧁","༺","༻","𒐫","⌫","ABC","SPACE","↵")
            )
            symbols -> listOf(
                listOf("1","2","3","4","5","6","7","8","9","0"),
                listOf("@","#","$","%","&","-","+","(",")","/"),
                listOf("=","*","\"","'",":",";","!","?","⌫"),
                listOf("ABC","🌐",",","SPACE",".","↵")
            )
            else -> listOf(
                listOf("q","w","e","r","t","y","u","i","o","p"),
                listOf("a","s","d","f","g","h","j","k","l"),
                listOf("⇧","z","x","c","v","b","n","m","⌫"),
                listOf("?123","🌐",",","SPACE",".","↵")
            )
        }

        rows.forEachIndexed { index, row ->
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            row.forEach { key ->
                val label = when {
                    key == "SPACE" -> "English (US)"
                    key.length == 1 && shifted && key[0].isLetter() -> key.uppercase()
                    else -> key
                }
                val special = index == rows.lastIndex || key in listOf("⇧", "⌫", "?123", "ABC", "↵")
                val button = key(label, special)
                button.setOnClickListener { handleKey(key) }
                if (key == "⌫") button.setOnLongClickListener {
                    currentInputConnection?.deleteSurroundingText(1000, 0)
                    true
                }
                val lp = LinearLayout.LayoutParams(0, dp(if (index == rows.lastIndex) 47 else 44), 1f)
                lp.setMargins(dp(2), dp(3), dp(2), dp(3))
                if (key == "SPACE") lp.weight = 4.1f
                line.addView(button, lp)
            }
            root.addView(line, LinearLayout.LayoutParams(-1, dp(if (index == rows.lastIndex) 53 else 50)))
        }
        return root
    }

    private fun handleKey(key: String) {
        val ic = currentInputConnection ?: return
        when (key) {
            "⇧" -> { shifted = !shifted; setInputView(buildKeyboard()) }
            "⌫" -> {
                val selected = ic.getSelectedText(0)
                if (!selected.isNullOrEmpty()) ic.commitText("", 1)
                else ic.deleteSurroundingText(1, 0)
            }
            "SPACE" -> ic.commitText(" ", 1)
            "↵" -> {
                val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                if (action != null && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) ic.performEditorAction(action)
                else ic.commitText("\n", 1)
            }
            "🌐" -> switchToNextInputMethod(false)
            "?123" -> { symbols = true; unicode = false; setInputView(buildKeyboard()) }
            "ABC" -> { symbols = false; unicode = false; setInputView(buildKeyboard()) }
            else -> {
                ic.commitText(if (shifted && key.length == 1) key.uppercase() else key, 1)
                if (shifted) { shifted = false; setInputView(buildKeyboard()) }
            }
        }
    }

    private fun pasteClipboard() {
        val clip = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val text = if (clip.hasPrimaryClip()) clip.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString() else null
        if (!text.isNullOrEmpty()) currentInputConnection?.commitText(text, 1)
        else Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
    }

    private fun showTextTools() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)?.toString()
        if (selected.isNullOrEmpty()) {
            Toast.makeText(this, "Select text first, then open Aa for text tools.", Toast.LENGTH_SHORT).show()
            return
        }
        val options = arrayOf("UPPERCASE", "lowercase", "Title Case", "Reverse text", "Remove extra spaces", "Copy transformed text")
        AlertDialog.Builder(this)
            .setTitle("Text tools")
            .setItems(options) { _, which ->
                val transformed = when (which) {
                    0 -> selected.uppercase()
                    1 -> selected.lowercase()
                    2 -> selected.split(Regex("\\s+")).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    3 -> selected.reversed()
                    else -> selected.trim().split(Regex("\\s+")).joinToString(" ")
                }
                if (which == 5) {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Transformed text", transformed))
                    Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
                } else ic.commitText(transformed, 1)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun key(label: String, special: Boolean): Button = Button(this).apply {
        text = label
        textSize = if (label.length > 3) 11f else 18f
        isAllCaps = false
        setTextColor(Color.WHITE)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        background = glass(if (special) Color.rgb(74, 37, 105) else keyColor)
        stateListAnimator = null
        minimumWidth = 0
        minWidth = 0
        minimumHeight = 0
        minHeight = 0
        setPadding(0, 0, 0, 0)
        elevation = dp(1).toFloat()
    }

    private fun tool(label: String, description: String, action: () -> Unit): View = TextView(this).apply {
        text = label
        contentDescription = description
        textSize = 19f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        background = glass(Color.rgb(37, 22, 58))
        setOnClickListener { action() }
        layoutParams = weight()
    }

    private fun glass(color: Int) = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(lighten(color), color)
    ).apply {
        cornerRadius = dp(12).toFloat()
        setStroke(dp(1), Color.argb(75, Color.red(themeAccent), Color.green(themeAccent), Color.blue(themeAccent)))
    }

    private fun lighten(color: Int): Int = Color.rgb(
        (Color.red(color) + 18).coerceAtMost(255),
        (Color.green(color) + 10).coerceAtMost(255),
        (Color.blue(color) + 24).coerceAtMost(255)
    )

    private fun weight() = LinearLayout.LayoutParams(0, -1, 1f)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
