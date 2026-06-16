package com.example.splurge.ui.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.splurge.R
import com.example.splurge.data.PrivacyPreferences
import com.example.splurge.data.local.CategorySpendTotal
import com.example.splurge.ui.common.FinanceUiFormatter

/**
 * Adapter used by the transactions totals tab to show grouped category summaries.
 */
class CategoryTotalsAdapter : RecyclerView.Adapter<CategoryTotalsAdapter.CategoryTotalViewHolder>() {

    private var items: List<CategorySpendTotal> = emptyList()

    /** Updates the totals list with the most recent query results. */
    fun submitList(newItems: List<CategorySpendTotal>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryTotalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_total, parent, false)
        return CategoryTotalViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryTotalViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    /** Holds the views for one category total row. */
    class CategoryTotalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.total_category_name)
        private val entryCount: TextView = itemView.findViewById(R.id.total_category_entries)
        private val totalAmount: TextView = itemView.findViewById(R.id.total_category_amount)

        /** Writes one category summary into the row. */
        fun bind(item: CategorySpendTotal) {
            categoryName.text = item.categoryName
            entryCount.text = itemView.context.resources.getQuantityString(
                R.plurals.category_entry_count,
                item.entryCount,
                item.entryCount
            )
            totalAmount.text = PrivacyPreferences(itemView.context).formatCurrency(item.totalAmount)
        }
    }
}
