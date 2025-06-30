package com.example.finance_tracker_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.security.GeneralSecurityException


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            retryPendingOperations(this@MainActivity)
            retryPendingCards(this@MainActivity)
            retryPendingCategories(this@MainActivity)
        }
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

    suspend fun retryPendingOperations(context: Context) {
        val dao = AppDatabase.getDatabase(context).pendingOperationDao()
        val list = dao.getAll()

        if (list.isEmpty()) {
            Log.d("RetryOperation", "No pending operations to sync")
            return
        }

        val gson = Gson()

        list.forEach { pending ->
            val request = gson.fromJson(pending.operationJson, OperationRequest::class.java)

            RetrofitClient.instance.saveOperation(request)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            lifecycleScope.launch {
                                dao.delete(pending)
                                Log.d("RetryOperation", "Successfully synced pending operation")
                            }
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("RetryOperation", "Still failed to sync pending operation", t)
                    }
                })
        }
    }

    suspend fun retryPendingCards(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val pendingCardDao = db.pendingCardDao()
        val gson = Gson()

        val list = pendingCardDao.getAll()
        if (list.isEmpty()) {
            Log.d("RetryCards", "No pending cards to sync")
            return
        }

        for (pending in list) {
            val cardRequest = gson.fromJson(pending.cardJson, CardRequest::class.java)

            RetrofitClient.instance.saveCard(cardRequest)
                .enqueue(object : retrofit2.Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            lifecycleScope.launch {
                                pendingCardDao.delete(pending)
                                Log.d("RetryCards", "Card synced successfully and deleted locally")
                            }
                        } else {
                            Log.e("RetryCards", "Failed to sync card: ${response.code()}")
                        }
                    }
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("RetryCards", "Network error syncing card", t)
                    }
                })
        }
    }

    suspend fun retryPendingCategories(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val pendingCategoryDao = db.pendingCategoryDao()
        val gson = Gson()

        val list = pendingCategoryDao.getAll()
        if (list.isEmpty()) {
            Log.d("RetryCategories", "No pending categories to sync")
            return
        }

        for (pending in list) {
            val categoryRequest = gson.fromJson(pending.categoryJson, CategoryRequest::class.java)

            RetrofitClient.instance.saveCategory(categoryRequest)
                .enqueue(object : retrofit2.Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            lifecycleScope.launch {
                                pendingCategoryDao.delete(pending)
                                Log.d("RetryCategories", "Category synced and removed from local storage")
                            }
                        } else {
                            Log.e("RetryCategories", "Failed to sync category: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("RetryCategories", "Network error syncing category", t)
                    }
                })
        }
    }

}
