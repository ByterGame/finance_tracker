//package com.example.finance_tracker_app
//
//import android.content.Context
//import android.content.res.Configuration
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import androidx.appcompat.app.AppCompatDelegate
//import android.widget.Button
//
//class switch_theme : AppCompatActivity() {
//
//    private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        loadTheme()
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.enter_pin_code_layout)
//
//        val switchThemeButton = findViewById<Button>(R.id.switch_theme_button)
//        switchThemeButton.setOnClickListener {
//            toggleTheme()
//        }
//    }
//
//    private fun toggleTheme() {
//        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
//        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
//            saveTheme(false)
//        } else {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
//            saveTheme(true)
//        }
//    }
//
//    private fun saveTheme(isDark: Boolean) {
//        prefs.edit().putBoolean("dark_theme", isDark).apply()
//    }
//
//    private fun loadTheme() {
//        val isDark = prefs.getBoolean("dark_theme", false)
//        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
//        AppCompatDelegate.setDefaultNightMode(mode)
//    }
//}
