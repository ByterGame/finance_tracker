package com.example.finance_tracker_app

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.MPPointF
import androidx.appcompat.app.AppCompatDelegate
import com.example.finance_tracker_app.AddCardActivity.Card
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response




class DashboardActivity : AppCompatActivity() {


    private lateinit var lineChart: LineChart
    private lateinit var startDateInput: EditText
    private lateinit var endDateInput: EditText
    private lateinit var typeGraphSpinner: Spinner

    private var startDateMillis: Long = 0
    private var endDateMillis: Long = 0
    private var currentGraphType = GraphType.ALL

    private lateinit var operationDao: OperationDao
    private var allOperations = listOf<Operation>()

    private val PREFS_NAME = "dashboard_prefs"
    private val KEY_START_DATE = "start_date"
    private val KEY_END_DATE = "end_date"
    private val KEY_GRAPH_TYPE_POS = "graph_type_position"
    private var mainCurrency =  ""


    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_theme", false)
        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
        mainCurrency = getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("main_currency", "USD") ?: "USD"
        ExchangeRatesManager.loadFromPrefs(this)

        val hasInternet = checkInternetConnection()

        if (hasInternet) {
            if (ExchangeRatesManager.isUpdateNeeded()) {
                ExchangeRateClient.api.getRates(mainCurrency).enqueue(object : Callback<ErApiResponse> {
                    override fun onResponse(call: Call<ErApiResponse>, response: Response<ErApiResponse>) {
                        val data = response.body()
                        if (data != null && data.result == "success" && data.rates != null) {
                            ExchangeRatesManager.updateRates(data.rates, data.lastUpdate, data.nextUpdate)
                            ExchangeRatesManager.saveToPrefs(this@DashboardActivity)
                        }
                    }
                    override fun onFailure(call: Call<ErApiResponse>, t: Throwable) {

                    }
                })
            }
        } else {
            if (ExchangeRatesManager.isUpdateNeeded()) {
                runOnUiThread {
                    Toast.makeText(this, "Вы используете устаревшие курсы валют. Пожалуйста, подключитесь к интернету для обновления.", Toast.LENGTH_LONG).show()
                }
            }
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_layout)

        lineChart = findViewById(R.id.line_chart)
        startDateInput = findViewById(R.id.start_data_graph)
        endDateInput = findViewById(R.id.end_data_graph)
        typeGraphSpinner = findViewById(R.id.graph_type)
        val detailedStatisticButton = findViewById<LinearLayout>(R.id.detailed_statistic_button)

        detailedStatisticButton.setOnClickListener {
            startActivity(Intent(this@DashboardActivity, DetailedStatisticsActivity::class.java))
            finish()
        }

        val defaultCal = Calendar.getInstance().apply {
            set(2025, Calendar.JUNE, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val defaultStartDate = defaultCal.timeInMillis
        defaultCal.set(2025, Calendar.JUNE, 1, 23, 59, 59)
        val defaultEndDate = defaultCal.timeInMillis

        val prefs_dash = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        startDateMillis = prefs_dash.getLong(KEY_START_DATE, defaultStartDate)
        endDateMillis = prefs_dash.getLong(KEY_END_DATE, defaultEndDate)
        val savedGraphPos = prefs_dash.getInt(KEY_GRAPH_TYPE_POS, 4)

        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        startDateInput.setText(sdf.format(Date(startDateMillis)))
        endDateInput.setText(sdf.format(Date(endDateMillis)))

        setupDatePickers()
        setupTypeGraphSpinner()
        typeGraphSpinner.setSelection(savedGraphPos)
        initChartConfig()

        operationDao = AppDatabase.getDatabase(this).operationDao()
        lifecycleScope.launch {
            allOperations = operationDao.getAllOperations()
            withContext(Dispatchers.Main) {
                updateChart()
            }
        }
        runOnUiThread { initCardsUI() }
        setupSettingsButton()
    }

    private fun setupSettingsButton() {
        val settingsBtn = findViewById<LinearLayout>(R.id.settings_section)
        settingsBtn.setOnClickListener {
            startActivity(Intent(this@DashboardActivity, SettingsActivity::class.java))
            finish()
        }
    }

    fun checkInternetConnection(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun initChartConfig() {
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.setNoDataText("No data to display")
        lineChart.setNoDataTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.primary_color))
        lineChart.animateX(1000)

        lineChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = ContextCompat.getColor(this@DashboardActivity, R.color.primary_color)
            textSize = 12f
            setDrawGridLines(false)
            granularity = 1f
        }

        lineChart.axisLeft.apply {
            textColor = ContextCompat.getColor(this@DashboardActivity, R.color.primary_color)
            textSize = 12f
            setDrawGridLines(true)
        }

        lineChart.axisRight.isEnabled = false

        lineChart.legend.apply {
            isEnabled = true
            form = Legend.LegendForm.LINE
            textSize = 12f
            textColor = ContextCompat.getColor(this@DashboardActivity, R.color.primary_color)
        }
    }

    private fun initCardsUI() {
        val totalValue = findViewById<TextView>(R.id.total_value)
        val anyCardContainer = findViewById<FrameLayout>(R.id.any_card)
        val allCards = findViewById<FrameLayout>(R.id.all_cards_button)
        val addBtn = findViewById<LinearLayout>(R.id.add_operation_section)

        val currencySymbolMap = mapOf(
            "USD" to "$",
            "EUR" to "€",
            "GBP" to "£",
            "JPY" to "¥",
            "CNY" to "¥",
            "RUB" to "₽",
            "INR" to "₹",
            "AUD" to "A$",
            "CAD" to "C$",
            "CHF" to "₣",
            "TRY" to "₺"
        )

        anyCardContainer.removeAllViews()
        allCards.setOnClickListener {
            startActivity(Intent(this, AllCardsActivity::class.java))
            finish()
        }
        addBtn.setOnClickListener {
            startActivity(Intent(this, AddOperationActivity::class.java))
            finish()
        }
        var totalValueSum = 0.0
        val cards = loadCards()
        for (card in cards) {
            val fromCurrency = card.currency ?: continue
            val rateFrom = ExchangeRatesManager.getRate(fromCurrency)
            val rateTo = ExchangeRatesManager.getRate(mainCurrency)
            if (rateFrom != 0.0) {
                val converted = card.balance / rateFrom * rateTo
                totalValueSum += converted
            }
        }
        val formattedTotalAmount = String.format("%,.0f", totalValueSum)
        val currencyTotalAmount = currencySymbolMap[mainCurrency]
        totalValue.text = "$formattedTotalAmount $currencyTotalAmount"

        if (cards.isEmpty()) {
            allCards.isEnabled = false
            val tv = TextView(this).apply {
                text = "+ Add your first card"
                textSize = 21f
                setTextColor(resources.getColor(R.color.theme_color))
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setPadding(16,16,16,16)
                setOnClickListener {
                    startActivity(Intent(this@DashboardActivity, AddCardActivity::class.java))
                }
            }
            anyCardContainer.addView(tv, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        } else {
            allCards.isEnabled = true
            val card = cards[0]
            val view = layoutInflater.inflate(R.layout.card_item, anyCardContainer, false)
            view.findViewById<TextView>(R.id.card_type_and_name).text = "Card name: ${card.name}"

            val currencySymbol = currencySymbolMap[card.currency]
            val formattedBalance = String.format("%,.0f", card.balance)
            view.findViewById<TextView>(R.id.card_balance).text = "$formattedBalance $currencySymbol"
            view.findViewById<ImageView>(R.id.card_icon).setImageResource(R.drawable.card)
            anyCardContainer.addView(view)
        }
    }

    private fun setupDatePickers() {
        val cal = Calendar.getInstance()
        val onStart = DatePickerDialog.OnDateSetListener { _, y, m, d ->
            cal.set(y, m, d, 0, 0, 0)
            startDateMillis = cal.timeInMillis
            startDateInput.setText("%02d.%02d.%d".format(d, m+1, y))
            savePrefs()
            checkDates()
        }
        val onEnd = DatePickerDialog.OnDateSetListener { _, y, m, d ->
            cal.set(y, m, d, 23, 59, 59)
            endDateMillis = cal.timeInMillis
            endDateInput.setText("%02d.%02d.%d".format(d, m+1, y))
            savePrefs()
            checkDates()
        }
        startDateInput.setOnClickListener {
            cal.time = Date()
            DatePickerDialog(this, onStart, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
        endDateInput.setOnClickListener {
            cal.time = Date()
            DatePickerDialog(this, onEnd, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun checkDates() {
        if (startDateMillis != 0L && endDateMillis != 0L && endDateMillis < startDateMillis) {
            AlertDialog.Builder(this)
                .setTitle("Invalid dates")
                .setMessage("End date can't be before start date.")
                .setPositiveButton("OK") { d, _ -> d.dismiss() }
                .show()
            endDateMillis = 0
            endDateInput.text.clear()
        } else updateChart()
    }

    private fun setupTypeGraphSpinner() {
        val types = listOf("Select type", "Income only", "Expense only", "Net (I‑E)", "All")
        val adapter = createStyledSpinnerAdapter(types)
        typeGraphSpinner.adapter = adapter
        typeGraphSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(a: AdapterView<*>, v: View?, pos: Int, id: Long) {
                if (pos == 0) {
                    return
                }
                currentGraphType = GraphType.values()[pos - 1]
                savePrefs(selectedSpinnerPos = pos)
                updateChart()
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }
    }

    private fun savePrefs(selectedSpinnerPos: Int? = null) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putLong(KEY_START_DATE, startDateMillis)
            putLong(KEY_END_DATE, endDateMillis)
            if (selectedSpinnerPos != null) putInt(KEY_GRAPH_TYPE_POS, selectedSpinnerPos)
            apply()
        }
    }

    private fun createStyledSpinnerAdapter(
        items: List<String>,
        highlightLastItem: Boolean = false
    ): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(this, R.layout.spinner_item, R.id.spinnerText, items) {

            override fun isEnabled(position: Int): Boolean = position != 0

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(R.id.spinnerText)
                if (position == 0) {
                    textView.setTextColor(resources.getColor(android.R.color.darker_gray))
                } else {
                    textView.setTextColor(resources.getColor(R.color.theme_color))
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val inflater = LayoutInflater.from(context)
                val view = inflater.inflate(R.layout.spinner_dropdown_item, parent, false)
                val textView = view.findViewById<TextView>(R.id.dropdownText)
                textView.text = items[position]

                val color = when {
                    position == 0 -> resources.getColor(android.R.color.darker_gray)
                    highlightLastItem && position == items.lastIndex -> resources.getColor(android.R.color.holo_blue_dark)
                    else -> resources.getColor(R.color.theme_color)
                }
                textView.setTextColor(color)
                return view
            }
        }
    }

    private fun updateChart() {
        if (allOperations.isEmpty()) return
        val ops = allOperations.filter {
            (startDateMillis == 0L || it.date >= startDateMillis) &&
                    (endDateMillis == 0L || it.date <= endDateMillis)
        }

        val byDate = ops.groupBy {
            val c = Calendar.getInstance()
            c.timeInMillis = it.date
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis
        }

        val dates = byDate.keys.sorted()
        val inc = mutableListOf<Entry>()
        val exp = mutableListOf<Entry>()
        dates.forEachIndexed { idx, ts ->
            val list = byDate[ts]!!
            val iSum = list.filter { it.type.equals("Income", true) }.sumOf {
                val rateFrom = ExchangeRatesManager.getRate(it.currency)
                val rateTo = ExchangeRatesManager.getRate(mainCurrency)
                if (rateFrom != 0.0) it.amount / rateFrom * rateTo else 0.0
            }.toFloat()
            val eSum = list.filter { it.type.equals("Expense", true) }.sumOf {
                val rateFrom = ExchangeRatesManager.getRate(it.currency)
                val rateTo = ExchangeRatesManager.getRate(mainCurrency)
                if (rateFrom != 0.0) it.amount / rateFrom * rateTo else 0.0
            }.toFloat()
            inc.add(Entry(idx.toFloat(), iSum))
            exp.add(Entry(idx.toFloat(), eSum))
        }

        val sets = mutableListOf<ILineDataSet>()

        fun styledSet(entries: List<Entry>, label: String, color: Int): LineDataSet {
            return LineDataSet(entries, label).apply {
                this.color = color
                lineWidth = 2f
                mode = LineDataSet.Mode.CUBIC_BEZIER
                cubicIntensity = 0.2f
                setDrawCircles(false)
                setDrawValues(false)
                setDrawFilled(true)
                fillDrawable = ContextCompat.getDrawable(this@DashboardActivity, R.drawable.chart_gradient)
            }
        }

        if (currentGraphType == GraphType.INCOME || currentGraphType == GraphType.ALL)
            sets.add(styledSet(inc, "Income", 0xFF4CAF50.toInt()))

        if (currentGraphType == GraphType.EXPENSE || currentGraphType == GraphType.ALL)
            sets.add(styledSet(exp, "Expense", 0xFFF44336.toInt()))

        if (currentGraphType == GraphType.NET || currentGraphType == GraphType.ALL) {
            val net = inc.zip(exp).map { Entry(it.first.x, it.first.y - it.second.y) }
            sets.add(styledSet(net, "Net", 0xFF2196F3.toInt()))
        }

        lineChart.xAxis.apply {
            granularity = 1f
            valueFormatter = object: ValueFormatter() {
                private val df = SimpleDateFormat("dd.MM", Locale.getDefault())
                override fun getFormattedValue(v: Float) =
                    df.format(Date(dates.getOrNull(v.toInt()) ?: 0L))
            }
        }

        val minY = sets.minOf { it.yMin }.coerceAtMost(0f)
        val maxY = sets.maxOf { it.yMax }
        val padding = (maxY - minY) * 0.1f
        lineChart.axisLeft.axisMinimum = (minY - padding).coerceAtMost(0f)
        lineChart.axisLeft.axisMaximum = maxY + padding

        lineChart.highlightValues(null)
        lineChart.marker = null
        lineChart.data = LineData(sets)
        lineChart.invalidate()
        lineChart.xAxis.valueFormatter = object : ValueFormatter() {
            private val df = SimpleDateFormat("dd.MM", Locale.getDefault())
            override fun getFormattedValue(v: Float): String {
                return df.format(Date(dates.getOrNull(v.toInt()) ?: 0L))
            }
        }

        if (currentGraphType == GraphType.ALL) {
            lineChart.marker = null
        } else {
            lineChart.marker = MyMarkerView(this, R.layout.custom_marker_view, mainCurrency)
        }
        lineChart.invalidate()
    }

    class MyMarkerView(
        context: Context,
        layoutResource: Int,
        private val mainCurrency: String
    ) : MarkerView(context, layoutResource) {

        private val textView: TextView = findViewById(R.id.marker_text)

        private val currencySymbolMap = mapOf(
            "USD" to "$", "EUR" to "€", "GBP" to "£", "JPY" to "¥",
            "CNY" to "¥", "RUB" to "₽", "INR" to "₹", "AUD" to "A$",
            "CAD" to "C$", "CHF" to "₣", "TRY" to "₺"
        )

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            if (e != null) {
                val symbol = currencySymbolMap[mainCurrency] ?: mainCurrency
                textView.text = "${e.y.toInt()} $symbol"
                super.refreshContent(e, highlight)
            }
        }

        override fun getOffset(): MPPointF {
            return MPPointF(-(width / 2f), -height.toFloat())
        }
    }


    private fun loadCards(): List<Card> {
        return try {
            val key = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            val prefs = EncryptedSharedPreferences.create(this, "cards_secure_prefs",
                key, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
            prefs.getString("cards_list", null)?.let {
                Gson().fromJson(it, object: TypeToken<List<Card>>(){}.type)
            } ?: emptyList()
        } catch(e: Exception) { emptyList() }
    }

    enum class GraphType { INCOME, EXPENSE, NET, ALL }
}

