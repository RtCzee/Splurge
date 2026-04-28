package com.example.splurge.ui.budgets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.splurge.R
import com.example.splurge.data.local.CategorySpendTotal
import com.example.splurge.ui.common.FinanceUiFormatter

/**
 * Simple adapter that renders each category spending total on the budgets screen.
 */
class BudgetCategoryAdapter : RecyclerView.Adapter<BudgetCategoryAdapter.BudgetCategoryViewHolder>() {

    private var items: List<CategorySpendTotal> = emptyList()

    /** Replaces the displayed category totals with the latest query results. */
    fun submitList(newItems: List<CategorySpendTotal>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetCategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget_category, parent, false)
        return BudgetCategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: BudgetCategoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    /** Holds the views used to display one category summary row. */
    class BudgetCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.category_name)
        private val categoryAmount: TextView = itemView.findViewById(R.id.category_amount)
        private val categoryEntries: TextView = itemView.findViewById(R.id.category_entries)

        /** Binds one category total into the item view. */
        fun bind(item: CategorySpendTotal) {
            categoryName.text = item.categoryName
            categoryAmount.text = FinanceUiFormatter.formatCurrency(item.totalAmount)
            categoryEntries.text = itemView.context.resources.getQuantityString(
                R.plurals.category_entry_count,
                item.entryCount,
                item.entryCount
            )
        }
    }
}
