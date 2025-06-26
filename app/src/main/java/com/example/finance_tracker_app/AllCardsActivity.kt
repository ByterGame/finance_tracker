package com.example.finance_tracker_app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context
import com.example.finance_tracker_app.AddCardActivity.Card




class AllCardsActivity : AppCompatActivity() {


    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var cards: MutableList<Card>
    private lateinit var container: LinearLayout

    private fun saveCards(cards: List<Card>) {
        val json = Gson().toJson(cards)
        sharedPreferences.edit().putString("cards_list", json).apply()
    }

    private fun loadCards(): MutableList<Card> {
        val cardsJson = sharedPreferences.getString("cards_list", null)
        return if (cardsJson != null) {
            val type = object : TypeToken<MutableList<Card>>() {}.type
            Gson().fromJson(cardsJson, type)
        } else {
            mutableListOf()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_theme", false)
        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.all_cards)

        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            this,
            "cards_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        container = findViewById(R.id.cards_container)
        cards = loadCards()

        val backArrow = findViewById<ImageButton>(R.id.back_arrow)
        backArrow.setOnClickListener {
            startActivity(Intent(this@AllCardsActivity, DashboardActivity::class.java))
            finish()
        }

        val addCard = findViewById<FrameLayout>(R.id.add_new_card)
        addCard.setOnClickListener {
            startActivity(Intent(this@AllCardsActivity, AddCardActivity::class.java))
            finish()
        }

        refreshCardsUI()
    }

    private fun refreshCardsUI() {
        container.removeAllViews()

        if (cards.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "You don't have any cards yet."
                textSize = 18f
                setTextColor(resources.getColor(R.color.theme_color, theme))
                setPadding(32, 32, 32, 32)
            }
            container.addView(emptyView)
        } else {
            cards.forEachIndexed { index, card ->
                val cardView = layoutInflater.inflate(R.layout.card_item_with_crush, container, false)

                val typeAndName = cardView.findViewById<TextView>(R.id.card_type_and_name)
                val icon = cardView.findViewById<ImageView>(R.id.card_icon)
                val balance = cardView.findViewById<TextView>(R.id.card_balance)
                val crushButton = cardView.findViewById<ImageButton>(R.id.crush_button)

                typeAndName.text = "Card name: ${card.name}"
                val currencySymbol = when (card.currency) {
                    "USD" -> "$"
                    "EUR" -> "€"
                    "GBP" -> "£"
                    "JPY", "CNY" -> "¥"
                    "RUB" -> "₽"
                    "INR" -> "₹"
                    "AUD" -> "A$"
                    "CAD" -> "C$"
                    "CHF" -> "₣"
                    "TRY" -> "₺"
                    else -> card.currency ?: ""
                }
                val formattedBalance = String.format("%,.0f", card.balance)

                balance.text = "$formattedBalance $currencySymbol"


                icon.setImageResource(R.drawable.card)

                crushButton.setOnClickListener {
                    showDeleteConfirmationDialog(index, cardView)
                }

                container.addView(cardView)
            }
        }
    }

    private fun showDeleteConfirmationDialog(index: Int, cardView: View) {
        AlertDialog.Builder(this)
            .setTitle("Delete Card")
            .setMessage("Are you sure you want to delete this card?")
            .setPositiveButton("Delete") { _, _ ->
                animateCardRemoval(index, cardView)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun animateCardRemoval(index: Int, cardView: View) {
        val fadeOut = AlphaAnimation(1f, 0f).apply {
            duration = 700
            fillAfter = true
        }

        fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}

            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                cards.removeAt(index)
                saveCards(cards)
                refreshCardsUI()
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        cardView.startAnimation(fadeOut)
    }
}
