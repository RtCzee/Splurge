package com.example.splurge.ui.transactions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.splurge.R
import com.example.splurge.data.FinanceRepository
import com.example.splurge.ui.base.BaseActivity
import com.example.splurge.ui.common.FinanceUiFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class Transactions : BaseActivity() {
    private lateinit var repository: FinanceRepository
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var categoryTotalsAdapter: CategoryTotalsAdapter
    private var selectedStartDate: LocalDate = YearMonth.now().atDay(1)
    private var selectedEndDate: LocalDate = LocalDate.now()
    private var pendingExpensePhotoUri: Uri? = null
    private var activePhotoPreview: ImageView? = null
    private var activePhotoLabel: TextView? = null
    private var activeRemovePhotoButton: MaterialButton? = null
    private var showingTotalsTab: Boolean = false

    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            return@registerForActivityResult
        }
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }
        pendingExpensePhotoUri = uri
        updatePhotoPreview()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_transactions)
        repository = FinanceRepository.getInstance(this)
        expenseAdapter = ExpenseAdapter(::showPhotoPreview)
        categoryTotalsAdapter = CategoryTotalsAdapter()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.transactions_recycler).apply {
            layoutManager = LinearLayoutManager(this@Transactions)
            adapter = expenseAdapter
        }
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.category_totals_recycler).apply {
            layoutManager = LinearLayoutManager(this@Transactions)
            adapter = categoryTotalsAdapter
        }

        setupTabs()
        setupFilters()
        setupActions()
        bindTransactionData()
        setupBottomNavigation(R.id.navigation_transactions)
    }

    override fun onResume() {
        super.onResume()
        bindTransactionData()
    }

    private fun setupTabs() {
        val entriesRecycler = findViewById<View>(R.id.transactions_recycler)
        val totalsRecycler = findViewById<View>(R.id.category_totals_recycler)
        val emptyText = findViewById<TextView>(R.id.empty_transactions_text)

        findViewById<TabLayout>(R.id.tab_layout).addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    showingTotalsTab = tab.position == 1
                    entriesRecycler.visibility = if (showingTotalsTab) View.GONE else View.VISIBLE
                    totalsRecycler.visibility = if (showingTotalsTab) View.VISIBLE else View.GONE
                    emptyText.text = getString(
                        if (showingTotalsTab) R.string.no_category_totals_message else R.string.no_expenses_message
                    )
                    bindTransactionData()
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit

                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            }
        )
    }

    private fun setupFilters() {
        findViewById<MaterialButton>(R.id.start_period_button).setOnClickListener {
            showDatePicker(selectedStartDate) { selectedDate ->
                selectedStartDate = selectedDate
                if (selectedStartDate.isAfter(selectedEndDate)) {
                    selectedEndDate = selectedDate
                }
                bindTransactionData()
            }
        }
        findViewById<MaterialButton>(R.id.end_period_button).setOnClickListener {
            showDatePicker(selectedEndDate) { selectedDate ->
                selectedEndDate = selectedDate
                if (selectedEndDate.isBefore(selectedStartDate)) {
                    selectedStartDate = selectedDate
                }
                bindTransactionData()
            }
        }
    }

    private fun setupActions() {
        findViewById<FloatingActionButton>(R.id.add_transaction_fab).setOnClickListener {
            if (repository.getCategoryCount() == 0) {
                Toast.makeText(this, R.string.create_category_first_message, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showAddExpenseDialog()
        }
    }

    private fun bindTransactionData() {
        val startDateValue = FinanceUiFormatter.formatDate(selectedStartDate)
        val endDateValue = FinanceUiFormatter.formatDate(selectedEndDate)
        val expenses = repository.getExpensesForPeriod(startDateValue, endDateValue)
        val categoryTotals = repository.getCategoryTotalsForPeriod(startDateValue, endDateValue)
        val totalSpent = repository.getTotalSpentForPeriod(startDateValue, endDateValue)
        val expenseCount = repository.getExpenseCountForPeriod(startDateValue, endDateValue)

        findViewById<TextView>(R.id.selected_period_label).text = getString(
            R.string.selected_period_value,
            FinanceUiFormatter.formatDisplayDate(startDateValue),
            FinanceUiFormatter.formatDisplayDate(endDateValue)
        )
        findViewById<TextView>(R.id.period_entry_count).text = resources.getQuantityString(
            R.plurals.expense_entry_count,
            expenseCount,
            expenseCount
        )
        findViewById<TextView>(R.id.period_total_amount).text = FinanceUiFormatter.formatCurrency(totalSpent)
        findViewById<MaterialButton>(R.id.start_period_button).text =
            FinanceUiFormatter.formatDisplayDate(startDateValue)
        findViewById<MaterialButton>(R.id.end_period_button).text =
            FinanceUiFormatter.formatDisplayDate(endDateValue)

        expenseAdapter.submitList(expenses)
        categoryTotalsAdapter.submitList(categoryTotals)

        val emptyText = findViewById<TextView>(R.id.empty_transactions_text)
        val isEmpty = if (showingTotalsTab) {
            categoryTotals.isEmpty()
        } else {
            expenses.isEmpty()
        }
        emptyText.text = getString(
            if (showingTotalsTab) R.string.no_category_totals_message else R.string.no_expenses_message
        )
        emptyText.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showAddExpenseDialog() {
        val categories = repository.getCategories()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_expense_entry, null, false)
        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.expense_amount_input)
        val dateInput = dialogView.findViewById<TextInputEditText>(R.id.expense_date_input)
        val startTimeInput = dialogView.findViewById<TextInputEditText>(R.id.expense_start_time_input)
        val endTimeInput = dialogView.findViewById<TextInputEditText>(R.id.expense_end_time_input)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.expense_description_input)
        val categoryInput = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.expense_category_input)
        val selectPhotoButton = dialogView.findViewById<MaterialButton>(R.id.select_photo_button)
        val removePhotoButton = dialogView.findViewById<MaterialButton>(R.id.remove_photo_button)
        val photoLabel = dialogView.findViewById<TextView>(R.id.selected_photo_label)
        val photoPreview = dialogView.findViewById<ImageView>(R.id.dialog_photo_preview)

        val selectedDate = arrayOf(LocalDate.now())
        val selectedStartTime = arrayOf(LocalTime.of(9, 0))
        val selectedEndTime = arrayOf(LocalTime.of(10, 0))
        pendingExpensePhotoUri = null
        activePhotoPreview = photoPreview
        activePhotoLabel = photoLabel
        activeRemovePhotoButton = removePhotoButton

        dateInput.setText(FinanceUiFormatter.formatDisplayDate(FinanceUiFormatter.formatDate(selectedDate[0])))
        startTimeInput.setText(FinanceUiFormatter.formatTime(selectedStartTime[0]))
        endTimeInput.setText(FinanceUiFormatter.formatTime(selectedEndTime[0]))
        categoryInput.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                categories.map { it.name }
            )
        )
        updatePhotoPreview()

        dateInput.setOnClickListener {
            showDatePicker(selectedDate[0]) { date ->
                selectedDate[0] = date
                dateInput.setText(FinanceUiFormatter.formatDisplayDate(FinanceUiFormatter.formatDate(date)))
            }
        }
        startTimeInput.setOnClickListener {
            showTimePicker(selectedStartTime[0]) { time ->
                selectedStartTime[0] = time
                startTimeInput.setText(FinanceUiFormatter.formatTime(time))
            }
        }
        endTimeInput.setOnClickListener {
            showTimePicker(selectedEndTime[0]) { time ->
                selectedEndTime[0] = time
                endTimeInput.setText(FinanceUiFormatter.formatTime(time))
            }
        }
        selectPhotoButton.setOnClickListener {
            photoPicker.launch(arrayOf("image/*"))
        }
        removePhotoButton.setOnClickListener {
            pendingExpensePhotoUri = null
            updatePhotoPreview()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_expense_dialog_title)
            .setView(dialogView)
            .setPositiveButton(R.string.save_expense_button, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener {
                clearActivePhotoViews()
            }
            .show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val amount = amountInput.text.toCurrencyValue()
            val description = descriptionInput.text?.toString()?.trim().orEmpty()
            val selectedCategoryName = categoryInput.text?.toString()?.trim().orEmpty()
            val category = categories.firstOrNull { it.name.equals(selectedCategoryName, ignoreCase = true) }

            when {
                amount <= 0.0 -> Toast.makeText(this, R.string.invalid_expense_amount, Toast.LENGTH_SHORT).show()
                description.isEmpty() -> Toast.makeText(this, R.string.invalid_expense_description, Toast.LENGTH_SHORT).show()
                category == null -> Toast.makeText(this, R.string.invalid_expense_category, Toast.LENGTH_SHORT).show()
                selectedEndTime[0].isBefore(selectedStartTime[0]) ->
                    Toast.makeText(this, R.string.invalid_expense_time_range, Toast.LENGTH_SHORT).show()
                else -> {
                    repository.addExpense(
                        amount = amount,
                        date = FinanceUiFormatter.formatDate(selectedDate[0]),
                        startTime = FinanceUiFormatter.formatTime(selectedStartTime[0]),
                        endTime = FinanceUiFormatter.formatTime(selectedEndTime[0]),
                        description = description,
                        categoryId = category.id,
                        photoUri = pendingExpensePhotoUri?.toString()
                    )
                    bindTransactionData()
                    dialog.dismiss()
                }
            }
        }
    }

    private fun updatePhotoPreview() {
        val uri = pendingExpensePhotoUri
        if (uri == null) {
            activePhotoPreview?.visibility = View.GONE
            activePhotoPreview?.setImageDrawable(null)
            activePhotoLabel?.text = getString(R.string.no_photo_selected)
            activeRemovePhotoButton?.visibility = View.GONE
            return
        }

        activePhotoPreview?.visibility = View.VISIBLE
        activePhotoPreview?.setImageURI(uri)
        activePhotoLabel?.text = getString(R.string.photo_selected_message)
        activeRemovePhotoButton?.visibility = View.VISIBLE
    }

    private fun clearActivePhotoViews() {
        pendingExpensePhotoUri = null
        activePhotoPreview = null
        activePhotoLabel = null
        activeRemovePhotoButton = null
    }

    private fun showDatePicker(initialDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).show()
    }

    private fun showTimePicker(initialTime: LocalTime, onTimeSelected: (LocalTime) -> Unit) {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                onTimeSelected(LocalTime.of(hourOfDay, minute))
            },
            initialTime.hour,
            initialTime.minute,
            true
        ).show()
    }

    private fun showPhotoPreview(photoUri: Uri) {
        val imageView = ImageView(this).apply {
            adjustViewBounds = true
            setPadding(24, 24, 24, 24)
            setImageURI(photoUri)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.photo_preview_title)
            .setView(imageView)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun CharSequence?.toCurrencyValue(): Double {
        return this?.toString()?.trim()?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }
}

