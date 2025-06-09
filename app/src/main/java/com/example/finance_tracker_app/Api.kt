package com.example.finance_tracker_app

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import okhttp3.ResponseBody

interface Api {
    @POST("send_code")
    fun sendCode(@Body emailRequest: EmailRequest): Call<ResponseBody>

    @POST("verify_code")
    fun verifyCode(@Body verifyRequest: CodeVerificationRequest): Call<ResponseBody>
}

data class EmailRequest(val email: String)
data class CodeVerificationRequest(val email: String, val code: String)