package com.example.splurge.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.splurge.data.FinanceRepository
import com.example.splurge.data.local.BillEntity
import com.example.splurge.data.local.BillStatus
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.absoluteValue

/**
 * Schedules bill reminder alarms with the system AlarmManager.
 */
object BillReminderScheduler {

    private const val ACTION_TRIGGER_REMINDER = "com.example.splurge.action.TRIGGER_BILL_REMINDER"
    private const val ACTION_MARK_PAID = "com.example.splurge.action.MARK_BILL_PAID"
    private const val ACTION_SNOOZE = "com.example.splurge.action.SNOOZE_BILL_REMINDER"
    private const val EXTRA_BILL_ID = "extra_bill_id"
    private const val EXTRA_REMINDER_DAYS = "extra_reminder_days"
    private const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    private const val REQUEST_CODE_MULTIPLIER = 1000
    private const val REQUEST_CODE_SNOOZE_OFFSET = 900
    private const val REMINDER_HOUR = 9
    private const val REMINDER_MINUTE = 0
    private const val MILLISECONDS_PER_MINUTE = 60_000L

    /** Schedules every reminder window for a single bill. */
    fun scheduleBill(context: Context, bill: BillEntity, reminderDays: List<Int>) {
        if (bill.status != BillStatus.ACTIVE) {
            return
        }

        if (!shouldSchedule(context)) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        reminderDays.forEach { daysBefore ->
            val triggerAtMillis = calculateTriggerMillis(bill.dueDate, daysBefore)
            if (triggerAtMillis == null) {
                return@forEach
            }
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                buildReminderPendingIntent(context, bill.id, daysBefore)
            )
        }

        bill.snoozedUntilMillis?.takeIf { it > System.currentTimeMillis() }?.let { snoozedUntil ->
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                snoozedUntil,
                buildSnoozePendingIntent(context, bill.id)
            )
        }
    }

    /** Cancels every scheduled reminder for one bill. */
    fun cancelBill(context: Context, billId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(7, 3, 1).forEach { daysBefore ->
            alarmManager.cancel(buildReminderPendingIntent(context, billId, daysBefore))
        }
        alarmManager.cancel(buildSnoozePendingIntent(context, billId))
    }

    /** Removes every known bill alarm, usually after reminders are disabled. */
    fun cancelAll(context: Context) {
        FinanceRepository.getInstance(context).getBills().forEach { bill ->
            cancelBill(context, bill.id)
        }
    }

    /** Rebuilds the alarm set from the current database rows. */
    fun rescheduleAll(context: Context, bills: List<BillEntity>, reminderDays: List<Int>) {
        bills.forEach { bill ->
            cancelBill(context, bill.id)
            scheduleBill(context, bill, reminderDays)
        }
    }

    /** Creates the notification action for marking a bill as paid. */
    fun buildMarkPaidIntent(context: Context, billId: Long): PendingIntent {
        val intent = Intent(context, BillReminderReceiver::class.java).apply {
            action = ACTION_MARK_PAID
            putExtra(EXTRA_BILL_ID, billId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeForAction(billId, ACTION_MARK_PAID),
            intent,
            pendingIntentFlags()
        )
    }

    /** Creates the notification action for snoozing the current reminder by one day. */
    fun buildSnoozeActionIntent(context: Context, billId: Long): PendingIntent {
        val intent = Intent(context, BillReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_BILL_ID, billId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeForAction(billId, ACTION_SNOOZE),
            intent,
            pendingIntentFlags()
        )
    }

    /** Opens the bills screen directly from the notification. */
    fun buildOpenBillsIntent(context: Context, billId: Long): PendingIntent {
        val intent = Intent(context, com.example.splurge.ui.bills.Bills::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_BILL_ID, billId)
        }
        return PendingIntent.getActivity(
            context,
            requestCodeForAction(billId, "OPEN_BILLS"),
            intent,
            pendingIntentFlags()
        )
    }

    /** Returns the action id used when the alarm fires for a reminder window. */
    fun buildReminderTriggerIntent(context: Context, billId: Long, daysBefore: Int, isSnooze: Boolean = false): PendingIntent {
        val intent = Intent(context, BillReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_BILL_ID, billId)
            putExtra(EXTRA_REMINDER_DAYS, daysBefore)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeForReminder(billId, daysBefore, isSnooze),
            intent,
            pendingIntentFlags()
        )
    }

    /** Convenience for scheduling a one-day snooze reminder. */
    fun scheduleSnooze(context: Context, billId: Long, snoozedUntilMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            snoozedUntilMillis,
            buildSnoozePendingIntent(context, billId)
        )
    }

    internal fun calculateTriggerMillis(dueDate: String, daysBefore: Int): Long? {
        val reminderDate = LocalDate.parse(dueDate).minusDays(daysBefore.toLong())
        if (reminderDate.isBefore(LocalDate.now())) {
            return null
        }

        val triggerDateTime = reminderDate.atTime(REMINDER_HOUR, REMINDER_MINUTE)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        return max(triggerDateTime, System.currentTimeMillis() + MILLISECONDS_PER_MINUTE)
    }

    private fun shouldSchedule(context: Context): Boolean {
        val preferences = BillReminderPreferences(context)
        return preferences.areBillRemindersEnabled() && preferences.isPushNotificationsEnabled() && NotificationPermissionChecker.hasPostNotificationsPermission(context)
    }

    private fun buildReminderPendingIntent(context: Context, billId: Long, daysBefore: Int): PendingIntent {
        return buildReminderTriggerIntent(context, billId, daysBefore)
    }

    private fun buildSnoozePendingIntent(context: Context, billId: Long): PendingIntent {
        val intent = Intent(context, BillReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_BILL_ID, billId)
            putExtra(EXTRA_IS_SNOOZE, true)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeForAction(billId, ACTION_SNOOZE),
            intent,
            pendingIntentFlags()
        )
    }

    private fun requestCodeForReminder(billId: Long, daysBefore: Int, isSnooze: Boolean): Int {
        val base = billId * REQUEST_CODE_MULTIPLIER + daysBefore + if (isSnooze) REQUEST_CODE_SNOOZE_OFFSET else 0
        return base.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun requestCodeForAction(billId: Long, action: String): Int {
        val base = billId * REQUEST_CODE_MULTIPLIER + action.hashCode().absoluteValue.toLong()
        return base.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun pendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }
}
