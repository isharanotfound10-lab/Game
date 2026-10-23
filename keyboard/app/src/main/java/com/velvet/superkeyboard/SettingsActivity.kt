package com.velvet.superkeyboard

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("keyboard_prefs", MODE_PRIVATE) }
    private val purple = Color.rgb(210,155,255)

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18),dp(16),dp(18),dp(24)); setBackgroundColor(Color.rgb(7,5,17))
        }
        root.addView(TextView(this).apply {
            text = "SUPER KEYBOARD"; textSize = 25f; typeface = Typeface.create("sans-serif",Typeface.BOLD)
            setTextColor(purple); gravity = Gravity.CENTER
        },LinearLayout.LayoutParams(-1,dp(64)))
        root.addView(TextView(this).apply {
            text = "Personalize your keyboard"; textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        },LinearLayout.LayoutParams(-1,dp(36)))
        root.addView(actionButton("🎨  Theme: ${prefs.getString("theme","Purple")}") { chooseTheme() },buttonParams(54))
        root.addView(actionButton("⌨  Layout: ${prefs.getString("layout","QWERTY")}") { chooseLayout() },buttonParams(50))
        root.addView(actionButton("↕  Key size: ${prefs.getString("key_size","Normal")}") { chooseKeySize() },buttonParams(50))
        root.addView(actionButton("🧩  Keyboard appearance / key shape") { chooseKeyAppearance() },buttonParams(50))
        root.addView(actionButton("🌐  Typing language: ${prefs.getString("dictionary_language","English")}") { chooseDictionaryLanguage() },buttonParams(50))
        root.addView(actionButton("🛠  Custom keyboard rows") { editCustomLayout() },buttonParams(50))
        root.addView(actionButton("🎨  Customize individual key") { customizeIndividualKey() },buttonParams(50))
        addSwitch(root,"Smooth animations / effects","animations",true)
        addSwitch(root,"Show number row","number_row",true)
        addSwitch(root,"Auto-correct common typos","auto_correct",true)
        addSwitch(root,"Learn words locally as you type","personalized_learning",true)
        addSwitch(root,"Automatic sentence capitalization","auto_capitalize",true)
        addSwitch(root,"Double-space inserts a period","double_space_period",true)
        addSwitch(root,"Key haptic feedback","key_haptics",false)
        root.addView(actionButton("✍  Set email signature") { editSignature() },buttonParams(50))
        root.addView(actionButton("🧠  Reset learned words") {
            AlertDialog.Builder(this).setTitle("Reset learned words?")
                .setMessage("This removes the keyboard's locally learned word list.")
                .setNegativeButton("Cancel",null).setPositiveButton("Reset") { _,_ ->
                    prefs.edit().remove("learned_words").apply()
                    Toast.makeText(this,"Learned words cleared",Toast.LENGTH_SHORT).show()
                }.show()
        },buttonParams(50))
        root.addView(actionButton("1  Enable Super Keyboard") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },buttonParams(54))
        root.addView(actionButton("2  Choose Super Keyboard") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        },buttonParams(54))
        root.addView(TextView(this).apply {
            text = """FEATURES IN THIS BUILD
• Native QWERTY typing, shift, delete, enter and system keyboard switching\n• Optional number row, symbols, Unicode panel, QWERTY/Dvorak/Colemak layouts and three key sizes\n• Offline word suggestions, common-typo correction and basic glide typing\n• Emoji picker, clipboard paste and external Tenor GIF search\n• Text tools: case conversion, reverse, whitespace cleanup, typo fixes, date/time, saved snippets, persistent on-device clipboard history, saved signature, and offline writing helpers (grammar cleanup, concise, professional/friendly tone, slight expansion)
• Horizontal glass-shine reflection on toolbar controls: 2100 ms travel, 1200 ms pause, 35% intensity; follows the smooth-effects toggle
• Optional local personalization learns up to 2000 words from non-password text\n• Five themes, smooth-effects toggle, auto-correct, auto-capitalization, double-space punctuation and optional haptics\n\nPRIVACY\nPredictions run locally. Suggestions and correction are disabled in detected password fields. GIF search opens your browser and sends the search query to Tenor. This keyboard does not upload typed text.\n\nLIMITATIONS\nThis build does not yet include a neural on-device AI model, advanced neural swipe prediction, GIF insertion directly into apps, cloud sync, full downloadable multilingual dictionaries, or a drag-and-drop visual keyboard designer. Writing helpers use lightweight rules, not a generative AI model. Key shape customization changes corner radius; per-key color/size editing is not included. Local word learning is lightweight dictionary personalization. Clipboard history is stored on this device; use the clear-history button on shared devices."""
            textSize = 14f; setTextColor(Color.rgb(190,177,210)); setPadding(dp(4),dp(16),dp(4),dp(16))
        })
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun addSwitch(root: LinearLayout, label: String, key: String, default: Boolean) {
        root.addView(Switch(this).apply {
            text = label; textSize = 15f; setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean(key,default)
            setOnCheckedChangeListener { _,checked ->
                prefs.edit().putBoolean(key,checked).apply()
                Toast.makeText(this@SettingsActivity,if(checked) "Enabled" else "Disabled",Toast.LENGTH_SHORT).show()
            }
        },buttonParams(50))
    }

    private fun chooseLayout() {
        val layouts = arrayOf("QWERTY","Dvorak","Colemak")
        AlertDialog.Builder(this).setTitle("Keyboard layout")
            .setSingleChoiceItems(layouts,layouts.indexOf(prefs.getString("layout","QWERTY"))) { dialog,which ->
                prefs.edit().putString("layout",layouts[which]).apply(); dialog.dismiss(); render()
            }.setNegativeButton("Cancel",null).show()
    }

    private fun chooseKeySize() {
        val sizes = arrayOf("Compact","Normal","Tall")
        AlertDialog.Builder(this).setTitle("Key size")
            .setSingleChoiceItems(sizes,sizes.indexOf(prefs.getString("key_size","Normal"))) { dialog,which ->
                prefs.edit().putString("key_size",sizes[which]).apply(); dialog.dismiss(); render()
            }.setNegativeButton("Cancel",null).show()
    }

    private fun chooseKeyAppearance() {
        val labels = arrayOf("Square (4)", "Soft (8)", "Glass (12)", "Round (18)", "Pill (26)")
        val values = intArrayOf(4, 8, 12, 18, 26)
        val current = prefs.getInt("custom_key_radius", 12)
        val selected = values.indices.minByOrNull { kotlin.math.abs(values[it] - current) } ?: 2
        AlertDialog.Builder(this).setTitle("Key shape")
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                prefs.edit().putInt("custom_key_radius", values[which]).apply()
                dialog.dismiss()
                Toast.makeText(this, "Key shape saved. Reopen the keyboard to apply.", Toast.LENGTH_SHORT).show()
                render()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun chooseDictionaryLanguage() {
        val languages = arrayOf("English", "Sinhala", "Tamil")
        val selected = languages.indexOf(prefs.getString("dictionary_language", "English")).coerceAtLeast(0)
        AlertDialog.Builder(this).setTitle("Typing language and offline dictionary")
            .setMessage("Includes small built-in word lists for suggestions. This is not a full spell-check dictionary.")
            .setSingleChoiceItems(languages, selected) { dialog, which ->
                prefs.edit().putString("dictionary_language", languages[which]).remove("custom_layout_rows").apply()
                dialog.dismiss()
                render()
                Toast.makeText(this, "Language saved. Reopen the keyboard to apply.", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun editCustomLayout() {
        val defaultRows = when (prefs.getString("dictionary_language", "English")) {
            "Sinhala" -> "අආඇඉඊඋඑඔකග\nචජටඩතදනපබ\nමයරලවසහළෆ"
            "Tamil" -> "அஆஇஈஉஊஎஏகங\nசஞடணதநபமய\nரலவழளறனஜஹ"
            else -> "qwertyuiop\nasdfghjkl\nzxcvbnm"
        }
        val input = EditText(this).apply {
            hint = "One key per character; three rows"
            setText(prefs.getString("custom_layout_rows", defaultRows))
            minLines = 3
            gravity = Gravity.TOP
            isSingleLine = false
        }
        AlertDialog.Builder(this).setTitle("Custom keyboard rows")
            .setMessage("Edit three lines. Each character becomes one key. Use letters only; special keys remain on the bottom row. Leave blank to restore the selected language's default layout.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Reset") { _, _ ->
                prefs.edit().remove("custom_layout_rows").apply()
                Toast.makeText(this, "Default layout restored", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Save") { _, _ ->
                val rows = input.text.toString().trim().lines()
                if (rows.size == 3 && rows.all { it.isNotBlank() && it.codePointCount(0, it.length) in 5..12 }) {
                    prefs.edit().putString("custom_layout_rows", rows.joinToString("\n")).apply()
                    Toast.makeText(this, "Custom layout saved. Reopen the keyboard.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Enter exactly three non-empty rows, each with 5–12 characters.", Toast.LENGTH_LONG).show()
                }
            }.show()
    }

    private fun customizeIndividualKey() {
        val keyInput = EditText(this).apply {
            hint = "Key character (for example: a, SPACE, or ?123)"
            setText("a")
            setSingleLine(true)
        }
        AlertDialog.Builder(this).setTitle("Choose a key")
            .setMessage("Set a color and width for one key. Use SPACE for the spacebar.")
            .setView(keyInput)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue") { _, _ ->
                val key = keyInput.text.toString().trim()
                if (key.isEmpty() || key.length > 8) {
                    Toast.makeText(this, "Enter one key label (up to 8 characters).", Toast.LENGTH_SHORT).show()
                } else chooseIndividualKeyColor(key)
            }.show()
    }

    private fun chooseIndividualKeyColor(key: String) {
        val colors = arrayOf("Default theme", "Purple", "Pink", "Blue", "Green", "Amber")
        val values = arrayOf("", "#733CCB", "#C52F94", "#167DB3", "#16835B", "#9B6514")
        val saved = prefs.getString("key_color_$key", "") ?: ""
        val selected = values.indexOf(saved).coerceAtLeast(0)
        AlertDialog.Builder(this).setTitle("Color for '$key'")
            .setSingleChoiceItems(colors, selected) { dialog, which ->
                if (values[which].isBlank()) prefs.edit().remove("key_color_$key").apply()
                else prefs.edit().putString("key_color_$key", values[which]).apply()
                dialog.dismiss()
                chooseIndividualKeyWidth(key)
            }.setNegativeButton("Cancel", null).show()
    }

    private fun chooseIndividualKeyWidth(key: String) {
        val labels = arrayOf("Narrow (0.75×)", "Normal (1×)", "Wide (1.25×)", "Extra wide (1.5×)")
        val values = floatArrayOf(0.75f, 1f, 1.25f, 1.5f)
        val current = prefs.getFloat("key_width_$key", 1f)
        val selected = values.indices.minByOrNull { kotlin.math.abs(values[it] - current) } ?: 1
        AlertDialog.Builder(this).setTitle("Width for '$key'")
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                if (values[which] == 1f) prefs.edit().remove("key_width_$key").apply()
                else prefs.edit().putFloat("key_width_$key", values[which]).apply()
                dialog.dismiss()
                Toast.makeText(this, "Key style saved. Reopen the keyboard to apply.", Toast.LENGTH_SHORT).show()
                render()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun editSignature() {
        val input = EditText(this).apply { setText(prefs.getString("signature","") ?: ""); hint = "Sent from my phone" }
        AlertDialog.Builder(this).setTitle("Email signature").setMessage("This text is inserted by Text tools.")
            .setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save") { _,_ ->
                prefs.edit().putString("signature",input.text.toString()).apply()
            }.show()
    }

    private fun chooseTheme() {
        val themes = arrayOf("Purple","Cyber Pink","Ocean","Emerald","AMOLED")
        AlertDialog.Builder(this).setTitle("Choose a theme")
            .setSingleChoiceItems(themes,themes.indexOf(prefs.getString("theme","Purple"))) { dialog,which ->
                prefs.edit().putString("theme",themes[which]).apply(); dialog.dismiss(); render()
            }.setNegativeButton("Cancel",null).show()
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        minHeight = dp(48); minimumHeight = dp(48)
        minimumWidth = 0; minWidth = 0
        setPadding(dp(12), dp(8), dp(12), dp(8))
        background = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(62,38,91), Color.rgb(27,17,43))
        ).apply {
            cornerRadius = dp(14).toFloat()
            setStroke(dp(1), Color.argb(130, 160, 80, 255))
        }
        stateListAnimator = null
        setOnClickListener { action() }
    }

    private fun buttonParams(heightDp: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, dp(heightDp)).apply {
            setMargins(0, dp(3), 0, dp(3))
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
