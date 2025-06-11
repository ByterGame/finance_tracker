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


class AddCardActivity : AppCompatActivity() {

    data class Card(val type: String, val name: String, val balance: Double)


    private lateinit var spinner: Spinner
    private lateinit var nameInput: EditText
    private lateinit var addButton: Button
    private lateinit var balanceInput: EditText
    private lateinit var back_arrow: ImageButton


    private val gson = Gson()
    private val cardTypes = listOf("Debit card", "Savings account", "Brokerage account")
    private var selectedCardType = cardTypes[0]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_card_layout)

        spinner = findViewById(R.id.cardTypeSpinner)
        nameInput = findViewById(R.id.name_card_input)
        addButton = findViewById(R.id.add_bank_account)
        balanceInput = findViewById(R.id.card_balance)
        back_arrow = findViewById<ImageButton>(R.id.back_arrow)

        back_arrow.setOnClickListener {
            startActivity(Intent(this@AddCardActivity, DashboardActivity::class.java))
        }

        setupBalanceFormatting()
        setupSpinner()
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


    private fun setupSpinner() {
        val adapter = object : ArrayAdapter<String>(
            this,
            R.layout.spinner_item,
            cardTypes
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.spinner_item, parent, false)
                val textView = view.findViewById<TextView>(R.id.spinnerText)
                textView.text = "Type: ${getItem(position)}"
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
                selectedCardType = cardTypes[position]
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

            val fullCardName = nameInput.text.toString().trim()
            val balanceString = balanceInput.text.toString().replace(",", "")
            val balance = balanceString.toDoubleOrNull() ?: 0.0
            val savedCards = loadSavedCards().toMutableList()
            savedCards.add(Card(type = selectedCardType, name = fullCardName, balance = balance))
            saveCards(savedCards)

            Toast.makeText(this, "Card saved", Toast.LENGTH_SHORT).show()
            nameInput.text.clear()
        }
    }

    private fun getEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            applicationContext,
            "cards_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun saveCards(cards: List<Card>) {
        val json = gson.toJson(cards)
        getEncryptedPrefs().edit().putString("cards_list", json).apply()
        startActivity(Intent(this@AddCardActivity, DashboardActivity::class.java))
        finish()
    }

    private fun loadSavedCards(): List<Card> {
        val json = getEncryptedPrefs().getString("cards_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<Card>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
}
