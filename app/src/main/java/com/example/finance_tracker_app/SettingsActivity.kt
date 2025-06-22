package com.example.finance_tracker_app


import android.content.res.Configuration
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context
import android.content.Intent
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.core.content.edit


class SettingsActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_layout)

//        val themedDrawable = ContextCompat.getDrawable(this, R.drawable.spinner_background)
//        findViewById<Button>(R.id.clear_data_button).background = themedDrawable

//        val clearButton = findViewById<Button>(R.id.clear_data_button)
//        clearButton.setBackgroundResource(R.drawable.spinner_background)
        val backArrow = findViewById<ImageButton>(R.id.back_arrow)
        backArrow.setOnClickListener {
            startActivity(Intent(this@SettingsActivity, DashboardActivity::class.java))
        }

        val clearDataButton = findViewById<Button>(R.id.clear_data_button)

        clearDataButton.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Confirm Deletion")
            builder.setMessage("Are you sure you want to delete all data without the possibility of recovery?")
            builder.setPositiveButton("Yes") { dialog, _ ->
                val prefsList = listOf(
                    "settings",
                    "dark_theme",
                    "cards_secure_prefs",
                    "user_categories",
                    "categories_list",
                    "cards_list",
                    "dashboard_prefs",
                    "secure_prefs",
                    "app_prefs",
                )

                for (name in prefsList) {
                    getSharedPreferences(name, Context.MODE_PRIVATE).edit() { clear() }
                }

                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(this@SettingsActivity)
                    db.operationDao().deleteAllOperations()

                    Toast.makeText(this@SettingsActivity, "Data cleared successfully", Toast.LENGTH_SHORT).show()

                    startActivity(Intent(this@SettingsActivity, Registration::class.java))
                    finish()
                }
                dialog.dismiss()
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

            builder.create().show()
        }

        setupSwitchTheme()
    }

    private fun setupSwitchTheme() {
        val switchThemeBtn = findViewById<ImageButton>(R.id.theme_check_box)
        val isDarkTheme = prefs.getBoolean("dark_theme", false)

        val initialDrawable = if (isDarkTheme) {
            R.drawable.check_box_true
        } else {
            R.drawable.check_box_false
        }
        switchThemeBtn.setBackgroundResource(initialDrawable)

        switchThemeBtn.setOnClickListener {
            val newIsDark = !prefs.getBoolean("dark_theme", false)
            val newDrawableRes = if (newIsDark) {
                R.drawable.check_box_true
            } else {
                R.drawable.check_box_false
            }
            switchThemeBtn.setBackgroundResource(newDrawableRes)

            toggleTheme(newIsDark)
        }
    }

    private fun toggleTheme(isDark: Boolean) {
        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
        saveTheme(isDark)

        recreate()
    }

    private fun saveTheme(isDark: Boolean) {
        prefs.edit().putBoolean("dark_theme", isDark).apply()
    }

    private fun applySavedTheme() {
        val isDark = prefs.getBoolean("dark_theme", false)
        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
