package com.example.finance_tracker_app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.finance_tracker_app.AddOperationActivity
import okhttp3.ResponseBody
import retrofit2.Call

data class Category(val name: String, val color: Int)

class AddCategoryActivity : AppCompatActivity(), ColorPickerDialogListener {

    private val gson = Gson()
    private lateinit var categoryNameInput: EditText
    private var selectedColor: Int = Color.WHITE
    private lateinit var colorPreview: View

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_theme", false)
        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_category_layout)

        categoryNameInput = findViewById(R.id.name_category_input)
        colorPreview = findViewById(R.id.color_preview)
        val openColorPickerBtn = findViewById<Button>(R.id.open_color_picker)
        val saveButton = findViewById<Button>(R.id.add_category)

        openColorPickerBtn.setOnClickListener {
            ColorPickerDialog.newBuilder()
                .setDialogType(ColorPickerDialog.TYPE_PRESETS)
                .setAllowCustom(true)
                .setDialogId(0)
                .setColor(selectedColor)
                .show(this)
        }

        saveButton.setOnClickListener {
            val name = categoryNameInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveCustomCategory(name)

            val resultIntent = intent
            resultIntent.putExtra("new_category_name", name)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        if (dialogId == 0) {
            selectedColor = color
            val drawable = colorPreview.background as GradientDrawable
            drawable.setColor(selectedColor)
        }
    }

    override fun onDialogDismissed(dialogId: Int) {}

    private fun saveCustomCategory(categoryName: String) {
        val prefs = getSharedPreferences("user_categories", MODE_PRIVATE)
        val json = prefs.getString("categories_list", null)

        val type = object : TypeToken<MutableList<Category>>() {}.type
        val currentCategories: MutableList<Category> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
        
        if (currentCategories.none { it.name == categoryName }) {
            currentCategories.add(Category(categoryName, selectedColor))

            val email = getUserEmail(this@AddCategoryActivity)
            Log.d("email", email.toString())
            val categoryRequest = CategoryRequest(
                userEmail = email.toString(),
                name = categoryName,
                color = selectedColor
            )
            RetrofitClient.instance.saveCategory(categoryRequest)
                .enqueue(object : retrofit2.Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            Log.d("AddCategory", "Category synced to backend")
                        } else {
                            Log.e("AddCategory", "Failed to sync Category: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("AddCategory", "Network error syncing operation", t)
                    }
                })
            prefs.edit().putString("categories_list", gson.toJson(currentCategories)).apply()
        }
    }
    fun getUserEmail(context: Context): String? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                context,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPreferences.getString("user_email", null)
        } catch (e: Exception) {
            e.printStackTrace()
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("user_email", null)
        }
    }
}
