package com.example.finance_tracker_app.activities

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.finance_tracker_app.data.db.AppDatabase
import com.example.finance_tracker_app.data.db.Operation
import com.example.finance_tracker_app.data.db.OperationDao
import com.example.finance_tracker_app.data.api.OperationRequest
import com.example.finance_tracker_app.data.db.PendingOperation
import com.example.finance_tracker_app.R
import com.example.finance_tracker_app.data.api.RetrofitClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import kotlin.collections.get

class AddOperationActivity : AppCompatActivity() {

    private lateinit var transactionTypeSpinner: Spinner
    private lateinit var selectAccountSpinner: Spinner
    private lateinit var selectCategorySpinner: Spinner
    private lateinit var dateInput: EditText
    private lateinit var backArrow: ImageButton
    private lateinit var addOperation: Button
    private lateinit var currencySymbolView: TextView

    private val gson = Gson()
    private var cardsList: List<AddCardActivity.Card> = emptyList()
    private var userCategories: MutableList<String> = mutableListOf()
    private lateinit var db: AppDatabase
    private lateinit var operationDao: OperationDao
    private val pendingOperationDao by lazy {
        AppDatabase.Companion.getDatabase(this).pendingOperationDao()
    }

    private var selectedDateMillis: Long = System.currentTimeMillis()

    private val addCategoryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val newCategory = result.data?.getStringExtra("new_category_name")
            if (!newCategory.isNullOrBlank()) {
                userCategories.add(newCategory)
                saveUserCategories()
                updateCategorySpinnerWithNewCategory(newCategory)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_theme", false)
        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_operation_layout)

        dateInput = findViewById(R.id.date_input)
        transactionTypeSpinner = findViewById(R.id.transaction_type_spinner)
        selectAccountSpinner = findViewById(R.id.select_account_spinner)
        selectCategorySpinner = findViewById(R.id.select_category_spinner)
        backArrow = findViewById(R.id.back_arrow)
        addOperation = findViewById(R.id.add_operation)
        currencySymbolView = findViewById(R.id.currency)


        db = AppDatabase.Companion.getDatabase(this)
        operationDao = db.operationDao()


        setupAddOperation()
        setupBackArrow()
        setupDateInput()
        setupTransactionTypeSpinner()
        loadCards()
        setupSelectAccountSpinner()
        loadUserCategories()
        updateCategorySpinnerWithNewCategory(null)
    }

    private fun setupAddOperation() {
        addOperation.setOnClickListener {
            saveOperation()
        }
    }

    private fun saveOperation() {
        val type = transactionTypeSpinner.selectedItem?.toString() ?: ""
        val amountStr = findViewById<EditText>(R.id.transaction_amount).text.toString()
        val accountName = selectAccountSpinner.selectedItem?.toString() ?: ""
        val category = selectCategorySpinner.selectedItem?.toString() ?: ""
        val note = findViewById<EditText>(R.id.note_input).text.toString()

        val amount = amountStr.replace(",", "").toDoubleOrNull()
        if (type.isEmpty() || amount == null || accountName.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val cards = loadSavedCards().toMutableList()
        val cardIndex = cards.indexOfFirst { it.name == accountName }

        if (cardIndex == -1) {
            Toast.makeText(this, "Selected card not found", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedCard = cards[cardIndex]
        val newBalance = when (type.lowercase()) {
            "income" -> selectedCard.balance + amount
            "expense" -> selectedCard.balance - amount
            else -> selectedCard.balance
        }

        fun proceedSaving() {

            val operation = Operation(
                type = type,
                amount = amount,
                accountName = accountName,
                currency = selectedCard.currency.toString(),
                category = category,
                date = selectedDateMillis,
                note = if (note.isBlank()) null else note
            )

            cards[cardIndex] = selectedCard.copy(balance = newBalance)
            saveCards(cards)

            lifecycleScope.launch {
                try {
                    operationDao.insertOperation(operation)
                    val email = getUserEmail(this@AddOperationActivity)

                    val operationRequest = OperationRequest(
                        userEmail = email.toString(),
                        type = type,
                        amount = amount,
                        accountName = accountName,
                        currency = selectedCard.currency.toString(),
                        category = category,
                        date = selectedDateMillis,
                        note = if (note.isBlank()) null else note
                    )
                    RetrofitClient.instance.saveOperation(operationRequest)
                        .enqueue(object : Callback<ResponseBody> {
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                if (response.isSuccessful) {
                                    Log.d("AddOperation", "Operation synced to backend")
                                    lifecycleScope.launch {
                                        retryPendingOperations(this@AddOperationActivity)
                                    }
                                } else {
                                    Log.e("AddOperation", "Failed to sync operation: ${response.code()}")
                                    Log.d("check json", "гыгыгы")
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val gson = Gson()
                                        val json = gson.toJson(operationRequest)
                                        Log.d("check json", json)
                                        pendingOperationDao.insert(PendingOperation(operationJson = json))
                                    }
                                }
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                Log.e("AddOperation", "Network error syncing operation", t)
                                Log.d("check json", "гыгыгы")
                                CoroutineScope(Dispatchers.IO).launch {
                                    val gson = Gson()
                                    val json = gson.toJson(operationRequest)
                                    Log.d("check json", json)
                                    pendingOperationDao.insert(PendingOperation(operationJson = json))
                                }
                            }
                        })
                    runOnUiThread {
                        Toast.makeText(this@AddOperationActivity, "Operation saved", Toast.LENGTH_SHORT).show()
                        startActivity(
                            Intent(
                                this@AddOperationActivity,
                                DashboardActivity::class.java
                            )
                        )
                        finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this@AddOperationActivity, "Error saving operation", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        if (type.lowercase() == "expense" && newBalance < 0) {
            AlertDialog.Builder(this)
                .setTitle("Negative Balance")
                .setMessage("This expense will result in a negative balance on this account. Are you sure you want to continue?")
                .setPositiveButton("Yes") { _, _ -> proceedSaving() }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            proceedSaving()
        }
    }

    suspend fun retryPendingOperations(context: Context) {
        val dao = AppDatabase.Companion.getDatabase(context).pendingOperationDao()
        val list = dao.getAll()

        if (list.isEmpty()) {
            Log.d("RetryOperation", "No pending operations to sync")
            return
        }

        val gson = Gson()

        list.forEach { pending ->
            val request = gson.fromJson(pending.operationJson, OperationRequest::class.java)

            RetrofitClient.instance.saveOperation(request)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            lifecycleScope.launch {
                                dao.delete(pending)
                                Log.d("RetryOperation", "Successfully synced pending operation")
                            }
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("RetryOperation", "Still failed to sync pending operation", t)
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
            context.getSharedPreferences("app_prefs", MODE_PRIVATE).getString("user_email", null)
        }
    }

    private fun saveCards(cards: List<AddCardActivity.Card>) {
        val json = gson.toJson(cards)
        getEncryptedPrefs().edit().putString("cards_list", json).apply()
    }

    private fun setupBackArrow() {
        backArrow.setOnClickListener {
            startActivity(Intent(this@AddOperationActivity, DashboardActivity::class.java))
            finish()
        }
    }

    private fun setupDateInput() {
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth, 0, 0, 0)
            selectedDateMillis = calendar.timeInMillis
            val formattedDate = "%02d.%02d.%d".format(dayOfMonth, month + 1, year)
            dateInput.setText(formattedDate)
        }

        dateInput.setOnClickListener {
            DatePickerDialog(
                this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupTransactionTypeSpinner() {
        val transactionTypes = listOf("Transaction type:", "Income", "Expense")
        val adapter = createStyledSpinnerAdapter(transactionTypes)
        transactionTypeSpinner.adapter = adapter
        transactionTypeSpinner.setSelection(0)
    }

    private fun loadCards() {
        cardsList = loadSavedCards()
    }

    private fun setupSelectAccountSpinner() {
        val currencySymbolMap = mapOf(
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

        val accountNames = mutableListOf("Select account")
        accountNames.addAll(cardsList.map { it.name })

        val adapter = createStyledSpinnerAdapter(accountNames)
        selectAccountSpinner.adapter = adapter
        selectAccountSpinner.setSelection(0)

        val currencyTextView = findViewById<TextView>(R.id.currency)

        selectAccountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    currencyTextView.text = "?"
                } else {
                    val selectedCardName = parent.getItemAtPosition(position).toString()
                    val selectedCard = cardsList.find { it.name == selectedCardName }

                    val symbol = currencySymbolMap[selectedCard?.currency] ?: selectedCard?.currency ?: "?"
                    currencyTextView.text = symbol
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }


    private fun loadUserCategories() {
        val prefs = getEncryptedPrefs()
        val json = prefs.getString("user_categories", null)
        userCategories = if (json != null) {
            gson.fromJson(json, object : TypeToken<MutableList<String>>() {}.type)
        } else {
            mutableListOf()
        }
    }

    private fun saveUserCategories() {
        val prefs = getEncryptedPrefs()
        prefs.edit().putString("user_categories", gson.toJson(userCategories)).apply()
    }

    private fun updateCategorySpinnerWithNewCategory(newCategory: String?) {
        val categories = mutableListOf("Select category").apply {
            addAll(userCategories)
            add("Add new category")
        }

        val adapter = createStyledSpinnerAdapter(categories, highlightLastItem = true)
        selectCategorySpinner.adapter = adapter

        selectCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                if (position == categories.lastIndex) {
                    val intent = Intent(this@AddOperationActivity, AddCategoryActivity::class.java)
                    addCategoryLauncher.launch(intent)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        newCategory?.let {
            val index = categories.indexOf(it)
            if (index != -1) selectCategorySpinner.setSelection(index)
        } ?: run {
            selectCategorySpinner.setSelection(0)
        }
    }

    private fun createStyledSpinnerAdapter(
        items: List<String>,
        highlightLastItem: Boolean = false
    ): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(this, R.layout.spinner_item, R.id.spinnerText, items) {
            override fun isEnabled(position: Int): Boolean = position != 0

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val inflater = LayoutInflater.from(context)
                val view = inflater.inflate(R.layout.spinner_dropdown_item, parent, false)
                val textView = view.findViewById<TextView>(R.id.dropdownText)
                textView.text = items[position]

                val color = when {
                    position == 0 -> resources.getColor(android.R.color.darker_gray)
                    highlightLastItem && position == items.lastIndex -> resources.getColor(android.R.color.holo_blue_dark)
                    else -> resources.getColor(R.color.theme_color)
                }

                textView.setTextColor(color)
                return view
            }
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

    private fun loadSavedCards(): List<AddCardActivity.Card> {
        val json = getEncryptedPrefs().getString("cards_list", null)
        return if (json != null) {
            gson.fromJson(json, object : TypeToken<List<AddCardActivity.Card>>() {}.type)
        } else {
            emptyList()
        }
    }
}