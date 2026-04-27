package com.example.splurge.ui.budgets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.splurge.R
import com.example.splurge.data.local.CategorySpendTotal
import com.example.splurge.ui.common.FinanceUiFormatter

class BudgetCategoryAdapter : RecyclerView.Adapter<BudgetCategoryAdapter.BudgetCategoryViewHolder>() {

    private var items: List<CategorySpendTotal> = emptyList()

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

    class BudgetCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.category_name)
        private val categoryAmount: TextView = itemView.findViewById(R.id.category_amount)
        private val categoryEntries: TextView = itemView.findViewById(R.id.category_entries)

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

