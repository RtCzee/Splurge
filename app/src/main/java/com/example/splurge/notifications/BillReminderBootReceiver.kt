package com.example.splurge.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.splurge.data.FinanceRepository

/**
 * Rebuilds reminder alarms after the device restarts.
 */
class BillReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        FinanceRepository.getInstance(context).rescheduleAllBillReminders()
    }
}
