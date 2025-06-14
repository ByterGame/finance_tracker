package com.example.finance_tracker_app

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DashboardActivity : AppCompatActivity() {

    data class Card(val type: String, val name: String, val balance: Double)

    private fun loadCards(): List<Card> {
        return try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                this,
                "cards_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val cardsJson = sharedPreferences.getString("cards_list", null)
            if (cardsJson != null) {
                val type = object : TypeToken<List<Card>>() {}.type
                Gson().fromJson(cardsJson, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_layout)

        val totalValue = findViewById<TextView>(R.id.total_value)
        val anyCardContainer = findViewById<FrameLayout>(R.id.any_card)
        val allCards = findViewById<FrameLayout>(R.id.all_cards_button)
        anyCardContainer.removeAllViews()
        allCards.setOnClickListener {
            startActivity(Intent(this@DashboardActivity, AllCardsActivity::class.java))
            finish()
        }
        val addCategoryButton = findViewById<LinearLayout>(R.id.add_operation_section)
        addCategoryButton.setOnClickListener {
            startActivity(Intent(this@DashboardActivity, AddOperationActivity::class.java))
            finish()
        }

        val cards = loadCards()
        val total = cards.sumOf { it.balance }
        totalValue.text = String.format("%,.0f $", total)

        if (cards.isEmpty()) {
            allCards.isEnabled = false
            val textView = TextView(this).apply {
                text = "+ Add your first card"
                textSize = 21f
                setTextColor(resources.getColor(R.color.theme_color))
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setPadding(16, 16, 16, 16)
                setOnClickListener {
                    startActivity(Intent(this@DashboardActivity, AddCardActivity::class.java))
                }
            }
            anyCardContainer.addView(textView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        } else {
            allCards.isEnabled = true
            val card = cards[0]

            val cardView = layoutInflater.inflate(R.layout.card_item, anyCardContainer, false)

            val typeAndName = cardView.findViewById<TextView>(R.id.card_type_and_name)
            val icon = cardView.findViewById<ImageView>(R.id.card_icon)
            val balance = cardView.findViewById<TextView>(R.id.card_balance)

            typeAndName.text = "${card.type}: ${card.name}"
            balance.text = String.format("%,.0f $", card.balance)

            val iconRes = when (card.type) {
                "Debit card" -> R.drawable.card
                "Savings account" -> R.drawable.card
                "Brokerage account" -> R.drawable.card
                else -> R.drawable.card
            }
            icon.setImageResource(iconRes)

            anyCardContainer.addView(cardView)
        }
    }
}
