package com.example.finance_tracker_app

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

data class ErApiResponse(
    val result: String? = null,
    @SerializedName("base_code")
    val baseCode: String? = null,
    @SerializedName("time_last_update_utc")
    val lastUpdate: String? = null,
    @SerializedName("rates")
    val rates: Map<String, Any>? = null
)

interface ExchangeRateApi {
    @GET("v6/latest/{base}")
    fun getRates(@Path("base") base: String = "USD"): Call<ErApiResponse>
}
