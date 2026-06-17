package com.example.splurge.data

import android.content.Context
import com.example.splurge.R
import java.text.NumberFormat
import java.util.Locale

/**
 * Stores privacy-related preferences that should work for both guests and signed-in users.
 */
class PrivacyPreferences(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val appContext = context.applicationContext
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))

    fun isHideBalancesEnabled(): Boolean {
        return preferences.getBoolean(KEY_HIDE_BALANCES, false)
    }

    fun setHideBalancesEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_HIDE_BALANCES, enabled).apply()
    }

    fun formatCurrency(amount: Double): String {
        return if (isHideBalancesEnabled()) {
            appContext.getString(R.string.privacy_hidden_value)
        } else {
            currencyFormatter.format(amount)
        }
    }

    fun formatCurrency(amount: Double?): String {
        return amount?.let { formatCurrency(it) } ?: appContext.getString(R.string.bill_amount_not_set)
    }

    companion object {
        private const val PREFS_NAME = "splurge_privacy_settings"
        private const val KEY_HIDE_BALANCES = "hide_balances"
    }
}
