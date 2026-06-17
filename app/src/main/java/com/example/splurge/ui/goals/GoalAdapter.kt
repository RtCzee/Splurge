package com.example.splurge.ui.goals

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.splurge.R
import com.example.splurge.data.PrivacyPreferences
import com.example.splurge.data.local.GoalEntity
import java.text.SimpleDateFormat
import java.util.*

class GoalAdapter(
    private val onAddProgressClick: (GoalEntity) -> Unit
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    private var goals = listOf<GoalEntity>()
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    fun submitList(newGoals: List<GoalEntity>) {
        goals = newGoals
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monthly_goal, parent, false)  // This name is correct
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(goals[position])
    }

    override fun getItemCount(): Int = goals.size

    inner class GoalViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvGoalName: TextView = itemView.findViewById(R.id.tvGoalName)
        private val tvTargetAmount: TextView = itemView.findViewById(R.id.tvTargetAmount)
        private val tvCurrentAmount: TextView = itemView.findViewById(R.id.tvCurrentAmount)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvDeadline: TextView = itemView.findViewById(R.id.tvDeadline)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val tvProgressPercentage: TextView = itemView.findViewById(R.id.tvProgressPercentage)
        private val btnAddProgress: Button = itemView.findViewById(R.id.btnAddProgress)

        fun bind(goal: GoalEntity) {
            val privacyPreferences = PrivacyPreferences(itemView.context)
            val progress = if (goal.targetAmount > 0) {
                ((goal.currentAmount / goal.targetAmount) * 100).toFloat()
            } else 0f

            tvGoalName.text = goal.name
            tvTargetAmount.text = privacyPreferences.formatCurrency(goal.targetAmount)
            tvCurrentAmount.text = privacyPreferences.formatCurrency(goal.currentAmount)
            tvCategory.text = goal.category
            tvDeadline.text = dateFormat.format(goal.deadline)
            tvProgressPercentage.text = if (privacyPreferences.isHideBalancesEnabled()) {
                itemView.context.getString(R.string.privacy_hidden_value)
            } else {
                String.format(Locale.US, "%.0f%%", progress)
            }
            progressBar.max = 100
            progressBar.progress = if (privacyPreferences.isHideBalancesEnabled()) 0 else progress.toInt()

            val progressColor = when {
                progress >= 90f -> android.R.color.holo_green_dark
                progress >= 60f -> android.R.color.holo_blue_dark
                progress >= 30f -> android.R.color.holo_orange_dark
                else -> android.R.color.holo_red_dark
            }
            progressBar.progressTintList = ContextCompat.getColorStateList(itemView.context, progressColor)

            val categoryColor = when (goal.category) {
                "SAVING" -> android.R.color.holo_green_dark
                "SPENDING_LIMIT" -> android.R.color.holo_orange_dark
                "INVESTMENT" -> android.R.color.holo_blue_dark
                "DEBT_PAYMENT" -> android.R.color.holo_red_dark
                else -> android.R.color.holo_purple
            }
            tvCategory.setBackgroundColor(ContextCompat.getColor(itemView.context, categoryColor))

            btnAddProgress.setOnClickListener {
                onAddProgressClick(goal)
            }
        }
    }
}
