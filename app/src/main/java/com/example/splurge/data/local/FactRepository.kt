package com.example.splurge.data.local

import android.content.Context
import com.example.splurge.R

object FactRepository {

    fun getRandomFact(context: Context): String {

        val facts = context.resources
            .openRawResource(R.raw.facts)
            .bufferedReader()
            .readLines()

        return facts.random()
    }
}