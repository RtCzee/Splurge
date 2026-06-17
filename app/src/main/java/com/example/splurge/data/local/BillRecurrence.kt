package com.example.splurge.data.local

/**
 * Controls how a bill should roll forward after it is paid.
 */
enum class BillRecurrence {
    ONE_TIME,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY
}
