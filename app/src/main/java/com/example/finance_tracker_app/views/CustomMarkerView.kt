package com.example.finance_tracker_app.views

import android.content.Context
import android.widget.TextView
import com.example.finance_tracker_app.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class CustomMarkerView(context: Context) : MarkerView(context, R.layout.marker_view) {

    private val labelTextView: TextView = findViewById(R.id.markerLabel)
    private val valueTextView: TextView = findViewById(R.id.markerValue)

    private var offsetX = 0f
    private var offsetY = 0f

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        val pieEntry = e as? PieEntry
        pieEntry?.let {
            labelTextView.text = it.label
            valueTextView.text = it.value.toInt().toString()
        }

        highlight?.let {
            val x = it.drawX
            val y = it.drawY

            val centerX = chartView.width / 2f
            val centerY = chartView.height / 2f


            offsetX = when {
                x > centerX -> -width.toFloat() - 300f
                else -> 300f
            }

            offsetY = when {
                y > centerY -> -height.toFloat() - 200f
                else -> 200f
            }
        }

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(offsetX, offsetY)
    }
}