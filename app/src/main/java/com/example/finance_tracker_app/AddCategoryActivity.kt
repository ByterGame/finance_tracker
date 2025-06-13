package com.example.finance_tracker_app

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import android.graphics.drawable.GradientDrawable

class AddCategoryActivity : AppCompatActivity(), ColorPickerDialogListener {

    private var selectedColor: Int = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_category_layout)

        val colorButton = findViewById<Button>(R.id.open_color_picker)

        colorButton.setOnClickListener {
            ColorPickerDialog.newBuilder()
                .setDialogType(ColorPickerDialog.TYPE_PRESETS)
                .setAllowCustom(true)
                .setDialogId(0)
                .setColor(selectedColor)
                .show(this)
        }
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        if (dialogId == 0) {
            selectedColor = color

            val colorPreview = findViewById<View>(R.id.color_preview)

            val backgroundDrawable = colorPreview.background as? GradientDrawable
            backgroundDrawable?.setColor(color)
        }
    }

    override fun onDialogDismissed(dialogId: Int) {
        // я хз как там дефолтная заглушка работает, поэтому свою оставлю
    }
}
