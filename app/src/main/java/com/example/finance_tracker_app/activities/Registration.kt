package com.example.finance_tracker_app.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.appcompat.app.AppCompatDelegate
import com.example.finance_tracker_app.utils.CardStorage
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.example.finance_tracker_app.data.db.AppDatabase
import com.example.finance_tracker_app.data.db.Operation
import com.example.finance_tracker_app.R
import com.example.finance_tracker_app.activities.AddCardActivity.Card
import com.example.finance_tracker_app.data.api.CodeVerificationRequest
import com.example.finance_tracker_app.data.api.EmailRequest
import com.example.finance_tracker_app.data.api.RetrofitClient
import com.example.finance_tracker_app.data.api.UserDataResponse


class Registration : AppCompatActivity() {

    private lateinit var sendButton: Button
    private lateinit var verifyButton: Button
    private lateinit var emailField: TextInputEditText
    private lateinit var codeField: TextInputEditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var nameField: TextInputEditText


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_theme", false)
        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registration_layout)

        sendButton = findViewById(R.id.send_code_button)
        verifyButton = findViewById(R.id.check_correctly_code_button)
        emailField = findViewById(R.id.email_input)
        codeField = findViewById(R.id.enter_code_input)
        emailLayout = findViewById(R.id.email_input_layout)
        nameField = findViewById(R.id.name_input)

        sendButton.isEnabled = false
        sendButton.setBackgroundTintList(getColorStateList(R.color.primary_light))

        verifyButton.isEnabled = false
        verifyButton.setBackgroundTintList(getColorStateList(R.color.primary_light))

        emailField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val email = s.toString().trim()
                val isValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()

                if (emailField.isEnabled) {
                    sendButton.isEnabled = isValid
                    sendButton.setBackgroundTintList(
                        getColorStateList(if (isValid) R.color.primary_color else R.color.primary_light)
                    )

                    if (!isValid && email.isNotEmpty()) {
                        emailLayout.error = "Incorrect email"
                        emailLayout.setErrorEnabled(true)
                    } else {
                        emailLayout.error = null
                        emailLayout.setErrorEnabled(false)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        sendButton.setOnClickListener {
            val email = emailField.text.toString().trim()

            val name = nameField.text.toString().trim()
            if (name.isEmpty()) {
                nameField.error = "Enter name"
                return@setOnClickListener
            }

            if (sendButton.text == "Change email") {
                emailField.isEnabled = true
                emailField.requestFocus()
                sendButton.text = "Send code"
                sendButton.isEnabled = false
                sendButton.setBackgroundTintList(getColorStateList(R.color.primary_light))
                codeField.setText("")
                verifyButton.isEnabled = false
                verifyButton.setBackgroundTintList(getColorStateList(R.color.primary_light))
                return@setOnClickListener
            }
            emailField.isEnabled = false
            codeField.requestFocus()
            sendButton.isEnabled = false
            sendButton.setBackgroundTintList(getColorStateList(R.color.primary_light))
            sendButton.text = "Sending..."

            val request = EmailRequest(email)
            RetrofitClient.instance.sendCode(request).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@Registration, "Code sent!", Toast.LENGTH_SHORT).show()
                        sendButton.text = "Change email"
                        sendButton.isEnabled = true
                        verifyButton.isEnabled = true
                        verifyButton.setBackgroundTintList(getColorStateList(R.color.primary_color))
                    } else {
                        Toast.makeText(this@Registration, "Error sending code", Toast.LENGTH_SHORT).show()
                        sendButton.isEnabled = true
                        sendButton.text = "Send code"
                        emailField.isEnabled = true
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@Registration, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    sendButton.isEnabled = true
                    emailField.isEnabled = true
                    sendButton.text = "Send code"
                }
            })
        }

        verifyButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val code = codeField.text.toString().trim()

            if (code.toString().length < 6) {
                Toast.makeText(this, "Enter the six-digit code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = CodeVerificationRequest(email, code)
            RetrofitClient.instance.verifyCode(request).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        val name = nameField.text.toString().trim()
                        saveName(name)
                        Toast.makeText(this@Registration, "The test was successful!", Toast.LENGTH_SHORT).show()
                        saveEmailSecurely(email)

                        val request = EmailRequest(emailField.text.toString())

                        RetrofitClient.instance.registerUser(request).enqueue(object : Callback<ResponseBody> {
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                if (response.code() == 201 || response.code() == 200) {
                                    Log.d("Code 200||201", response.code().toString())
                                    Toast.makeText(this@Registration, "email registered", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@Registration, SetUpPassCode::class.java))
                                    finish()
                                } else if (response.code() == 409) {
                                    Log.d("Code 409", email)
                                    showRestoreDialog(email)
                                } else {
                                    Toast.makeText(this@Registration, "Registration error", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                Toast.makeText(this@Registration, "request failed", Toast.LENGTH_SHORT).show()
                            }
                        })


                    } else {
                        Toast.makeText(this@Registration, "Invalid code", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@Registration, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        val skipRegistrationButton = findViewById<TextView>(R.id.skip_registration)
        skipRegistrationButton.setOnClickListener {
            startActivity(Intent(this@Registration, SetUpPassCode::class.java))
            finish()
        }
    }

    private fun showRestoreDialog(email: String) {
        RetrofitClient.instance.getUserData(email)
            .enqueue(object : Callback<UserDataResponse> {
                override fun onResponse(
                    call: Call<UserDataResponse>,
                    response: Response<UserDataResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!
                        Log.d("check data", data.toString())
                        val hasAnyData = data.cards.isNotEmpty() ||
                                data.categories.isNotEmpty() ||
                                data.operations.isNotEmpty()
                        Log.d("check data", hasAnyData.toString())
                        if (!hasAnyData) {
                            startActivity(Intent(this@Registration, SetUpPassCode::class.java))
                            finish()
                        }

                        AlertDialog.Builder(this@Registration)
                            .setTitle("Recover data?")
                            .setMessage("Previously saved data found. Do you want to restore it?")
                            .setPositiveButton("Yes") { _, _ ->
                                restoreDataLocally(data)
                            }
                            .setNegativeButton("No") { _, _ ->
                                clearRemoteData(email)
                                startActivity(Intent(this@Registration, SetUpPassCode::class.java))
                                finish()
                            }
                            .show()
                    }
                }

                override fun onFailure(call: Call<UserDataResponse>, t: Throwable) {
                    Log.e("RestoreDialog", "Error loading data", t)
                }
            })
    }


    private fun restoreDataLocally(data: UserDataResponse) {
        val cards = data.cards.map {
            Card(name = it.name, balance = it.balance, currency = it.currency)
        }
        CardStorage.saveCards(this, cards.toMutableList())

        val prefs = getSharedPreferences("user_categories", MODE_PRIVATE)
        val json = Gson().toJson(data.categories.map {
            Category(name = it.name, color = it.color)
        })
        prefs.edit().putString("categories_list", json).apply()

        val db = AppDatabase.Companion.getDatabase(this)
        val operationDao = db.operationDao()
        lifecycleScope.launch {
            val operations = data.operations.map {
                Operation(
                    type = it.type,
                    amount = it.amount,
                    accountName = it.accountName,
                    currency = it.currency,
                    category = it.category,
                    date = it.date,
                    note = it.note
                )
            }
            operationDao.insertAll(operations)
            Toast.makeText(this@Registration, "Data restored", Toast.LENGTH_SHORT).show()
        }
        startActivity(Intent(this@Registration, SetUpPassCode::class.java))
        finish()
    }


    private fun clearRemoteData(email: String) {
        val emailRequest = EmailRequest(email = email)
        RetrofitClient.instance.deleteUserData(emailRequest).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    saveEmailSecurely(email)
                    Toast.makeText(this@Registration, "Data deleted, let's start from scratch", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@Registration, SetUpPassCode::class.java))
                    finish()
                } else {
                    Toast.makeText(this@Registration, "Failed to delete data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@Registration, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveEmailSecurely(email: String) {
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            applicationContext,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        sharedPreferences.edit().putString("user_email", email).apply()
    }


    private fun saveName(name: String) {
        try {
            val masterKey = MasterKey.Builder(applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                applicationContext,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            with(sharedPreferences.edit()) {
                putString("user_name", name)
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putString("user_name", name).apply()
        }
    }
}
