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
            setPadding(24,24,24,24); setBackgroundColor(Color.rgb(7,5,17))
        }
        root.addView(TextView(this).apply {
            text = "SUPER KEYBOARD"; textSize = 25f; typeface = Typeface.create("sans-serif",Typeface.BOLD)
            setTextColor(purple); gravity = Gravity.CENTER
        },LinearLayout.LayoutParams(-1,64))
        root.addView(TextView(this).apply {
            text = "Personalize your keyboard"; textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        },LinearLayout.LayoutParams(-1,36))
        root.addView(actionButton("🎨  Theme: ${prefs.getString("theme","Purple")}") { chooseTheme() },LinearLayout.LayoutParams(-1,54))
        root.addView(actionButton("⌨  Layout: ${prefs.getString("layout","QWERTY")}") { chooseLayout() },LinearLayout.LayoutParams(-1,50))
        root.addView(actionButton("↕  Key size: ${prefs.getString("key_size","Normal")}") { chooseKeySize() },LinearLayout.LayoutParams(-1,50))
        addSwitch(root,"Smooth animations / effects","animations",true)
        addSwitch(root,"Show number row","number_row",true)
        addSwitch(root,"Auto-correct common typos","auto_correct",true)
        addSwitch(root,"Automatic sentence capitalization","auto_capitalize",true)
        addSwitch(root,"Double-space inserts a period","double_space_period",true)
        addSwitch(root,"Key haptic feedback","key_haptics",false)
        root.addView(actionButton("✍  Set email signature") { editSignature() },LinearLayout.LayoutParams(-1,50))
        root.addView(actionButton("1  Enable Super Keyboard") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },LinearLayout.LayoutParams(-1,54))
        root.addView(actionButton("2  Choose Super Keyboard") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        },LinearLayout.LayoutParams(-1,54))
        root.addView(TextView(this).apply {
            text = "\nFEATURES IN THIS BUILD\n• Native QWERTY typing, shift, delete, enter and system keyboard switching\n• Optional number row, symbols, Unicode panel, QWERTY/Dvorak/Colemak layouts and three key sizes\n• Offline word suggestions, common-typo correction and basic glide typing\n• Emoji picker, clipboard paste and external Tenor GIF search\n• Text tools: case conversion, reverse, whitespace cleanup, typo fixes, date/time, saved snippets, session-only clipboard history and saved signature\n• Five themes, smooth-effects toggle, auto-correct, auto-capitalization, double-space punctuation and optional haptics\n\nPRIVACY\nPredictions run locally. Suggestions and correction are disabled in detected password fields. GIF search opens your browser and sends the search query to Tenor. This keyboard does not upload typed text.\n\nLIMITATIONS\nThis build does not yet include a neural on-device AI model, advanced personalized swipe prediction, GIF insertion directly into apps, cloud sync, full multilingual dictionaries, or a visual custom-keyboard designer."
            textSize = 14f; setTextColor(Color.rgb(190,177,210)); setPadding(4,16,4,16)
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
        },LinearLayout.LayoutParams(-1,50))
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
        text = label; isAllCaps = false; setOnClickListener { action() }
    }
}
