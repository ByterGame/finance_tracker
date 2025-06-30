package com.example.finance_tracker_app.data.api

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ExchangeRatesManager {
    private const val PREFS_NAME = "exchange_rates_prefs"
    private const val KEY_RATES = "rates_json"
    private const val KEY_LAST_UPDATE = "last_update"
    private const val KEY_NEXT_UPDATE = "next_update"

    private var ratesMap: Map<String, Double> = emptyMap()
    private var lastUpdateUnix: Long = 0
    private var nextUpdateUnix: Long = 0
    private val defaultRates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.95,
        "GBP" to 0.82,
        "JPY" to 140.0,
        "CNY" to 7.0,
        "RUB" to 74.0,
        "INR" to 82.0,
        "AUD" to 1.5,
        "CAD" to 1.3,
        "CHF" to 0.92,
        "TRY" to 26.0
    )

    fun loadFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_RATES, null)
        lastUpdateUnix = prefs.getLong(KEY_LAST_UPDATE, 0)
        nextUpdateUnix = prefs.getLong(KEY_NEXT_UPDATE, 0)

        ratesMap = if (json != null) {
            Gson().fromJson(json, object : TypeToken<Map<String, Double>>() {}.type)
        } else {
            defaultRates
        }
    }

    fun saveToPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(ratesMap)
        prefs.edit()
            .putString(KEY_RATES, json)
            .putLong(KEY_LAST_UPDATE, lastUpdateUnix)
            .putLong(KEY_NEXT_UPDATE, nextUpdateUnix)
            .apply()
    }

    fun updateRates(newRates: Map<String, Any>, lastUpdate: String?, nextUpdate: String?) {
        ratesMap = newRates.mapValues { it.value.toString().toDoubleOrNull() ?: 1.0 }
        lastUpdateUnix = lastUpdate?.toLongOrNull() ?: 0
        nextUpdateUnix = nextUpdate?.toLongOrNull() ?: 0
    }

    fun getRate(currencyCode: String): Double {
        return ratesMap[currencyCode] ?: 1.0
    }

    fun getLastUpdateUnix(): Long = lastUpdateUnix
    fun getNextUpdateUnix(): Long = nextUpdateUnix

    fun convertAmount(amount: Double, fromCurrency: String, toCurrency: String): Double {
        val rateFrom = getRate(fromCurrency)
        val rateTo = getRate(toCurrency)
        return amount / rateFrom * rateTo
    }

    fun isUpdateNeeded(): Boolean {
        val now = System.currentTimeMillis() / 1000
        return now > nextUpdateUnix
    }
}
