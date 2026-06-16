package com.example.splurge

import android.content.Context

object FactRepository {

    fun getRandomFact(context: Context): String {

        val facts = context.resources
            .openRawResource(R.raw.facts)
            .bufferedReader()
            .readLines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return facts.random()
    }
}