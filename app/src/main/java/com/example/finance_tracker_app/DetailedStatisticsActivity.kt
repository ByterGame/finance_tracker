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
import android.os.Environment
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.Legend
import com.example.finance_tracker_app.databinding.DetailedStatisticsLayoutBinding
import com.example.finance_tracker_app.utils.CardStorage
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.internal.cache.DiskLruCache
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import com.example.finance_tracker_app.AddCardActivity.Card


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

        val watcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                updateStatistics()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        binding.yearsPeriod.addTextChangedListener(watcher)
        binding.monthsPeriod.addTextChangedListener(watcher)
        binding.daysPeriod.addTextChangedListener(watcher)

        updateStatistics()

        binding.backArrow.setOnClickListener {
            startActivity(Intent(this@DetailedStatisticsActivity, DashboardActivity::class.java))
            finish()
        }

        binding.exportAllData.setOnClickListener {
            exportDataToCSV()
        }
    }

    private fun exportDataToCSV() {
        lifecycleScope.launch {
            val years = binding.yearsPeriod.text.toString().toIntOrNull() ?: 0
            val months = binding.monthsPeriod.text.toString().toIntOrNull() ?: 0
            val days = binding.daysPeriod.text.toString().toIntOrNull() ?: 0

            val now = LocalDateTime.now()
            val fromDate = now.minusYears(years.toLong())
                .minusMonths(months.toLong())
                .minusDays(days.toLong())

            val fromMillis = fromDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val operations = operationDao.getAllOperations().filter { it.date >= fromMillis }
            val cards = CardStorage.loadSavedCards(this@DetailedStatisticsActivity)

            val csvContent = buildCSVContent(operations, cards, fromDate, now)
            val file = saveCSVToDocuments(csvContent)

            if (file != null) {
                Toast.makeText(this@DetailedStatisticsActivity, "File saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@DetailedStatisticsActivity, "Failed to save data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildCSVContent(
        operations: List<Operation>,
        cards: List<Card>,
        fromDate: LocalDateTime,
        toDate: LocalDateTime
    ): String {
        val sb = StringBuilder()

        sb.append("Finance Tracker Export\n")
        sb.append("Period:,${fromDate.toLocalDate()},to,${toDate.toLocalDate()}\n\n")

        sb.append("Operations\n")
        sb.append("Date,Type,Category,Amount\n")
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        for (op in operations) {
            val opDate = java.time.Instant.ofEpochMilli(op.date).atZone(ZoneId.systemDefault()).toLocalDate()
            sb.append("${opDate.format(dateFormatter)},${op.type},${op.category},${op.amount}\n")
        }
        sb.append("\n")

        val incomeOps = operations.filter { it.type.equals("income", ignoreCase = true) }
        val expenseOps = operations.filter { it.type.equals("expense", ignoreCase = true) }

        val totalIncome = incomeOps.sumOf { it.amount }
        val totalExpense = expenseOps.sumOf { it.amount }

        val monthsBetween = java.time.temporal.ChronoUnit.MONTHS.between(fromDate, toDate).coerceAtLeast(1)
        val avgIncomePerMonth = totalIncome / monthsBetween
        val avgExpensePerMonth = totalExpense / monthsBetween

        sb.append("Summary\n")
        sb.append("Total Income,${totalIncome}\n")
        sb.append("Total Expense,${totalExpense}\n")
        sb.append("Average Income Per Month,${String.format("%.2f", avgIncomePerMonth)}\n")
        sb.append("Average Expense Per Month,${String.format("%.2f", avgExpensePerMonth)}\n\n")

        sb.append("Cards\n")
        sb.append("Name,Balance,Currency\n")
        for (card in cards) {
            sb.append(",${card.name},${card.balance},${card.currency}\n")
        }

        return sb.toString()
    }

    private fun saveCSVToFile(csvContent: String): android.net.Uri? {
        return try {
            val filename = "finance_export_${System.currentTimeMillis()}.csv"
            val file = File(getExternalFilesDir(null), filename)
            file.writeText(csvContent)
            androidx.core.content.FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveCSVToDocuments(csvContent: String): File? {
        return try {
            val docsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: return null
            val filename = "finance_export_${System.currentTimeMillis()}.csv"
            val file = File(docsDir, filename)
            file.writeText(csvContent)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun updateStatistics() {
        val years = binding.yearsPeriod.text.toString().toIntOrNull() ?: 0
        val months = binding.monthsPeriod.text.toString().toIntOrNull() ?: 0
        val days = binding.daysPeriod.text.toString().toIntOrNull() ?: 0

        val now = LocalDateTime.now()
        val fromDate = now.minusYears(years.toLong())
            .minusMonths(months.toLong())
            .minusDays(days.toLong())

        val fromMillis = fromDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        lifecycleScope.launch {
            val operations = operationDao.getAllOperations().filter { it.date >= fromMillis }
            val categories = loadUserCategoriesWithColor()

            setupTotalAmountChart(operations)
            setupPieChartForType(operations, categories, "expense", binding.pieChartTotalExpenses, "Total expense")
            setupPieChartForType(operations, categories, "income", binding.pieChartTotalIncome, "Total income")
            setupGeneralInfo(operations)
            populateTopCategories(operations)
        }
    }

    private fun setupGeneralInfo(operations: List<Operation>) {
        val income = operations.filter { it.type.lowercase() == "income" }.sumOf { it.amount }
        val expense = operations.filter { it.type.lowercase() == "expense" }.sumOf { it.amount }

        val incomeValue = if (income.isNaN()) 0.0f else income.toFloat()
        val expenseValue = if (expense.isNaN()) 0.0f else expense.toFloat()
        val totalValue = expenseValue + incomeValue

        binding.totalAmount.text = String.format("%,.0f $", totalValue)
        binding.totalIncome.text = String.format("%,.0f $", incomeValue)
        binding.totalExpenses.text = String.format("%,.0f $", expenseValue)
    }

    private fun setupTotalAmountChart(operations: List<Operation>) {
        val income = operations.filter { it.type.lowercase() == "income" }.sumOf { it.amount }
        val expense = operations.filter { it.type.lowercase() == "expense" }.sumOf { it.amount }

        val incomeValue = if (income.isNaN()) 0.0f else income.toFloat()
        val expenseValue = if (expense.isNaN()) 0.0f else expense.toFloat()

        val entries = listOf(
            PieEntry(incomeValue.toFloat(), "Income"),
            PieEntry(expenseValue.toFloat(), "Expense")
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

    private fun setupPieChartForType(
        operations: List<Operation>,
        categories: List<Category>,
        type: String,
        pieChart: PieChart,
        title: String
    ) {
        val ops = operations.filter { it.type.equals(type, ignoreCase = true) }
        val total = ops.sumOf { it.amount }
        val grouped = ops.groupBy { it.category }.mapValues { it.value.sumOf { it.amount } }

        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        grouped.forEach { (category, sum) ->
            if ((sum / total) >= 0.005) {
                entries.add(PieEntry(sum.toFloat(), category))
                colors.add(categories.find { it.name == category }?.color ?: Color.GRAY)
            }
        }

        setupPieChart(pieChart, title, entries, colors, 170, 170)
    }

    fun setupPieChart(pieChart: PieChart, centerText: String, entries: List<PieEntry>, colors: List<Int>, heightDP: Int, widthDP: Int) {
        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 1f
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

    private fun addCategoryItem(parent: LinearLayout, name: String, amount: Double, currency: String, isIncome: Boolean) {
        val inflater = layoutInflater
        val itemView = inflater.inflate(R.layout.item_category, parent, false)

        val nameText = itemView.findViewById<TextView>(R.id.name_category)
        val amountText = itemView.findViewById<TextView>(R.id.amount_category)
        val currencyText = itemView.findViewById<TextView>(R.id.currency_category)

        nameText.text = "$name:"
        amountText.text = String.format("%,.0f", amount)
        currencyText.text = currency

        val color = if (isIncome) {
            ContextCompat.getColor(this, R.color.income_green)
        } else {
            ContextCompat.getColor(this, R.color.expense_red)
        }

        nameText.setTextColor(color)
        amountText.setTextColor(color)
        currencyText.setTextColor(color)

        parent.addView(itemView)
    }

    private fun populateTopCategories(operations: List<Operation>) {
        val container = binding.topCategories

        container.removeAllViews()

        val title = TextView(this).apply {
            text = "Important categories:"
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.primary_color))
            typeface = ResourcesCompat.getFont(context, R.font.lexend_deca)
            setPadding(dpToPx(10), 0, 0, 0)
        }
        container.addView(title)

        val incomeTop = operations
            .filter { it.type.lowercase() == "income" }
            .groupBy { it.category }
            .mapValues { (_, ops) -> ops.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(3)

        incomeTop.forEach { (category, sum) ->
            addCategoryItem(container, category, sum, "$", isIncome = true)
        }

        val expenseTop = operations
            .filter { it.type.lowercase() == "expense" }
            .groupBy { it.category }
            .mapValues { (_, ops) -> ops.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(3)

        expenseTop.forEach { (category, sum) ->
            addCategoryItem(container, category, sum, "$", isIncome = false)
        }
    }

}