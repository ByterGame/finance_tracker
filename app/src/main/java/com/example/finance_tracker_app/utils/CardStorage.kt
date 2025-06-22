package com.example.finance_tracker_app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.finance_tracker_app.AddCardActivity.Card
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CardStorage {

    private val gson = Gson()

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "cards_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun loadSavedCards(context: Context): List<Card> {
        val json = getEncryptedPrefs(context).getString("cards_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<Card>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun saveCards(context: Context, cards: List<Card>) {
        val json = gson.toJson(cards)
        getEncryptedPrefs(context).edit().putString("cards_list", json).apply()
    }
}
