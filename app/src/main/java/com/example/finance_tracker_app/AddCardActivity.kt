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
import android.text.Editable
import android.text.TextWatcher
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.finance_tracker_app.MainActivity
import com.example.finance_tracker_app.utils.CardStorage
import okhttp3.ResponseBody
import retrofit2.Call
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch



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

            val email = getUserEmail(this@AddCardActivity)

            val cardRequest = CardRequest(
                userEmail = email.toString(),
                name = newCard.name,
                balance = newCard.balance,
                currency = newCard.currency.toString()
            )
            RetrofitClient.instance.saveCard(cardRequest)
                .enqueue(object : retrofit2.Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            Log.d("AddCard", "Card synced to backend")
                            lifecycleScope.launch {
                                retryPendingCards(this@AddCardActivity)
                            }
                        } else {
                            Log.e("AddCard", "Failed to sync card: ${response.code()}")
                            CoroutineScope(Dispatchers.IO).launch {
                                val gson = Gson()
                                val json = gson.toJson(cardRequest)
                                val db = AppDatabase.getDatabase(this@AddCardActivity)
                                db.pendingCardDao().insert(PendingCard(cardJson = json))
                                Log.d("AddCard", "Saved card locally for retry")
                            }
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("AddCard", "Network error syncing operation", t)
                        CoroutineScope(Dispatchers.IO).launch {
                            val gson = Gson()
                            val json = gson.toJson(cardRequest)
                            val db = AppDatabase.getDatabase(this@AddCardActivity)
                            db.pendingCardDao().insert(PendingCard(cardJson = json))
                            Log.d("AddCard", "Saved card locally for retry")
                        }
                    }
                })

            Toast.makeText(this, "Card saved", Toast.LENGTH_SHORT).show()
            nameInput.text.clear()

            startActivity(Intent(this@AddCardActivity, DashboardActivity::class.java))
            finish()
        }
    }

    suspend fun retryPendingCards(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val pendingCardDao = db.pendingCardDao()
        val gson = Gson()

        val list = pendingCardDao.getAll()
        if (list.isEmpty()) {
            Log.d("RetryCards", "No pending cards to sync")
            return
        }

        for (pending in list) {
            val cardRequest = gson.fromJson(pending.cardJson, CardRequest::class.java)

            RetrofitClient.instance.saveCard(cardRequest)
                .enqueue(object : retrofit2.Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            lifecycleScope.launch {
                                pendingCardDao.delete(pending)
                                Log.d("RetryCards", "Card synced successfully and deleted locally")
                            }
                        } else {
                            Log.e("RetryCards", "Failed to sync card: ${response.code()}")
                        }
                    }
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("RetryCards", "Network error syncing card", t)
                    }
                })
        }
    }

    fun getUserEmail(context: Context): String? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                context,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPreferences.getString("user_email", null)
        } catch (e: Exception) {
            e.printStackTrace()
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("user_email", null)
        }
    }
}
