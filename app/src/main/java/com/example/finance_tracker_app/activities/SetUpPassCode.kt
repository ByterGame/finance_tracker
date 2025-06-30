package com.example.finance_tracker_app.activities

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.finance_tracker_app.databinding.SetUpPinCodeLayoutBinding
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException
import androidx.core.content.edit
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import com.example.finance_tracker_app.R


class SetUpPassCode: AppCompatActivity() {

    private lateinit var binding: SetUpPinCodeLayoutBinding
    private val enteredPassword = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_theme", false)
        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
        super.onCreate(savedInstanceState)
        binding = SetUpPinCodeLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNumberButtons()
        setupDeleteButton()
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
        if (enteredPassword.length == 4) {
            savePassword(enteredPassword.toString())
            Toast.makeText(this, "PIN code set successfully", Toast.LENGTH_SHORT).show()
            navigateToDashboard()
        }
    }

    private fun savePassword(password: String) {
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
                putString("app_pin_code", password)
                apply()
            }
        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
            savePasswordWithRegularPrefs(password)
        } catch (e: IOException) {
            e.printStackTrace()
            savePasswordWithRegularPrefs(password)
        }
    }

    private fun savePasswordWithRegularPrefs(password: String) {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit() {
                putString("pin_code", password)
            }
    }

    private fun navigateToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}