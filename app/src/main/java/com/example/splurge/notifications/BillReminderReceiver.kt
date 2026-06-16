package com.example.splurge.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.splurge.data.FinanceRepository
import com.example.splurge.data.local.BillEntity
import com.example.splurge.data.local.BillStatus
import com.example.splurge.ui.common.FinanceUiFormatter
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Handles reminder alarms and notification action buttons.
 */
class BillReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val billId = intent.getLongExtra(EXTRA_BILL_ID, -1L)
        if (billId <= 0L) {
            return
        }

        val repository = FinanceRepository.getInstance(context)
        when (intent.action) {
            ACTION_MARK_PAID -> {
                repository.markBillPaid(billId)
            }
            ACTION_SNOOZE -> {
                repository.snoozeBill(billId, 1)
            }
            ACTION_TRIGGER_REMINDER -> {
                handleReminderTrigger(context, repository, billId, intent)
            }
        }
    }

    private fun handleReminderTrigger(
        context: Context,
        repository: FinanceRepository,
        billId: Long,
        intent: Intent
    ) {
        val bill = repository.getBillById(billId) ?: return
        if (bill.status != BillStatus.ACTIVE) {
            return
        }

        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)
        val snoozedUntil = bill.snoozedUntilMillis
        val now = System.currentTimeMillis()
        if (!isSnooze && snoozedUntil != null && snoozedUntil > now) {
            return
        }

        if (isSnooze) {
            repository.updateBill(bill.clearSnooze())
        }

        val dueDate = FinanceUiFormatter.parseDateOrNull(bill.dueDate) ?: return
        val daysBefore = if (isSnooze) {
            ChronoUnit.DAYS.between(LocalDate.now(), dueDate).toInt().coerceAtLeast(0)
        } else {
            intent.getIntExtra(EXTRA_REMINDER_DAYS, 0)
        }
        BillReminderNotificationHelper.showReminder(context, bill, daysBefore, false)
    }

    private fun BillEntity.clearSnooze(): BillEntity {
        return copy(snoozedUntilMillis = null)
    }

    companion object {
        private const val ACTION_TRIGGER_REMINDER = "com.example.splurge.action.TRIGGER_BILL_REMINDER"
        private const val ACTION_MARK_PAID = "com.example.splurge.action.MARK_BILL_PAID"
        private const val ACTION_SNOOZE = "com.example.splurge.action.SNOOZE_BILL_REMINDER"
        private const val EXTRA_BILL_ID = "extra_bill_id"
        private const val EXTRA_REMINDER_DAYS = "extra_reminder_days"
        private const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    }
}
