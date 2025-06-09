package com.example.finance_tracker_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.finance_tracker_app.databinding.EnterPinCodeLayoutBinding

class EnterPassCode : AppCompatActivity() {

    private lateinit var binding: EnterPinCodeLayoutBinding
    private val enteredPassword = StringBuilder()
    private var correctPassword: String = "0000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        correctPassword = loadPassword() ?: "0000"
        binding = EnterPinCodeLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        updateCircles()
        setupNumberButtons()
        setupDeleteButton()
        setupForgotCodeButton()
    }

    private fun loadPassword(): String? {
        return try {
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

            sharedPreferences.getString("app_pin_code", null)
                ?: getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .getString("pin_code", null)
        } catch (e: Exception) {
            e.printStackTrace()
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getString("pin_code", null)
        }
    }

    private fun setupNumberButtons() {
        listOf(
            binding.key1, binding.key2, binding.key3,
            binding.key4, binding.key5, binding.key6,
            binding.key7, binding.key8, binding.key9, binding.key0
        ).forEach { button ->
            button.setOnClickListener {
                if (enteredPassword.length < 4) {
                    val number = when (button.id) {
                        binding.key0.id -> "0"
                        binding.key1.id -> "1"
                        binding.key2.id -> "2"
                        binding.key3.id -> "3"
                        binding.key4.id -> "4"
                        binding.key5.id -> "5"
                        binding.key6.id -> "6"
                        binding.key7.id -> "7"
                        binding.key8.id -> "8"
                        binding.key9.id -> "9"
                        else -> ""
                    }
                    enteredPassword.append(number)
                    updateCircles()

                    if (enteredPassword.length == 4) {
                        checkPassword()
                    }
                }
            }
        }
    }

    private fun setupDeleteButton() {
        binding.delete.setOnClickListener {
            if (enteredPassword.isNotEmpty()) {
                enteredPassword.deleteCharAt(enteredPassword.length - 1)
                updateCircles()
            }
        }

        binding.delete.setOnLongClickListener {
            enteredPassword.clear()
            updateCircles()
            true
        }
    }

    private fun setupForgotCodeButton() {
        binding.forgotCode.setOnClickListener {
            Toast.makeText(this, "Forgot code clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCircles() {
        listOf(binding.circle1, binding.circle2, binding.circle3, binding.circle4).forEachIndexed { index, circle ->
            (circle.background as? GradientDrawable)?.setColor(
                ContextCompat.getColor(
                    this,
                    if (index < enteredPassword.length) R.color.primary_color else R.color.gray
                )
            )
        }
    }

    private fun checkPassword() {
        if (enteredPassword.toString() == correctPassword) {
            Toast.makeText(this, "Success! Password correct", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@EnterPassCode, DashboardActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Wrong password. Try again", Toast.LENGTH_SHORT).show()
            enteredPassword.clear()
            updateCircles()
        }
    }
}