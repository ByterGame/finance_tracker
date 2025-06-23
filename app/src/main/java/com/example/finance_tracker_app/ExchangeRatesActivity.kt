package com.example.finance_tracker_app

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ExchangeRatesActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var dateText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.exchange_rates_layout)

        listView = findViewById(R.id.listView)
        dateText = findViewById(R.id.dateText)

        ExchangeRateClient.api.getRates("USD").enqueue(object : Callback<ErApiResponse> {
            override fun onResponse(call: Call<ErApiResponse>, response: Response<ErApiResponse>) {
                val data = response.body()

                if (data != null && data.result == "success" && data.rates != null) {
                    dateText.text = "Курсы на: ${data.lastUpdate}"
                    val items = data.rates.map { "${it.key}: ${"%.2f".format(it.value)}" }
                    listView.adapter = ArrayAdapter(this@ExchangeRatesActivity, android.R.layout.simple_list_item_1, items)
                } else {
                    dateText.text = "Ошибка: некорректные данные"
                    Log.e("API", "Result: ${data?.result}, Rates: ${data?.rates}")
                }
            }

            override fun onFailure(call: Call<ErApiResponse>, t: Throwable) {
                dateText.text = "Ошибка подключения: ${t.message}"
            }
        })
    }
}
