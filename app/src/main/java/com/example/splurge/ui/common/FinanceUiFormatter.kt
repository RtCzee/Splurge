package com.example.splurge.ui.common

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object FinanceUiFormatter {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))
    private val displayDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val displayTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun formatCurrency(amount: Double): String {
        return currencyFormatter.format(amount)
    }

    fun formatMonthLabel(month: YearMonth): String {
        return month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    fun formatDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    fun parseDate(rawDate: String): LocalDate {
        return LocalDate.parse(rawDate, DateTimeFormatter.ISO_LOCAL_DATE)
    }

    fun formatDisplayDate(rawDate: String): String {
        return parseDate(rawDate).format(displayDateFormatter)
    }

    fun formatTime(time: LocalTime): String {
        return time.format(displayTimeFormatter)
    }

    fun parseTime(rawTime: String): LocalTime {
        return LocalTime.parse(rawTime, displayTimeFormatter)
    }

    fun monthKey(month: YearMonth): String {
        return month.toString()
    }
}

