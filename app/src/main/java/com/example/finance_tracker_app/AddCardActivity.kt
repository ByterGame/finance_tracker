package com.example.finance_tracker_app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.content.SharedPreferences
import android.text.Editable
import android.text.TextWatcher
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context
import com.example.finance_tracker_app.utils.CardStorage


class AddCardActivity : AppCompatActivity() {

    data class Card(val name: String, val balance: Double, val currency: String?)


    private lateinit var spinner: Spinner
    private lateinit var nameInput: EditText
    private lateinit var addButton: Button
    private lateinit var balanceInput: EditText
    private lateinit var back_arrow: ImageButton


    private val gson = Gson()
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
    private val currencySymbolMap = mapOf(
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "JPY" to "¥",
        "CNY" to "¥",
        "RUB" to "₽",
        "INR" to "₹",
        "AUD" to "A$",
        "CAD" to "C$",
        "CHF" to "₣",
        "TRY" to "₺"
    )

    private var selectedCurrency = "USD"

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_theme", false)
        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_card_layout)

        spinner = findViewById(R.id.cardCurrencySpinner)
        nameInput = findViewById(R.id.name_card_input)
        addButton = findViewById(R.id.add_bank_account)
        balanceInput = findViewById(R.id.card_balance)
        back_arrow = findViewById<ImageButton>(R.id.back_arrow)

        back_arrow.setOnClickListener {
            startActivity(Intent(this@AddCardActivity, DashboardActivity::class.java))
        }

        setupBalanceFormatting()
        setupCurrencySpinner()
        setupAddButton()
    }

    private fun setupBalanceFormatting() {
        balanceInput.addTextChangedListener(object : TextWatcher {
            private var editing = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (editing || s == null) return

                editing = true

                try {
                    val original = s.toString()

                    val initialCursorPosition = balanceInput.selectionStart

                    val cleanString = original.replace("[^\\d.]".toRegex(), "")
                    val parsed = cleanString.toDoubleOrNull()

                    if (parsed != null) {
                        val formatter = DecimalFormat("#,###.##", DecimalFormatSymbols(Locale.US))
                        val formatted = formatter.format(parsed)

                        val digitsBeforeCursor = original.substring(0, initialCursorPosition).count { it.isDigit() }

                        balanceInput.setText(formatted)

                        var newCursorPos = 0
                        var digitsSeen = 0
                        while (newCursorPos < formatted.length && digitsSeen < digitsBeforeCursor) {
                            if (formatted[newCursorPos].isDigit()) {
                                digitsSeen++
                            }
                            newCursorPos++
                        }

                        balanceInput.setSelection(newCursorPos.coerceAtMost(formatted.length))
                    } else {
                        balanceInput.setText("")
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

                editing = false
            }
        })
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
                textView.text = "Currency: ${getItem(position)}"
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

        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                selectedCurrency = currencyCodes[position]
                val currencySymbol = currencySymbolMap[selectedCurrency] ?: selectedCurrency
                val currencyTextView = findViewById<TextView>(R.id.currency)
                currencyTextView.text = currencySymbol
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }


    private fun setupAddButton() {
        addButton.setOnClickListener {
            val name = nameInput.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Card name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val balanceString = balanceInput.text.toString().replace(",", "")
            val balance = balanceString.toDoubleOrNull() ?: 0.0

            val savedCards = CardStorage.loadSavedCards(this).toMutableList()

            val newCard = Card(
                name = name,
                balance = balance,
                currency = selectedCurrency
            )

            savedCards.add(newCard)
            CardStorage.saveCards(this, savedCards)

            Toast.makeText(this, "Card saved", Toast.LENGTH_SHORT).show()
            nameInput.text.clear()

            startActivity(Intent(this@AddCardActivity, DashboardActivity::class.java))
            finish()
        }
    }

}
