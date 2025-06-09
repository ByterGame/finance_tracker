package com.example.finance_tracker_app

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Registration : AppCompatActivity() {

    private lateinit var sendButton: ImageButton
    private lateinit var verifyButton: ImageButton
    private lateinit var emailField: TextInputEditText
    private lateinit var codeField: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registration_layout)

        sendButton = findViewById(R.id.send_code_button)
        verifyButton = findViewById(R.id.check_correctly_code_button)
        emailField = findViewById(R.id.email_input)
        codeField = findViewById(R.id.enter_code_input)

        sendButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = EmailRequest(email)
            RetrofitClient.instance.sendCode(request).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@Registration, "Код отправлен!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@Registration, "Ошибка отправки кода", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@Registration, "Ошибка: ${t.message}", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@Registration, "Проверка прошла успешно!", Toast.LENGTH_SHORT).show()
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
}
