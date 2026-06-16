package com.example.splurge

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.jvm.java

class FactLoadingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fact_loading)

        val factText = findViewById<TextView>(R.id.factText)

        factText.text = FactRepository.getRandomFact(this)

        lifecycleScope.launch {
            delay(2500)
            startActivity(Intent(this@FactLoadingActivity, WelcomeActivity::class.kotlin))
            finish()
        }
    }
}