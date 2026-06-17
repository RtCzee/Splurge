package com.example.splurge.ui.graph

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.splurge.R
import com.example.splurge.data.PrivacyPreferences
import com.example.splurge.data.local.AppDatabase
import com.example.splurge.data.local.CategorySpendTotal
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CategorySpendingGraph"

class CategorySpendingGraphFragment : Fragment() {

    private var _root: View? = null
    private val root get() = _root!!

    private lateinit var barChart: BarChart
    private lateinit var progressBar: View
    private lateinit var tvEmptyState: TextView
    private lateinit var tvDateRangeDisplay: TextView
    private lateinit var tvTotalSpending: TextView
    private lateinit var privacyPreferences: PrivacyPreferences

    private var currentStartDate: String = ""
    private var currentEndDate: String = ""

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            _root = inflater.inflate(R.layout.fragment_category_spending_graph, container, false)
            _root
        } catch (e: Exception) {
            Log.e(TAG, "Error inflating layout", e)
            Toast.makeText(requireContext(), "Error loading graph layout", Toast.LENGTH_SHORT).show()
            TextView(requireContext()).apply {
                text = "Error loading graph: ${e.message}"
                setPadding(50, 50, 50, 50)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            // Initialize views
            barChart = view.findViewById(R.id.bar_chart)
            progressBar = view.findViewById(R.id.progress_bar)
            tvEmptyState = view.findViewById(R.id.tv_empty_state)
            tvDateRangeDisplay = view.findViewById(R.id.tv_date_range_display)
            tvTotalSpending = view.findViewById(R.id.tv_total_spending)
            privacyPreferences = PrivacyPreferences(requireContext())

            setupToolbar(view)
            setupBarChart()
            setupButtons(view)

            // Load current month data by default
            loadCurrentMonthData()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupToolbar(view: View) {
        try {
            val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            toolbar.setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up toolbar", e)
        }
    }

    private fun setupBarChart() {
        try {
            barChart.apply {
                description.isEnabled = false
                setDrawGridBackground(false)
                setDrawBarShadow(false)
                setDrawValueAboveBar(true)
                setPinchZoom(false)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    labelRotationAngle = -45f
                    textSize = 11f
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    axisMinimum = 0f
                    textSize = 11f
                    valueFormatter = CurrencyValueFormatter()
                }

                axisRight.isEnabled = false
                legend.isEnabled = true

                animateY(500)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up bar chart", e)
        }
    }

    private fun setupButtons(view: View) {
        try {
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_this_month)?.setOnClickListener {
                loadCurrentMonthData()
            }

            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_this_week)?.setOnClickListener {
                loadCurrentWeekData()
            }

            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_last_3_months)?.setOnClickListener {
                loadLastThreeMonthsData()
            }

            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_last_6_months)?.setOnClickListener {
                loadLastSixMonthsData()
            }

            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_custom_range)?.setOnClickListener {
                showCustomDateRangePicker()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up buttons", e)
        }
    }

    private fun loadCurrentMonthData() {
        try {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1

            val firstDay = String.format("%d-%02d-01", year, month)
            val lastDay = String.format("%d-%02d-%02d", year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))

            currentStartDate = firstDay
            currentEndDate = lastDay

            val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
            tvDateRangeDisplay.text = "$monthName ($firstDay to $lastDay)"

            loadCategoryData(firstDay, lastDay)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading current month data", e)
            showErrorMessage("Error loading month data: ${e.message}")
        }
    }

    private fun loadCurrentWeekData() {
        try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            currentStartDate = dateFormat.format(calendar.time)

            calendar.add(Calendar.DAY_OF_WEEK, 6)
            currentEndDate = dateFormat.format(calendar.time)

            tvDateRangeDisplay.text = "${displayDateFormat.format(dateFormat.parse(currentStartDate)!!)} - ${displayDateFormat.format(dateFormat.parse(currentEndDate)!!)}"

            loadCategoryData(currentStartDate, currentEndDate)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading current week data", e)
            showErrorMessage("Error loading week data: ${e.message}")
        }
    }

    private fun loadLastThreeMonthsData() {
        try {
            val endCalendar = Calendar.getInstance()
            currentEndDate = dateFormat.format(endCalendar.time)

            val startCalendar = Calendar.getInstance()
            startCalendar.add(Calendar.MONTH, -3)
            currentStartDate = dateFormat.format(startCalendar.time)

            tvDateRangeDisplay.text = "${displayDateFormat.format(dateFormat.parse(currentStartDate)!!)} - ${displayDateFormat.format(dateFormat.parse(currentEndDate)!!)}"

            loadCategoryData(currentStartDate, currentEndDate)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading last 3 months data", e)
            showErrorMessage("Error loading data: ${e.message}")
        }
    }

    private fun loadLastSixMonthsData() {
        try {
            val endCalendar = Calendar.getInstance()
            currentEndDate = dateFormat.format(endCalendar.time)

            val startCalendar = Calendar.getInstance()
            startCalendar.add(Calendar.MONTH, -6)
            currentStartDate = dateFormat.format(startCalendar.time)

            tvDateRangeDisplay.text = "${displayDateFormat.format(dateFormat.parse(currentStartDate)!!)} - ${displayDateFormat.format(dateFormat.parse(currentEndDate)!!)}"

            loadCategoryData(currentStartDate, currentEndDate)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading last 6 months data", e)
            showErrorMessage("Error loading data: ${e.message}")
        }
    }

    private fun showCustomDateRangePicker() {
        try {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(requireContext(), { _, startYear, startMonth, startDay ->
                val startDate = String.format("%d-%02d-%02d", startYear, startMonth + 1, startDay)

                DatePickerDialog(requireContext(), { _, endYear, endMonth, endDay ->
                    val endDate = String.format("%d-%02d-%02d", endYear, endMonth + 1, endDay)
                    currentStartDate = startDate
                    currentEndDate = endDate
                    tvDateRangeDisplay.text = "${displayDateFormat.format(dateFormat.parse(startDate)!!)} - ${displayDateFormat.format(dateFormat.parse(endDate)!!)}"
                    loadCategoryData(startDate, endDate)
                }, year, month, day).show()
            }, year, month, day).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing date picker", e)
            showErrorMessage("Error: ${e.message}")
        }
    }

    private fun loadCategoryData(startDate: String, endDate: String) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val database = AppDatabase.getInstance(requireContext())
                val categoryTotals = database.expenseDao().getCategoryTotalsForDateRange(startDate, endDate)

                requireActivity().runOnUiThread {
                    updateChart(categoryTotals)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading category data", e)
                requireActivity().runOnUiThread {
                    showErrorMessage("Error loading data: ${e.message}")
                }
            }
        }
    }

    private fun updateChart(categoryTotals: List<CategorySpendTotal>) {
        progressBar.visibility = View.GONE

        if (privacyPreferences.isHideBalancesEnabled()) {
            barChart.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
            tvEmptyState.text = getString(R.string.privacy_hidden_graph_message)
            tvTotalSpending.text = getString(R.string.privacy_hidden_value)
            return
        }

        val nonZeroTotals = categoryTotals.filter { it.totalAmount > 0 }

        if (nonZeroTotals.isEmpty()) {
            barChart.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
            tvEmptyState.text = "No expense data for this period.\nAdd expenses to see spending analytics."
            tvTotalSpending.text = formatCurrency(0.0)
            return
        }

        barChart.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE

        val categoryNames = nonZeroTotals.map { it.categoryName }
        val entries = ArrayList<BarEntry>()
        var totalSpending = 0.0

        nonZeroTotals.forEachIndexed { index, total ->
            entries.add(BarEntry(index.toFloat(), total.totalAmount.toFloat()))
            totalSpending += total.totalAmount
        }

        tvTotalSpending.text = formatCurrency(totalSpending)

        val barDataSet = BarDataSet(entries, "Amount Spent")
        barDataSet.color = requireContext().getColor(R.color.auth_primary)
        barDataSet.valueTextSize = 11f
        barDataSet.valueFormatter = CurrencyValueFormatter()

        val barData = BarData(barDataSet)
        barData.barWidth = 0.7f

        barChart.data = barData
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(categoryNames)
        barChart.invalidate()
    }

    private fun showErrorMessage(message: String) {
        progressBar.visibility = View.GONE
        barChart.visibility = View.GONE
        tvEmptyState.visibility = View.VISIBLE
        tvEmptyState.text = message
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA")).format(amount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _root = null
    }

    inner class CurrencyValueFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return String.format("R %.0f", value)
        }
    }
}
