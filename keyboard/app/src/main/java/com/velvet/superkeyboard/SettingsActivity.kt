package com.velvet.superkeyboard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 32, 24, 24)
            setBackgroundColor(Color.rgb(7, 5, 17))
        }
        val title = TextView(this).apply {
            text = "SUPER KEYBOARD"
            textSize = 25f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(Color.rgb(210, 155, 255))
            gravity = Gravity.CENTER
        }
        root.addView(title, LinearLayout.LayoutParams(-1, 64))
        val description = TextView(this).apply {
            text = "A privacy-first, purple glass keyboard. Android requires you to enable and select third-party keyboards yourself. Typed text stays in the active editor; this starter build does not send it to a server."
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8, 12, 8, 20)
        }
        root.addView(description)
        root.addView(actionButton("1  Enable Super Keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }, LinearLayout.LayoutParams(-1, 56))
        root.addView(actionButton("2  Choose Super Keyboard") {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }, LinearLayout.LayoutParams(-1, 56))
        val note = TextView(this).apply {
            text = "\nIncluded now: native typing, shift, delete, enter, symbols, emoji insertion, clipboard paste, and theme styling. AI rewriting, multilingual prediction, theme editor, and custom layouts need later development."
            textSize = 14f
            setTextColor(Color.rgb(190, 177, 210))
        }
        root.addView(note)
        setContentView(root)
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { action() }
    }
}
