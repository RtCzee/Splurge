package com.example.splurge.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.splurge.R
import com.example.splurge.data.local.BillEntity
import java.text.NumberFormat
import java.util.Locale

/**
 * Builds the notification payload shown when a bill reminder fires.
 */
object BillReminderNotificationHelper {

    const val CHANNEL_ID = "bill_reminders_channel"
    private const val CHANNEL_NAME = "Bill reminders"
    private const val CHANNEL_DESCRIPTION = "Notifications for upcoming and snoozed bills"
    private const val REMINDER_NOTIFICATION_ID_OFFSET = 10_000

    /** Ensures the reminder channel exists before any notification is posted. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }

        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
        )
    }

    /** Posts an actionable bill reminder notification. */
    fun showReminder(
        context: Context,
        bill: BillEntity,
        daysBefore: Int,
        isSnooze: Boolean
    ) {
        if (!NotificationPermissionChecker.hasPostNotificationsPermission(context)) {
            return
        }

        ensureChannel(context)

        val contentTitle = context.getString(R.string.bill_reminder_notification_title)
        val contentText = buildReminderMessage(context, bill, daysBefore, isSnooze)
        val notificationId = REMINDER_NOTIFICATION_ID_OFFSET + bill.id.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(BillReminderScheduler.buildOpenBillsIntent(context, bill.id))
            .addAction(
                android.R.drawable.ic_menu_agenda,
                context.getString(R.string.bill_notification_open_action),
                BillReminderScheduler.buildOpenBillsIntent(context, bill.id)
            )
            .addAction(
                android.R.drawable.ic_menu_save,
                context.getString(R.string.bill_notification_paid_action),
                BillReminderScheduler.buildMarkPaidIntent(context, bill.id)
            )
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                context.getString(R.string.bill_notification_snooze_action),
                BillReminderScheduler.buildSnoozeActionIntent(context, bill.id)
            )

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    private fun buildReminderMessage(
        context: Context,
        bill: BillEntity,
        daysBefore: Int,
        isSnooze: Boolean
    ): String {
        val amountSuffix = bill.amount?.let {
            context.getString(R.string.bill_reminder_amount_suffix, formatCurrency(it))
        }.orEmpty()

        return when {
            isSnooze -> context.getString(
                R.string.bill_reminder_snoozed_message,
                bill.name,
                amountSuffix
            ).trim()
            daysBefore <= 0 -> context.getString(
                R.string.bill_reminder_due_today_message,
                bill.name,
                amountSuffix
            ).trim()
            else -> context.getString(
                R.string.bill_reminder_days_message,
                bill.name,
                daysBefore,
                amountSuffix
            ).trim()
        }
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA")).format(amount)
    }
}
