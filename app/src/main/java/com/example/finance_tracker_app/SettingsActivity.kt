package com.example.finance_tracker_app


import android.content.res.Configuration
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.core.content.edit


class SettingsActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }
    private val currencyNameResMap = mapOf(
        "USD" to R.string.currency_usd,
        "EUR" to R.string.currency_eur,
        "GBP" to R.string.currency_gbp,
        "JPY" to R.string.currency_jpy,
        "CNY" to R.string.currency_cny,
        "RUB" to R.string.currency_rub,
        "INR" to R.string.currency_inr,
        "AUD" to R.string.currency_aud,
        "CAD" to R.string.currency_cad,
        "CHF" to R.string.currency_chf,
        "TRY" to R.string.currency_try
    )
    private lateinit var spinnerCurrency: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_layout)
        spinnerCurrency = findViewById<Spinner>(R.id.currency_spinner)
        val backArrow = findViewById<ImageButton>(R.id.back_arrow)
        backArrow.setOnClickListener {
            val intent = Intent(this@SettingsActivity, DashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
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
        setupCurrencySpinner()
    }

    private fun setupCurrencySpinner() {
        val currencyCodes = currencyNameResMap.keys.toList()
        val currencyDisplayNames = currencyCodes.map { getString(currencyNameResMap[it]!!) }

        val adapter = object : ArrayAdapter<String>(
            this,
            R.layout.spinner_item,
            currencyDisplayNames
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.spinner_item, parent, false)
                val textView = view.findViewById<TextView>(R.id.spinnerText)
                textView.text = "Main currency: ${getItem(position)}"
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.spinner_dropdown_item, parent, false)
                val textView = view.findViewById<TextView>(R.id.dropdownText)
                textView.text = getItem(position)
                return view
            }
        }

        spinnerCurrency.adapter = adapter

        val savedCurrency = prefs.getString("main_currency", "USD") ?: "USD"
        val selectedIndex = currencyCodes.indexOf(savedCurrency)
        if (selectedIndex >= 0) {
            spinnerCurrency.setSelection(selectedIndex)
        }

        spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCurrency = currencyCodes[position]
                prefs.edit().putString("main_currency", selectedCurrency).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

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
