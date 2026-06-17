package com.example.splurge.ui.budgets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.splurge.R
import com.example.splurge.data.local.ProgressVisualizationItem
import com.example.splurge.data.local.SpendingStatus
import com.example.splurge.ui.common.FinanceUiFormatter

/**
 * Adapter that displays progress visualization for category spending with status indicators.
 * Shows progress bars, spending amounts, and visual indicators for budget status.
 */
class ProgressVisualizationAdapter :
    RecyclerView.Adapter<ProgressVisualizationAdapter.ProgressViewHolder>() {

    private var items: List<ProgressVisualizationItem> = emptyList()
    private var expandedPosition: Int = -1

    /**
     * Submits a new list of progress items and refreshes the display.
     */
    fun submitList(newItems: List<ProgressVisualizationItem>) {
        items = newItems
        expandedPosition = -1
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_progress_visualization, parent, false)
        return ProgressViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        holder.bind(items[position], position == expandedPosition) { pos ->
            expandedPosition = if (expandedPosition == pos) -1 else pos
            notifyItemChanged(pos)
        }
    }

    override fun getItemCount(): Int = items.size

    /**
     * ViewHolder for displaying a single category's progress visualization.
     */
    class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.progress_category_name)
        private val statusIcon: ImageView = itemView.findViewById(R.id.progress_status_icon)
        private val statusLabel: TextView = itemView.findViewById(R.id.progress_status_label)
        private val currentAmount: TextView = itemView.findViewById(R.id.progress_current_amount)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
        private val expandButton: ImageView = itemView.findViewById(R.id.progress_expand_button)
        
        // Detailed views (shown when expanded)
        private val detailsContainer: View = itemView.findViewById(R.id.progress_details_container)
        private val minGoalText: TextView = itemView.findViewById(R.id.progress_min_goal_text)
        private val maxGoalText: TextView = itemView.findViewById(R.id.progress_max_goal_text)
        private val remainingText: TextView = itemView.findViewById(R.id.progress_remaining_text)
        private val progressPercentText: TextView = itemView.findViewById(R.id.progress_percent_text)

        fun bind(
            item: ProgressVisualizationItem,
            isExpanded: Boolean,
            onExpandClick: (Int) -> Unit
        ) {
            // Set category name
            categoryName.text = item.categoryName

            // Set current amount
            currentAmount.text = FinanceUiFormatter.formatCurrency(item.currentSpending)

            // Set status and styling
            val (statusIconRes, statusColor) = getStatusIndicators(item.status, itemView.context)
            statusIcon.setImageResource(statusIconRes)
            statusIcon.setColorFilter(statusColor)
            statusLabel.text = item.getStatusLabel()
            statusLabel.setTextColor(statusColor)

            // Set progress bar
            progressBar.progress = item.getProgressPercent().toInt()
            val progressColor = when (item.status) {
                SpendingStatus.OVER_MAX -> ContextCompat.getColor(itemView.context, R.color.status_over_max)
                SpendingStatus.UNDER_MIN -> ContextCompat.getColor(itemView.context, R.color.status_under_min)
                SpendingStatus.WITHIN_GOALS -> ContextCompat.getColor(itemView.context, R.color.status_on_track)
            }
            progressBar.progressDrawable.setColorFilter(progressColor, android.graphics.PorterDuff.Mode.SRC_IN)

            // Handle expansion
            detailsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            expandButton.rotation = if (isExpanded) 180f else 0f
            expandButton.setOnClickListener {
                onExpandClick(adapterPosition)
            }

            // Populate detailed information when expanded
            if (isExpanded) {
                minGoalText.text = itemView.context.getString(
                    R.string.progress_min_goal_label,
                    FinanceUiFormatter.formatCurrency(item.minimumGoal)
                )
                maxGoalText.text = itemView.context.getString(
                    R.string.progress_max_goal_label,
                    FinanceUiFormatter.formatCurrency(item.maximumGoal)
                )
                remainingText.text = itemView.context.getString(
                    R.string.progress_remaining_label,
                    FinanceUiFormatter.formatCurrency(item.getRemainingAmount())
                )
                progressPercentText.text = itemView.context.getString(
                    R.string.progress_percent_label,
                    item.getProgressPercent().toInt()
                )
            }
        }

        /**
         * Returns the appropriate status icon resource and color based on spending status.
         */
        private fun getStatusIndicators(
            status: SpendingStatus,
            context: android.content.Context
        ): Pair<Int, Int> {
            return when (status) {
                SpendingStatus.OVER_MAX -> Pair(
                    R.drawable.ic_status_over,
                    ContextCompat.getColor(context, R.color.status_over_max)
                )
                SpendingStatus.UNDER_MIN -> Pair(
                    R.drawable.ic_status_under,
                    ContextCompat.getColor(context, R.color.status_under_min)
                )
                SpendingStatus.WITHIN_GOALS -> Pair(
                    R.drawable.ic_status_check,
                    ContextCompat.getColor(context, R.color.status_on_track)
                )
            }
        }
    }
}
