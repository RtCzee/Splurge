package com.example.splurge.ui.bills

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.splurge.R
import com.example.splurge.data.FinanceRepository
import com.example.splurge.data.local.BillEntity
import com.example.splurge.data.local.BillRecurrence
import com.example.splurge.notifications.BillReminderPreferences
import com.example.splurge.notifications.BillReminderScheduler
import com.example.splurge.notifications.NotificationPermissionChecker
import com.example.splurge.ui.base.BaseActivity
import com.example.splurge.ui.common.FinanceUiFormatter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Screen for adding bills, reviewing reminders, and managing bill history.
 */
class Bills : BaseActivity(), BillActionListener {
    private lateinit var repository: FinanceRepository
    private lateinit var reminderPreferences: BillReminderPreferences
    private lateinit var activeBillsAdapter: BillAdapter
    private lateinit var billHistoryAdapter: BillAdapter
    private var showingHistoryTab = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                repository.rescheduleAllBillReminders()
                refreshBills()
            } else {
                Toast.makeText(this, R.string.bill_notifications_disabled_message, Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bills)
        repository = FinanceRepository.getInstance(this)
        reminderPreferences = BillReminderPreferences(this)
        activeBillsAdapter = BillAdapter(BillListMode.ACTIVE, ::getReminderDays, this)
        billHistoryAdapter = BillAdapter(BillListMode.HISTORY, ::getReminderDays, this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupLists()
        setupTabs()
        setupAddButton()
        refreshBills()
        setupBottomNavigation(R.id.navigation_bills)
        ensureNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        refreshBills()
        ensureNotificationPermission()
    }

    override fun onMarkPaid(bill: BillEntity) {
        repository.markBillPaid(bill.id)
        refreshBills()
    }

    override fun onSnooze(bill: BillEntity) {
        showSnoozeDialog(bill)
    }

    override fun onCancel(bill: BillEntity) {
        repository.archiveBill(bill.id)
        refreshBills()
    }

    override fun onDelete(bill: BillEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.bill_delete_confirm_title)
            .setMessage(R.string.bill_delete_confirm_message)
            .setPositiveButton(R.string.bill_delete_confirm_action) { _, _ ->
                repository.deleteBill(bill.id)
                refreshBills()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** Binds both recyclers once so the data refresh only replaces the rows. */
    private fun setupLists() {
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.active_bills_recycler).apply {
            layoutManager = LinearLayoutManager(this@Bills)
            adapter = activeBillsAdapter
        }
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.bill_history_recycler).apply {
            layoutManager = LinearLayoutManager(this@Bills)
            adapter = billHistoryAdapter
        }
    }

    /** Keeps the tab state aligned with whichever list is visible. */
    private fun setupTabs() {
        val activeRecycler = findViewById<View>(R.id.active_bills_recycler)
        val historyRecycler = findViewById<View>(R.id.bill_history_recycler)
        val emptyText = findViewById<TextView>(R.id.empty_bills_text)

        findViewById<TabLayout>(R.id.tab_layout).addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    showingHistoryTab = tab.position == 1
                    activeRecycler.visibility = if (showingHistoryTab) View.GONE else View.VISIBLE
                    historyRecycler.visibility = if (showingHistoryTab) View.VISIBLE else View.GONE
                    emptyText.text = getString(
                        if (showingHistoryTab) R.string.no_bill_history_message else R.string.no_bills_message
                    )
                    refreshBills()
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit
                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            }
        )
    }

    /** Wires the floating action button that opens the bill creation dialog. */
    private fun setupAddButton() {
        findViewById<FloatingActionButton>(R.id.add_bill_fab).setOnClickListener {
            if (!NotificationPermissionChecker.hasPostNotificationsPermission(this)) {
                showNotificationRequiredDialog()
                return@setOnClickListener
            }

            showAddBillDialog()
        }
    }

    /** Pulls the latest bill data and pushes it into the visible widgets. */
    private fun refreshBills() {
        val activeBills = repository.getActiveBills()
        val historicBills = repository.getHistoricBills()
        val reminderDays = getReminderDays()

        activeBillsAdapter.submitList(activeBills)
        billHistoryAdapter.submitList(historicBills)

        findViewById<TextView>(R.id.bills_summary_title).text = resources.getQuantityString(
            R.plurals.active_bill_count,
            activeBills.size,
            activeBills.size
        )
        findViewById<TextView>(R.id.bills_next_due_value).text = buildNextReminderSummary(activeBills, reminderDays)
        findViewById<TextView>(R.id.bills_notifications_value).text = buildNotificationStatusSummary()

        renderReminderCenter(activeBills, reminderDays)

        val emptyText = findViewById<TextView>(R.id.empty_bills_text)
        val isEmpty = if (showingHistoryTab) historicBills.isEmpty() else activeBills.isEmpty()
        emptyText.text = getString(
            if (showingHistoryTab) R.string.no_bill_history_message else R.string.no_bills_message
        )
        emptyText.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    /** Shows a dialog for creating a new bill with its due date and recurrence. */
    private fun showAddBillDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bill_entry, null, false)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.bill_name_input)
        val dueDateInput = dialogView.findViewById<TextInputEditText>(R.id.bill_due_date_input)
        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.bill_amount_input)
        val recurrenceInput = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.bill_recurrence_input)
        val notesInput = dialogView.findViewById<TextInputEditText>(R.id.bill_notes_input)

        val selectedDueDate = arrayOf(LocalDate.now().plusDays(1))
        dueDateInput.setText(FinanceUiFormatter.formatDisplayDate(FinanceUiFormatter.formatDate(selectedDueDate[0])))
        recurrenceInput.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                listOf(
                    getString(R.string.bill_recurrence_one_time),
                    getString(R.string.bill_recurrence_weekly),
                    getString(R.string.bill_recurrence_monthly),
                    getString(R.string.bill_recurrence_quarterly),
                    getString(R.string.bill_recurrence_yearly)
                )
            )
        )
        recurrenceInput.setText(getString(R.string.bill_recurrence_monthly), false)

        dueDateInput.setOnClickListener {
            showDatePicker(selectedDueDate[0]) { date ->
                selectedDueDate[0] = date
                dueDateInput.setText(FinanceUiFormatter.formatDisplayDate(FinanceUiFormatter.formatDate(date)))
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_bill_dialog_title)
            .setView(dialogView)
            .setPositiveButton(R.string.save_bill_button, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val billName = nameInput.text?.toString()?.trim().orEmpty()
            val amountText = amountInput.text?.toString()?.trim().orEmpty()
            val recurrenceLabel = recurrenceInput.text?.toString()?.trim().orEmpty()
            val notes = notesInput.text?.toString()?.trim().orEmpty()
            val amount = amountText.toOptionalAmount()
            val recurrence = mapRecurrenceLabel(recurrenceLabel)

            when {
                billName.isBlank() -> showValidationMessage(R.string.invalid_bill_name)
                amountText.isNotBlank() && amount == null -> showValidationMessage(R.string.invalid_bill_amount)
                recurrence == null -> showValidationMessage(R.string.invalid_bill_recurrence)
                else -> {
                    repository.addBill(
                        name = billName,
                        dueDate = FinanceUiFormatter.formatDate(selectedDueDate[0]),
                        amount = amount,
                        recurrence = recurrence,
                        notes = notes.takeIf { it.isNotBlank() }
                    )
                    refreshBills()
                    dialog.dismiss()
                }
            }
        }
    }

    /** Renders the reminder center as a small stack of actionable bill summaries. */
    private fun renderReminderCenter(activeBills: List<BillEntity>, reminderDays: List<Int>) {
        val container = findViewById<LinearLayout>(R.id.bill_reminder_center_container)
        val emptyText = findViewById<TextView>(R.id.bill_reminder_center_empty)
        container.removeAllViews()

        val reminders = activeBills.mapNotNull { bill ->
            BillUiFormatter.buildReminderPreview(this, bill, reminderDays)
        }.take(4)

        emptyText.visibility = if (reminders.isEmpty()) View.VISIBLE else View.GONE
        reminders.forEach { reminder ->
            val reminderView = TextView(this).apply {
                setTextColor(getColor(R.color.dashboard_primary_text))
                textSize = 13f
                text = reminder
                setPadding(0, 0, 0, 12)
            }
            container.addView(reminderView)
        }
    }

    /** Returns the currently enabled reminder intervals from settings. */
    private fun getReminderDays(): List<Int> {
        return reminderPreferences.getReminderIntervals()
    }

    /** Explains whether notifications are ready for bill reminders. */
    private fun buildNotificationStatusSummary(): String {
        val remindersEnabled = reminderPreferences.isPushNotificationsEnabled() && reminderPreferences.areBillRemindersEnabled()
        return if (remindersEnabled && NotificationPermissionChecker.hasPostNotificationsPermission(this)) {
            getString(R.string.bill_notifications_enabled_message)
        } else {
            getString(R.string.bill_notifications_disabled_message)
        }
    }

    /** Describes the next reminder in plain language for the summary card. */
    private fun buildNextReminderSummary(activeBills: List<BillEntity>, reminderDays: List<Int>): String {
        val nextBill = activeBills.mapNotNull { bill ->
            FinanceUiFormatter.parseDateOrNull(bill.dueDate)?.let { dueDate ->
                bill to dueDate
            }
        }.minByOrNull { (_, dueDate) ->
            ChronoUnit.DAYS.between(LocalDate.now(), dueDate)
        }?.first
        return nextBill?.let {
            BillUiFormatter.buildReminderPreview(this, it, reminderDays)
        } ?: getString(R.string.bill_next_due_empty_message)
    }

    /** Opens a date picker and returns the selected date through a callback. */
    private fun showDatePicker(initialDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
        android.app.DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).show()
    }

    /** Displays a persistent reminder request when notifications are unavailable. */
    private fun ensureNotificationPermission() {
        val remindersEnabled = reminderPreferences.isPushNotificationsEnabled() && reminderPreferences.areBillRemindersEnabled()
        if (!remindersEnabled) {
            return
        }

        if (NotificationPermissionChecker.hasPostNotificationsPermission(this)) {
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        showNotificationRequiredDialog()
    }

    private fun showNotificationRequiredDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.bill_notifications_required_title)
            .setMessage(R.string.bill_notifications_required_message)
            .setCancelable(false)
            .setPositiveButton(R.string.bill_notifications_enable_action) { _, _ ->
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                finish()
            }
            .show()
    }

    /** Lets the user pause one reminder without marking the bill as paid. */
    private fun showSnoozeDialog(bill: BillEntity) {
        val snoozeOptions = arrayOf(
            getString(R.string.bill_snooze_one_day),
            getString(R.string.bill_snooze_three_days),
            getString(R.string.bill_snooze_one_week)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.bill_snooze_title)
            .setItems(snoozeOptions) { _, which ->
                val snoozeDays = when (which) {
                    0 -> 1
                    1 -> 3
                    else -> 7
                }
                repository.snoozeBill(bill.id, snoozeDays)
                refreshBills()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** Shows a concise validation message if the form input is not usable yet. */
    private fun showValidationMessage(messageResId: Int) {
        MaterialAlertDialogBuilder(this)
            .setMessage(messageResId)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /** Maps the selected dropdown label back to the stored recurrence enum. */
    private fun mapRecurrenceLabel(label: String): BillRecurrence? {
        return when (label) {
            getString(R.string.bill_recurrence_one_time) -> BillRecurrence.ONE_TIME
            getString(R.string.bill_recurrence_weekly) -> BillRecurrence.WEEKLY
            getString(R.string.bill_recurrence_monthly) -> BillRecurrence.MONTHLY
            getString(R.string.bill_recurrence_quarterly) -> BillRecurrence.QUARTERLY
            getString(R.string.bill_recurrence_yearly) -> BillRecurrence.YEARLY
            else -> null
        }
    }

    /** Parses the optional amount field into a nullable number. */
    private fun String.toOptionalAmount(): Double? {
        return if (isBlank()) {
            null
        } else {
            toDoubleOrNull()?.takeIf { it > 0.0 }
        }
    }
}
