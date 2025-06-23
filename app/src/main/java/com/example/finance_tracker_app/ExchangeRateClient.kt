package com.example.finance_tracker_app

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ExchangeRateClient {
    private const val BASE_URL = "https://open.er-api.com/"

    val api: ExchangeRateApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(
                GsonBuilder().serializeNulls().create()
            ))
            .build()
            .create(ExchangeRateApi::class.java)
    }
}
