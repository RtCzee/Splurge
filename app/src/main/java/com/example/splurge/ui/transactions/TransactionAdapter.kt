package com.example.splurge.ui.transactions

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.splurge.R
import com.example.splurge.data.PrivacyPreferences
import com.example.splurge.data.local.TransactionListItem
import com.example.splurge.data.local.TransactionType
import com.example.splurge.ui.common.FinanceUiFormatter
import com.google.android.material.button.MaterialButton

/**
 * Adapter that renders detailed transaction entries (Income and Expenses).
 */
class TransactionAdapter(
    private val onPhotoClick: (Uri) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private var items: List<TransactionListItem> = emptyList()

    fun submitList(newItems: List<TransactionListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction_entry, parent, false)
        return TransactionViewHolder(view, onPhotoClick)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class TransactionViewHolder(
        itemView: View,
        private val onPhotoClick: (Uri) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val descriptionValue: TextView = itemView.findViewById(R.id.transaction_description_value)
        private val amountValue: TextView = itemView.findViewById(R.id.transaction_amount_value)
        private val categoryValue: TextView = itemView.findViewById(R.id.transaction_category_value)
        private val typeLabel: TextView = itemView.findViewById(R.id.transaction_type_label)
        private val dateValue: TextView = itemView.findViewById(R.id.transaction_date_value)
        private val photoPreview: ImageView = itemView.findViewById(R.id.transaction_photo_preview)
        private val photoButton: MaterialButton = itemView.findViewById(R.id.view_photo_button)

        fun bind(item: TransactionListItem) {
            descriptionValue.text = item.description
            amountValue.text = PrivacyPreferences(itemView.context).formatCurrency(item.amount)
            categoryValue.text = item.categoryName
            dateValue.text = FinanceUiFormatter.formatDisplayDate(item.date)
            
            typeLabel.text = item.type.name
            val typeColor = if (item.type == TransactionType.INCOME) {
                // Green for income
                android.graphics.Color.parseColor("#4CAF50")
            } else {
                // Red/Primary for expense
                android.graphics.Color.parseColor("#F44336")
            }
            typeLabel.setTextColor(typeColor)

            val photoUri = item.photoUri?.let(Uri::parse)
            if (photoUri != null) {
                photoPreview.visibility = View.VISIBLE
                photoButton.visibility = View.VISIBLE
                photoPreview.setImageURI(photoUri)
                photoPreview.setOnClickListener { onPhotoClick(photoUri) }
                photoButton.setOnClickListener { onPhotoClick(photoUri) }
            } else {
                photoPreview.visibility = View.GONE
                photoButton.visibility = View.GONE
            }
        }
    }
}
