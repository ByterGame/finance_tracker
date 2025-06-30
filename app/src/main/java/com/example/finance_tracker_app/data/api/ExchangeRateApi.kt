package com.example.finance_tracker_app.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

data class ErApiResponse(
    val result: String? = null,
    @SerializedName("base_code")
    val baseCode: String? = null,
    @SerializedName("time_last_update_unix")
    val lastUpdate: String? = null,
    @SerializedName("rates")
    val rates: Map<String, Any>? = null,
    @SerializedName("time_next_update_unix")
    val nextUpdate: String? = null
)

interface ExchangeRateApi {
    @GET("v6/latest/{base}")
    fun getRates(@Path("base") base: String = "USD"): Call<ErApiResponse>
}
