package com.example.finance_tracker_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey


class Registration : AppCompatActivity() {

    private lateinit var sendButton: Button
    private lateinit var verifyButton: Button
    private lateinit var emailField: TextInputEditText
    private lateinit var codeField: TextInputEditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var nameField: TextInputEditText


    override fun onCreate(savedInstanceState: Bundle?) {
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
                        emailLayout.error = "Некорректный email"
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
                nameField.error = "Введите имя"
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
            sendButton.isEnabled = false
            sendButton.setBackgroundTintList(getColorStateList(R.color.primary_light))
            sendButton.text = "Sending..."
            val request = EmailRequest(email)
            RetrofitClient.instance.sendCode(request).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@Registration, "Код отправлен!", Toast.LENGTH_SHORT).show()
                        emailField.isEnabled = false
                        sendButton.text = "Change email"
                        sendButton.isEnabled = true
                        verifyButton.isEnabled = true
                        verifyButton.setBackgroundTintList(getColorStateList(R.color.primary_color))
                    } else {
                        Toast.makeText(this@Registration, "Ошибка отправки кода", Toast.LENGTH_SHORT).show()
                        sendButton.isEnabled = true
                        sendButton.text = "Send code"
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@Registration, "Ошибка: ${t.message}", Toast.LENGTH_SHORT).show()
                    sendButton.isEnabled = true
                    sendButton.text = "Send code"
                }
            })
        }

        verifyButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val code = codeField.text.toString().trim()

            if (email.isEmpty() || code.isEmpty()) {
                Toast.makeText(this, "Введите email и код", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = CodeVerificationRequest(email, code)
            RetrofitClient.instance.verifyCode(request).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        val name = nameField.text.toString().trim()
                        saveName(name)
                        Toast.makeText(this@Registration, "Проверка прошла успешно!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@Registration, SetUpPassCode::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@Registration, "Неверный код", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@Registration, "Ошибка: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
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
