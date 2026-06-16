package com.example.splurge.ui.bills

import android.content.Context
import com.example.splurge.R
import com.example.splurge.data.PrivacyPreferences
import com.example.splurge.data.local.BillEntity
import com.example.splurge.data.local.BillRecurrence
import com.example.splurge.data.local.BillStatus
import com.example.splurge.ui.common.FinanceUiFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

/**
 * Shared bill formatting helpers so the list rows and summary cards stay consistent.
 */
object BillUiFormatter {

    fun formatAmount(context: Context, amount: Double?): String {
        return PrivacyPreferences(context).formatCurrency(amount)
    }

    fun formatRecurrence(context: Context, recurrence: BillRecurrence): String {
        return when (recurrence) {
            BillRecurrence.ONE_TIME -> context.getString(R.string.bill_recurrence_one_time)
            BillRecurrence.WEEKLY -> context.getString(R.string.bill_recurrence_weekly)
            BillRecurrence.MONTHLY -> context.getString(R.string.bill_recurrence_monthly)
            BillRecurrence.QUARTERLY -> context.getString(R.string.bill_recurrence_quarterly)
            BillRecurrence.YEARLY -> context.getString(R.string.bill_recurrence_yearly)
        }
    }

    fun buildDueSummary(
        context: Context,
        bill: BillEntity,
        reminderDays: List<Int>,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        bill.snoozedUntilMillis?.takeIf { it > nowMillis }?.let { snoozedUntil ->
            return context.getString(
                R.string.bill_snoozed_until_value,
                FinanceUiFormatter.formatDisplayDate(formatIsoDate(snoozedUntil))
            )
        }

        val today = LocalDate.now()
        val dueDate = FinanceUiFormatter.parseDateOrNull(bill.dueDate)
            ?: return context.getString(R.string.bill_status_active)
        val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate).toInt()

        return when {
            daysUntilDue < 0 -> context.getString(
                R.string.bill_overdue_value,
                bill.name,
                daysUntilDue.absoluteValue
            )
            daysUntilDue == 0 -> context.getString(R.string.bill_due_today_value, bill.name)
            else -> {
                val nextReminderWindow = reminderDays.filter { it <= daysUntilDue }.maxOrNull()
                if (nextReminderWindow == null) {
                    context.getString(
                        R.string.bill_due_in_days_value,
                        bill.name,
                        daysUntilDue
                    )
                } else {
                    val daysUntilReminder = daysUntilDue - nextReminderWindow
                    if (daysUntilReminder <= 0) {
                        context.getString(
                            R.string.bill_reminder_today_value,
                            bill.name,
                            daysUntilDue
                        )
                    } else {
                        context.getString(
                            R.string.bill_reminder_in_days_value,
                            bill.name,
                            daysUntilReminder,
                            daysUntilDue
                        )
                    }
                }
            }
        }
    }

    fun buildStatusChip(
        context: Context,
        bill: BillEntity,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        return when (bill.status) {
            BillStatus.ACTIVE -> {
                bill.snoozedUntilMillis?.takeIf { it > nowMillis }?.let {
                    return context.getString(
                        R.string.bill_status_snoozed,
                        FinanceUiFormatter.formatDisplayDate(formatIsoDate(it))
                    )
                }

                val dueDate = FinanceUiFormatter.parseDateOrNull(bill.dueDate)
                    ?: return context.getString(R.string.bill_status_active)
                val daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), dueDate).toInt()
                when {
                    daysUntilDue < 0 -> context.getString(R.string.bill_status_overdue)
                    daysUntilDue == 0 -> context.getString(R.string.bill_status_due_today)
                    else -> context.getString(R.string.bill_status_active)
                }
            }
            BillStatus.PAID -> context.getString(R.string.bill_status_paid)
            BillStatus.ARCHIVED -> context.getString(R.string.bill_status_archived)
        }
    }

    fun buildLifecycleLabel(context: Context, bill: BillEntity): String {
        return when (bill.status) {
            BillStatus.ACTIVE -> context.getString(
                R.string.bill_due_on_value,
                FinanceUiFormatter.parseDateOrNull(bill.dueDate)?.let {
                    FinanceUiFormatter.formatDisplayDate(it.toString())
                } ?: bill.dueDate
            )
            BillStatus.PAID -> context.getString(
                R.string.bill_paid_on_value,
                formatStoredTimestamp(bill.paidAtMillis)
            )
            BillStatus.ARCHIVED -> context.getString(
                R.string.bill_archived_value,
                FinanceUiFormatter.parseDateOrNull(bill.dueDate)?.let {
                    FinanceUiFormatter.formatDisplayDate(it.toString())
                } ?: bill.dueDate
            )
        }
    }

    fun buildReminderPreview(
        context: Context,
        bill: BillEntity,
        reminderDays: List<Int>,
        nowMillis: Long = System.currentTimeMillis()
    ): String? {
        if (bill.status != BillStatus.ACTIVE) {
            return null
        }

        bill.snoozedUntilMillis?.takeIf { it > nowMillis }?.let { snoozedUntil ->
            return context.getString(
                R.string.bill_preview_snoozed_value,
                bill.name,
                FinanceUiFormatter.formatDisplayDate(formatIsoDate(snoozedUntil))
            )
        }

        val today = LocalDate.now()
        val dueDate = FinanceUiFormatter.parseDateOrNull(bill.dueDate) ?: return null
        val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate).toInt()
        if (daysUntilDue < 0) {
            return context.getString(
                R.string.bill_preview_overdue_value,
                bill.name,
                daysUntilDue.absoluteValue
            )
        }

        if (daysUntilDue == 0) {
            return context.getString(R.string.bill_preview_due_today_value, bill.name)
        }

        val nextReminderWindow = reminderDays.filter { it <= daysUntilDue }.maxOrNull()
        return if (nextReminderWindow == null) {
            context.getString(R.string.bill_preview_due_value, bill.name, daysUntilDue)
        } else {
            val daysUntilReminder = daysUntilDue - nextReminderWindow
            if (daysUntilReminder <= 0) {
                context.getString(R.string.bill_preview_reminder_today_value, bill.name, daysUntilDue)
            } else {
                context.getString(
                    R.string.bill_preview_reminder_value,
                    bill.name,
                    daysUntilReminder,
                    daysUntilDue
                )
            }
        }
    }

    private fun formatIsoDate(millis: Long): String {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
    }

    private fun formatStoredTimestamp(millis: Long?): String {
        return millis?.let {
            FinanceUiFormatter.formatDisplayDate(formatIsoDate(it))
        } ?: "-"
    }
}
