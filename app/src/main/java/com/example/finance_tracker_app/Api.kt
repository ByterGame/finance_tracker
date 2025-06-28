package com.example.finance_tracker_app

import android.R
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import okhttp3.ResponseBody

interface Api {
    @POST("send_code")
    fun sendCode(@Body emailRequest: EmailRequest): Call<ResponseBody>

    @POST("verify_code")
    fun verifyCode(@Body verifyRequest: CodeVerificationRequest): Call<ResponseBody>

    @POST("register_user")
    fun registerUser(@Body request: EmailRequest): Call<ResponseBody>

    @POST("save_operation")
    fun saveOperation(@Body request: OperationRequest): Call<ResponseBody>

    @POST("save_card")
    fun saveCard(@Body request: CardRequest): Call<ResponseBody>

    @POST("save_category")
    fun saveCategory(@Body request: CategoryRequest): Call<ResponseBody>
}


data class EmailRequest(val email: String)
data class CodeVerificationRequest(val email: String, val code: String)
data class OperationRequest(
    val userEmail: String,
    val type: String,
    val amount: Double,
    val accountName: String,
    val currency: String,
    val category: String,
    val date: Long,
    val note: String? = null
)
data class CardRequest(
    val userEmail: String,
    val name: String,
    val balance: Double,
    val currency: String
)
data class CategoryRequest(
    val userEmail: String,
    val name: String,
    val color: Int
)