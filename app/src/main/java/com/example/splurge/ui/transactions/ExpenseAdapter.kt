package com.example.splurge.ui.transactions

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.splurge.R
import com.example.splurge.data.PrivacyPreferences
import com.example.splurge.data.local.ExpenseListItem
import com.example.splurge.ui.common.FinanceUiFormatter
import com.google.android.material.button.MaterialButton

/**
 * Adapter that renders detailed expense entries for the transactions screen.
 */
class ExpenseAdapter(
    // The screen decides how to preview photos, so the adapter exposes a callback.
    private val onPhotoClick: (Uri) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    private var items: List<ExpenseListItem> = emptyList()

    /** Swaps in the latest list of expenses for the selected period. */
    fun submitList(newItems: List<ExpenseListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense_entry, parent, false)
        return ExpenseViewHolder(view, onPhotoClick)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ExpenseViewHolder(
        itemView: View,
        private val onPhotoClick: (Uri) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val amountValue: TextView = itemView.findViewById(R.id.expense_amount_value)
        private val descriptionValue: TextView = itemView.findViewById(R.id.expense_description_value)
        private val categoryValue: TextView = itemView.findViewById(R.id.expense_category_value)
        private val dateValue: TextView = itemView.findViewById(R.id.expense_date_value)
        private val timeValue: TextView = itemView.findViewById(R.id.expense_time_value)
        private val photoPreview: ImageView = itemView.findViewById(R.id.expense_photo_preview)
        private val photoButton: MaterialButton = itemView.findViewById(R.id.view_photo_button)

        /** Populates one expense row and toggles photo controls when an image exists. */
        fun bind(item: ExpenseListItem) {
            amountValue.text = PrivacyPreferences(itemView.context).formatCurrency(item.amount)
            descriptionValue.text = item.description
            categoryValue.text = item.categoryName
            dateValue.text = FinanceUiFormatter.formatDisplayDate(item.date)
            timeValue.text = itemView.context.getString(
                R.string.expense_time_range,
                item.startTime,
                item.endTime
            )

            val photoUri = item.photoUri?.let(Uri::parse)
            if (photoUri != null) {
                // Both the thumbnail and button open the same full-screen preview callback.
                photoPreview.visibility = View.VISIBLE
                photoButton.visibility = View.VISIBLE
                photoPreview.setImageURI(photoUri)
                photoPreview.setOnClickListener { onPhotoClick(photoUri) }
                photoButton.setOnClickListener { onPhotoClick(photoUri) }
            } else {
                photoPreview.visibility = View.GONE
                photoButton.visibility = View.GONE
                photoPreview.setImageDrawable(null)
                photoPreview.setOnClickListener(null)
                photoButton.setOnClickListener(null)
            }
        }
    }
}
