package com.example.splurge.ui.graph

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.splurge.data.local.AppDatabase
import com.example.splurge.data.local.BudgetGoalEntity
import com.example.splurge.data.local.CategorySpendTotal
import java.text.SimpleDateFormat
import java.util.*

//ViewModel that powers the category spending graph screen.
//Handles date range selection, category filtering, and budget goals.

class CategorySpendingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val expenseDao = database.expenseDao()
    private val budgetGoalDao = database.budgetGoalDao()
    private val categoryDao = database.categoryDao()

    // LiveData for UI state
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _spendingData = MutableLiveData<List<CategorySpendingData>>(emptyList())
    val spendingData: LiveData<List<CategorySpendingData>> = _spendingData

    private val _selectedCategories = MutableLiveData<Set<Long>>(emptySet())
    val selectedCategories: LiveData<Set<Long>> = _selectedCategories

    private val _dateRangeText = MutableLiveData<String>()
    val dateRangeText: LiveData<String> = _dateRangeText

    private val _availableCategories = MutableLiveData<List<Pair<Long, String>>>(emptyList())
    val availableCategories: LiveData<List<Pair<Long, String>>> = _availableCategories

    // Current date range state
    private var currentStartDate: String = ""
    private var currentEndDate: String = ""
    private var currentMonthKey: String? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    init {
        // Set default to current month
        setToCurrentMonth()
    }

    //Sets the date range to the current month.
    fun setToCurrentMonth() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        currentMonthKey = String.format("%d-%02d", year, month)

        _dateRangeText.value = displayCurrentMonthRange()
        loadDataForMonth(currentMonthKey!!)
    }

    //Sets the date range to the current week.
    fun setToCurrentWeek() {
        currentMonthKey = null
        val calendar = Calendar.getInstance()
        // Set to start of week (Monday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        currentStartDate = dateFormat.format(calendar.time)

        // Set to end of week (Sunday)
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        currentEndDate = dateFormat.format(calendar.time)

        _dateRangeText.value = "${displayDateFormat.format(dateFormat.parse(currentStartDate)!!)} - ${displayDateFormat.format(dateFormat.parse(currentEndDate)!!)}"
        loadDataForDateRange(currentStartDate, currentEndDate)
    }

    //Sets the date range to the last 3 months.
    fun setToLastThreeMonths() {
        currentMonthKey = null
        val endCalendar = Calendar.getInstance()
        currentEndDate = dateFormat.format(endCalendar.time)

        val startCalendar = Calendar.getInstance()
        startCalendar.add(Calendar.MONTH, -3)
        currentStartDate = dateFormat.format(startCalendar.time)

        _dateRangeText.value = "${displayDateFormat.format(dateFormat.parse(currentStartDate)!!)} - ${displayDateFormat.format(dateFormat.parse(currentEndDate)!!)}"
        loadDataForDateRange(currentStartDate, currentEndDate)
    }

    //Sets the date range to the last 6 months.
    fun setToLastSixMonths() {
        currentMonthKey = null
        val endCalendar = Calendar.getInstance()
        currentEndDate = dateFormat.format(endCalendar.time)

        val startCalendar = Calendar.getInstance()
        startCalendar.add(Calendar.MONTH, -6)
        currentStartDate = dateFormat.format(startCalendar.time)

        _dateRangeText.value = "${displayDateFormat.format(dateFormat.parse(currentStartDate)!!)} - ${displayDateFormat.format(dateFormat.parse(currentEndDate)!!)}"
        loadDataForDateRange(currentStartDate, currentEndDate)
    }

    //Sets a custom date range.
    fun setCustomDateRange(startDate: String, endDate: String) {
        currentMonthKey = null
        currentStartDate = startDate
        currentEndDate = endDate

        _dateRangeText.value = "${displayDateFormat.format(dateFormat.parse(startDate)!!)} - ${displayDateFormat.format(dateFormat.parse(endDate)!!)}"
        loadDataForDateRange(startDate, endDate)
    }

    //Loads spending data for a specific month (uses monthKey like "2026-04").
    private fun loadDataForMonth(monthKey: String) {
        _isLoading.value = true
        try {
            // Get category totals for the month
            val categoryTotals = expenseDao.getCategoryTotalsForMonth("$monthKey-%")

            // Get budget goals for this month
            val budgetGoal = budgetGoalDao.getGoalForMonth(monthKey)

            // Combine into spending data
            val spendingDataList = categoryTotals.map { total ->
                CategorySpendingData(
                    categoryId = total.categoryId,
                    categoryName = total.categoryName,
                    totalSpent = total.totalAmount,
                    entryCount = total.entryCount,
                    minGoal = budgetGoal?.minimumGoal,
                    maxGoal = budgetGoal?.maximumGoal
                )
            }

            _spendingData.value = spendingDataList

            // Initialize selected categories (all by default)
            if (_selectedCategories.value.isNullOrEmpty()) {
                _selectedCategories.value = spendingDataList.map { it.categoryId }.toSet()
            }

            // Update available categories list
            _availableCategories.value = spendingDataList.map { it.categoryId to it.categoryName }

        } finally {
            _isLoading.value = false
        }
    }

    //Loads spending data for a custom date range.
    private fun loadDataForDateRange(startDate: String, endDate: String) {
        _isLoading.value = true
        try {
            // Get category totals for the date range
            val categoryTotals = expenseDao.getCategoryTotalsForDateRange(startDate, endDate)

            // For custom ranges, we don't show min/max goals (they're month-specific)
            // But we still get the overall spending data
            val spendingDataList = categoryTotals.map { total ->
                CategorySpendingData(
                    categoryId = total.categoryId,
                    categoryName = total.categoryName,
                    totalSpent = total.totalAmount,
                    entryCount = total.entryCount,
                    minGoal = null,  // Goals are month-specific
                    maxGoal = null
                )
            }

            _spendingData.value = spendingDataList

            // Initialize selected categories (all by default)
            if (_selectedCategories.value.isNullOrEmpty()) {
                _selectedCategories.value = spendingDataList.map { it.categoryId }.toSet()
            }

            // Update available categories list
            _availableCategories.value = spendingDataList.map { it.categoryId to it.categoryName }

        } finally {
            _isLoading.value = false
        }
    }

    //Toggles a category's visibility on the graph.
    fun toggleCategory(categoryId: Long) {
        val currentSet = _selectedCategories.value?.toMutableSet() ?: mutableSetOf()
        if (currentSet.contains(categoryId)) {
            currentSet.remove(categoryId)
        } else {
            currentSet.add(categoryId)
        }
        _selectedCategories.value = currentSet
    }

    //Selects all categories.
    fun selectAllCategories() {
        _spendingData.value?.let { data ->
            _selectedCategories.value = data.map { it.categoryId }.toSet()
        }
    }

    //Deselects all categories.
    fun deselectAllCategories() {
        _selectedCategories.value = emptySet()
    }

    //Returns filtered spending data based on selected categories.
    fun getFilteredSpendingData(): List<CategorySpendingData> {
        val selected = _selectedCategories.value ?: return emptyList()
        return _spendingData.value?.filter { selected.contains(it.categoryId) } ?: emptyList()
    }

    //Checks if any spending data exists.
    fun hasData(): Boolean {
        return !(_spendingData.value.isNullOrEmpty())
    }

    //Gets total spending for the selected period.
    fun getTotalSpending(): Double {
        return getFilteredSpendingData().sumOf { it.totalSpent }
    }

    private fun displayCurrentMonthRange(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

        // Get first day of month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDay = displayDateFormat.format(calendar.time)

        // Get last day of month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val lastDay = displayDateFormat.format(calendar.time)

        return "$monthName ($firstDay - $lastDay)"
    }
}