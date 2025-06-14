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

class AddCategoryActivity : AppCompatActivity(), ColorPickerDialogListener {

    private val gson = Gson()
    private lateinit var categoryNameInput: EditText
    private var selectedColor: Int = Color.WHITE
    private lateinit var colorPreview: View

    override fun onCreate(savedInstanceState: Bundle?) {
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
        val currentCategories: MutableList<String> = if (json != null) {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
        if (!currentCategories.contains(categoryName)) {
            currentCategories.add(categoryName)
            prefs.edit().putString("categories_list", gson.toJson(currentCategories)).apply()
        }
    }
}
