package com.example.splurge.ui.goals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.splurge.R
import com.example.splurge.data.local.SavingsGoalEntity
import com.example.splurge.ui.common.FinanceUiFormatter

/**
 * Adapter that renders each saved savings goal and its progress.
 */
class SavingsGoalsAdapter : RecyclerView.Adapter<SavingsGoalsAdapter.SavingsGoalViewHolder>() {

    private var items: List<SavingsGoalEntity> = emptyList()

    /** Replaces the list contents with the latest goals from the repository. */
    fun submitList(newItems: List<SavingsGoalEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavingsGoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_savings_goal, parent, false)
        return SavingsGoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavingsGoalViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    /** Caches the views used by one row in the savings goal list. */
    class SavingsGoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.goal_title)
        private val targetDateView: TextView = itemView.findViewById(R.id.goal_target_date)
        private val amountView: TextView = itemView.findViewById(R.id.goal_amounts)
        private val progressView: ProgressBar = itemView.findViewById(R.id.goal_progress)
        private val percentView: TextView = itemView.findViewById(R.id.goal_percent)

        /** Fills the row with one goal's values and completion percentage. */
        fun bind(item: SavingsGoalEntity) {
            val progress = if (item.targetAmount > 0.0) {
                ((item.currentAmount / item.targetAmount) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }

            titleView.text = item.title
            targetDateView.text = itemView.context.getString(
                R.string.goal_target_date_value,
                FinanceUiFormatter.formatDisplayDate(item.targetDate)
            )
            amountView.text = itemView.context.getString(
                R.string.goal_amounts_value,
                FinanceUiFormatter.formatCurrency(item.currentAmount),
                FinanceUiFormatter.formatCurrency(item.targetAmount)
            )
            progressView.max = 100
            progressView.progress = progress
            percentView.text = itemView.context.getString(R.string.goal_progress_percent, progress)
        }
    }
}

