package com.example.finance_tracker_app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val hasPinCode = checkIfPinExists()

        val nextIntent = if (hasPinCode) {
            Intent(this, EnterPassCode::class.java)
        } else {
            Intent(this, Registration::class.java)
        }

        startActivity(nextIntent)
        finish()
    }

    private fun checkIfPinExists(): Boolean {
        return try {
            val masterKey = MasterKey.Builder(applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val encryptedPrefs = EncryptedSharedPreferences.create(
                applicationContext,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            encryptedPrefs.contains("app_pin_code") ||
                    getSharedPreferences("app_prefs", MODE_PRIVATE).contains("pin_code")
        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
            false
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
}
