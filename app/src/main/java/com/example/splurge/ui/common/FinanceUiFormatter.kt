package com.example.splurge.ui.common

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shared formatting helpers so every screen shows dates, times, and money consistently.
 */
object FinanceUiFormatter {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))
    private val displayDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val displayTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** Formats a numeric amount using South African currency conventions. */
    fun formatCurrency(amount: Double): String {
        return currencyFormatter.format(amount)
    }

    /** Formats a month for display in headings such as "April 2026". */
    fun formatMonthLabel(month: YearMonth): String {
        return month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    /** Formats a date for storage and Room queries using ISO order. */
    fun formatDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /** Parses an ISO date string back into a LocalDate. */
    fun parseDate(rawDate: String): LocalDate {
        return LocalDate.parse(rawDate, DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /** Parses an ISO date string safely and returns null when the value is invalid. */
    fun parseDateOrNull(rawDate: String): LocalDate? {
        return runCatching { parseDate(rawDate) }.getOrNull()
    }

    /** Converts a stored ISO date string into a friendlier on-screen label. */
    fun formatDisplayDate(rawDate: String): String {
        return parseDate(rawDate).format(displayDateFormatter)
    }

    /** Formats a LocalTime for display and storage. */
    fun formatTime(time: LocalTime): String {
        return time.format(displayTimeFormatter)
    }

    /** Parses a time string created by [formatTime]. */
    fun parseTime(rawTime: String): LocalTime {
        return LocalTime.parse(rawTime, displayTimeFormatter)
    }

    /** Produces the month key used when saving budget goals in the database. */
    fun monthKey(month: YearMonth): String {
        return month.toString()
    }
}
