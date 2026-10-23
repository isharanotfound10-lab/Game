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
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("keyboard_prefs", MODE_PRIVATE) }
    private val purple = Color.rgb(210, 155, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.rgb(7, 5, 17))
        }
        root.addView(TextView(this).apply {
            text = "SUPER KEYBOARD"
            textSize = 25f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(purple)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, 64))

        root.addView(TextView(this).apply {
            text = "Personalize your keyboard"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, 36))

        root.addView(actionButton("🎨  Theme: ${prefs.getString("theme", "Purple")}") { chooseTheme() },
            LinearLayout.LayoutParams(-1, 54))

        val animationSwitch = Switch(this).apply {
            text = "Smooth animations / effects"
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("animations", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("animations", checked).apply()
                Toast.makeText(this@SettingsActivity, if (checked) "Effects enabled" else "Effects reduced", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(animationSwitch, LinearLayout.LayoutParams(-1, 56))

        root.addView(actionButton("1  Enable Super Keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }, LinearLayout.LayoutParams(-1, 54))
        root.addView(actionButton("2  Choose Super Keyboard") {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }, LinearLayout.LayoutParams(-1, 54))

        root.addView(TextView(this).apply {
            text = "\nINCLUDED IN THIS BUILD\n• Native typing, Shift, delete, enter and language picker\n• Number/symbol layouts and Unicode symbol panel\n• Emoji insertion and clipboard paste\n• Selected-text tools: case conversion, reverse, whitespace cleanup\n• Theme choices and effects toggle\n\nPrivacy: this starter keyboard does not upload typed text. AI rewriting, predictive dictionaries, swipe typing, GIF search, custom layout builder, and other advanced features are not implemented yet."
            textSize = 14f
            setTextColor(Color.rgb(190, 177, 210))
            setPadding(4, 16, 4, 16)
        })
        setContentView(root)
    }

    private fun chooseTheme() {
        val themes = arrayOf("Purple", "Cyber Pink", "Ocean", "Emerald", "AMOLED")
        val current = prefs.getString("theme", "Purple")
        AlertDialog.Builder(this)
            .setTitle("Choose a theme")
            .setSingleChoiceItems(themes, themes.indexOf(current)) { dialog, which ->
                prefs.edit().putString("theme", themes[which]).apply()
                dialog.dismiss()
                render()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { action() }
    }
}
