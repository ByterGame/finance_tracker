package com.example.finance_tracker_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import android.graphics.Color
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.Legend
import com.example.finance_tracker_app.databinding.DetailedStatisticsLayoutBinding
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.internal.cache.DiskLruCache


class DetailedStatisticsActivity : AppCompatActivity() {

    private lateinit var binding: DetailedStatisticsLayoutBinding
    private lateinit var db: AppDatabase
    private lateinit var operationDao: OperationDao


    data class Category(val name: String, val color: Int)

    private fun loadUserCategoriesWithColor(): List<Category> {
        val prefs = getSharedPreferences("user_categories", MODE_PRIVATE)
        val json = prefs.getString("categories_list", null)
        return if (json != null) {
            val type = object : com.google.gson.reflect.TypeToken<List<Category>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DetailedStatisticsLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        operationDao = db.operationDao()

        lifecycleScope.launch {
            val operations = operationDao.getAllOperations()
            val categories = loadUserCategoriesWithColor()

            setupTotalAmountChart(operations)
            setupIncomePieChart(operations, categories)
            setupExpensePieChart(operations, categories)
        }

        binding.backArrow.setOnClickListener {
            startActivity(Intent(this@DetailedStatisticsActivity, DashboardActivity::class.java))
            finish()
        }
    }

    fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun setupTotalAmountChart(operations: List<Operation>) {
        val income = operations.filter { it.type.lowercase() == "income" }.sumOf { it.amount }
        val expense = operations.filter { it.type.lowercase() == "expense" }.sumOf { it.amount }

        val entries = listOf(
            PieEntry(income.toFloat(), "Income"),
            PieEntry(expense.toFloat(), "Expense")
        )

        val colors = listOf(
            ContextCompat.getColor(this, R.color.income_green),
            ContextCompat.getColor(this, R.color.expense_red)
        )

        setupPieChart(
            binding.pieChartTotalAmount,
            "Total amount",
            entries,
            colors,
            heightDP = 200,
            widthDP = 200
        )
    }

    private fun setupIncomePieChart(operations: List<Operation>, categories: List<Category>) {
        val incomeOps = operations.filter { it.type.lowercase() == "income" }
        val totalIncome = incomeOps.sumOf { it.amount }

        val grouped = incomeOps.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        grouped.forEach { (categoryName, sum) ->
            val percent = sum / totalIncome
            if (percent >= 0.05) {
                entries.add(PieEntry(sum.toFloat(), categoryName))
                val catColor = categories.find { it.name == categoryName }?.color ?: Color.GRAY
                colors.add(catColor)
            }
        }

        setupPieChart(
            binding.pieChartTotalIncome,
            "Total income",
            entries,
            colors,
            heightDP = 170,
            widthDP = 170
        )
    }

    private fun setupExpensePieChart(operations: List<Operation>, categories: List<Category>) {
        val expenseOps = operations.filter { it.type.lowercase() == "expense" }
        val totalExpense = expenseOps.sumOf { it.amount }

        val grouped = expenseOps.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        grouped.forEach { (categoryName, sum) ->
            val percent = sum / totalExpense
            if (percent >= 0.05) {
                entries.add(PieEntry(sum.toFloat(), categoryName))
                val catColor = categories.find { it.name == categoryName }?.color ?: Color.GRAY
                colors.add(catColor)
            }
        }

        setupPieChart(
            binding.pieChartTotalExpenses,
            "Total expense",
            entries,
            colors,
            heightDP = 170,
            widthDP = 170
        )
    }


    fun setupPieChart(pieChart: PieChart, centerText: String, entries: List<PieEntry>, colors: List<Int>, heightDP: Int, widthDP: Int) {
        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 3f
            selectionShift = 5f
            setAutomaticallyDisableSliceSpacing(true)
        }

        val data = PieData(dataSet)
        data.setDrawValues(false)

        pieChart.apply {
            this.data = data
            setDrawEntryLabels(false)
            isDrawHoleEnabled = true
            holeRadius = 70f
            transparentCircleRadius = 0f
            description.isEnabled = false
            this.centerText = centerText
            setCenterTextSize(12f)
            setCenterTextColor(ContextCompat.getColor(context, R.color.primary_color))
            setCenterTextTypeface(ResourcesCompat.getFont(context, R.font.lexend_deca))

            layoutParams = layoutParams.apply {
                width = dpToPx(widthDP)
                height = dpToPx(heightDP)
            }

        }

        val legend = pieChart.legend
        legend.isEnabled = true
        legend.textSize = 11f
        legend.form = Legend.LegendForm.CIRCLE
        legend.textColor = ContextCompat.getColor(this@DetailedStatisticsActivity, R.color.primary_color)
        legend.typeface = ResourcesCompat.getFont(this, R.font.lexend_deca)
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.yOffset = 15f
        legend.setDrawInside(false)

        pieChart.invalidate()

    }

}