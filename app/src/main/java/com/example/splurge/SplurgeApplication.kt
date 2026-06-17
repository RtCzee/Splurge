package com.example.splurge

import android.app.Application
import com.example.splurge.data.FinanceRepository

/**
 * Application entry point that restores bill reminder alarms early.
 */
class SplurgeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FinanceRepository.getInstance(this).rescheduleAllBillReminders()
    }
}
