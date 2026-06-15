package com.example.splurge.notifications

import android.content.Context

/**
 * Small SharedPreferences wrapper for bill reminder settings.
 */
class BillReminderPreferences(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun isPushNotificationsEnabled(): Boolean {
        return preferences.getBoolean(KEY_PUSH_NOTIFICATIONS_ENABLED, true)
    }

    fun setPushNotificationsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PUSH_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun areBillRemindersEnabled(): Boolean {
        return preferences.getBoolean(KEY_BILL_REMINDERS_ENABLED, true)
    }

    fun setBillRemindersEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_BILL_REMINDERS_ENABLED, enabled).apply()
    }

    /** Returns the active reminder windows in descending order. */
    fun getReminderIntervals(): List<Int> {
        val reminderDays = buildList {
            if (preferences.getBoolean(KEY_REMINDER_SEVEN_DAYS, true)) add(7)
            if (preferences.getBoolean(KEY_REMINDER_THREE_DAYS, true)) add(3)
            if (preferences.getBoolean(KEY_REMINDER_ONE_DAY, true)) add(1)
        }

        return if (reminderDays.isEmpty()) {
            listOf(1)
        } else {
            reminderDays
        }
    }

    /** Saves the three built-in reminder intervals and keeps at least one enabled. */
    fun setReminderIntervals(
        sevenDays: Boolean,
        threeDays: Boolean,
        oneDay: Boolean
    ): List<Int> {
        val normalizedOneDay = if (!sevenDays && !threeDays && !oneDay) true else oneDay
        preferences.edit()
            .putBoolean(KEY_REMINDER_SEVEN_DAYS, sevenDays)
            .putBoolean(KEY_REMINDER_THREE_DAYS, threeDays)
            .putBoolean(KEY_REMINDER_ONE_DAY, normalizedOneDay)
            .apply()
        return getReminderIntervals()
    }

    companion object {
        private const val PREFS_NAME = "bill_reminder_settings"
        private const val KEY_PUSH_NOTIFICATIONS_ENABLED = "push_notifications_enabled"
        private const val KEY_BILL_REMINDERS_ENABLED = "bill_reminders_enabled"
        private const val KEY_REMINDER_SEVEN_DAYS = "reminder_seven_days"
        private const val KEY_REMINDER_THREE_DAYS = "reminder_three_days"
        private const val KEY_REMINDER_ONE_DAY = "reminder_one_day"
    }
}
