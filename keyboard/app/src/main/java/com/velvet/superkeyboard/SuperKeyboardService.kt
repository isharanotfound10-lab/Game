package com.velvet.superkeyboard

import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class SuperKeyboardService : InputMethodService() {
    private var shifted = false
    private var symbols = false
    private var compactTools = false
    private val bg = Color.rgb(7, 5, 17)
    private val keyColor = Color.rgb(30, 22, 48)
    private val accent = Color.rgb(160, 80, 255)
    private val pink = Color.rgb(236, 56, 188)

    override fun onCreateInputView(): View = buildKeyboard()

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
    }

    private fun buildKeyboard(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(7), dp(6), dp(8))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(bg, Color.rgb(20, 7, 38), Color.rgb(8, 5, 18))
            ).apply { cornerRadius = dp(18).toFloat() }
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        toolbar.addView(tool("✦", "AI tools") {
            Toast.makeText(this, "AI writing tools are planned; no text is sent anywhere.", Toast.LENGTH_SHORT).show()
        }, weight())
        toolbar.addView(tool("▤", "Clipboard") {
            val clip = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val text = if (clip.hasPrimaryClip()) clip.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString() else null
            if (!text.isNullOrEmpty()) currentInputConnection?.commitText(text, 1)
            else Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
        }, weight())
        toolbar.addView(tool("☺", "Emoji") { currentInputConnection?.commitText("😀", 1) }, weight())
        toolbar.addView(tool("⌘", "Symbols") {
            symbols = !symbols
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
        listOf("Super", "Keyboard", "✨").forEach { value ->
            val t = TextView(this).apply {
                text = value
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(225, 214, 245))
                setPadding(dp(10), 0, dp(10), 0)
            }
            suggestions.addView(t, weight())
        }
        root.addView(suggestions, LinearLayout.LayoutParams(-1, dp(30)))

        val rows = if (symbols) listOf(
            listOf("1","2","3","4","5","6","7","8","9","0"),
            listOf("@","#","$","%","&","-","+","(",")","/"),
            listOf("=","*","\"","'",":",";","!","?","⌫"),
            listOf("ABC","🌐",",","SPACE",".","↵")
        ) else listOf(
            listOf("q","w","e","r","t","y","u","i","o","p"),
            listOf("a","s","d","f","g","h","j","k","l"),
            listOf("⇧","z","x","c","v","b","n","m","⌫"),
            listOf("?123","🌐",",","SPACE",".","↵")
        )

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
                val button = key(label, index == 3 || key in listOf("⇧", "⌫", "?123", "ABC", "↵"))
                button.setOnClickListener { handleKey(key) }
                val lp = LinearLayout.LayoutParams(0, dp(if (index == 3) 47 else 44), 1f)
                lp.setMargins(dp(2), dp(3), dp(2), dp(3))
                if (key == "SPACE") lp.weight = 4.1f
                line.addView(button, lp)
            }
            root.addView(line, LinearLayout.LayoutParams(-1, dp(if (index == 3) 53 else 50)))
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
            "?123" -> { symbols = true; setInputView(buildKeyboard()) }
            "ABC" -> { symbols = false; setInputView(buildKeyboard()) }
            else -> {
                val out = if (shifted && key.length == 1) key.uppercase() else key
                ic.commitText(out, 1)
                if (shifted) { shifted = false; setInputView(buildKeyboard()) }
            }
        }
    }

    private fun key(label: String, special: Boolean): Button = Button(this).apply {
        text = label
        textSize = if (label.length > 3) 12f else 18f
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
        textSize = 21f
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
        setStroke(dp(1), Color.argb(75, 205, 160, 255))
    }

    private fun lighten(color: Int): Int = Color.rgb(
        (Color.red(color) + 18).coerceAtMost(255),
        (Color.green(color) + 10).coerceAtMost(255),
        (Color.blue(color) + 24).coerceAtMost(255)
    )

    private fun weight() = LinearLayout.LayoutParams(0, -1, 1f)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
