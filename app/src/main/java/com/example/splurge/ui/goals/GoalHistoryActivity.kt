package com.example.splurge.ui.goals

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.splurge.R
import com.example.splurge.data.AuthSessionManager
import com.example.splurge.data.GoalRepository
import com.example.splurge.data.PrivacyPreferences
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class GoalHistoryActivity : AppCompatActivity() {

    private lateinit var repository: GoalRepository
    private lateinit var authSessionManager: AuthSessionManager
    private lateinit var rvHistory: RecyclerView
    private lateinit var tvMonthlySummary: TextView
    private lateinit var tvSuccessRate: TextView
    private lateinit var tvBestCategory: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: Button

    private val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    private var currentUserId: Long = -1L
    private lateinit var privacyPreferences: PrivacyPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_history)

        repository = GoalRepository(this)
        authSessionManager = AuthSessionManager(this)
        privacyPreferences = PrivacyPreferences(this)

        currentUserId = authSessionManager.getSignedInUserId() ?: -1L

        if (currentUserId == -1L) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        loadHistory()
        setupClickListeners()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Goal History"
    }

    private fun initViews() {
        rvHistory = findViewById(R.id.rvHistory)
        tvMonthlySummary = findViewById(R.id.tvMonthlySummary)
        tvSuccessRate = findViewById(R.id.tvSuccessRate)
        tvBestCategory = findViewById(R.id.tvBestCategory)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)

        rvHistory.layoutManager = LinearLayoutManager(this)
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val review = repository.getMonthlyReview(currentUserId)

            if (privacyPreferences.isHideBalancesEnabled()) {
                tvMonthlySummary.text = getString(R.string.privacy_hidden_message)
                tvSuccessRate.text = getString(R.string.privacy_hidden_value)
                tvBestCategory.text = getString(R.string.privacy_hidden_value)
            } else {
                tvMonthlySummary.text = review.message
                tvSuccessRate.text = String.format(Locale.US, "Success Rate: %.0f%%", review.successRate)
                tvBestCategory.text = review.bestCategory?.let { "Best Category: $it" } ?: "No completed goals yet"
            }
            progressBar.max = 100
            progressBar.progress = if (privacyPreferences.isHideBalancesEnabled()) 0 else review.successRate.toInt()
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
