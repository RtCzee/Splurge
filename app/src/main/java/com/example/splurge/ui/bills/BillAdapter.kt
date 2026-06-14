package com.example.splurge.ui.bills

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.splurge.R
import com.example.splurge.data.local.BillEntity
import com.google.android.material.button.MaterialButton

/**
 * Adapter used by both the active list and the bill history list.
 */
class BillAdapter(
    private val mode: BillListMode,
    private val reminderDays: () -> List<Int>,
    private val listener: BillActionListener
) : RecyclerView.Adapter<BillAdapter.BillViewHolder>() {

    private val items = mutableListOf<BillEntity>()

    fun submitList(newItems: List<BillEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bill, parent, false)
        return BillViewHolder(view)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class BillViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val billName = itemView.findViewById<TextView>(R.id.bill_name)
        private val billStatusChip = itemView.findViewById<TextView>(R.id.bill_status_chip)
        private val billDueValue = itemView.findViewById<TextView>(R.id.bill_due_value)
        private val billAmountValue = itemView.findViewById<TextView>(R.id.bill_amount_value)
        private val billRecurrenceValue = itemView.findViewById<TextView>(R.id.bill_recurrence_value)
        private val billNotesValue = itemView.findViewById<TextView>(R.id.bill_notes_value)
        private val actionRow = itemView.findViewById<View>(R.id.bill_action_row)
        private val paidButton = itemView.findViewById<MaterialButton>(R.id.mark_paid_button)
        private val snoozeButton = itemView.findViewById<MaterialButton>(R.id.snooze_button)
        private val cancelButton = itemView.findViewById<MaterialButton>(R.id.cancel_button)
        private val deleteButton = itemView.findViewById<MaterialButton>(R.id.delete_button)

        fun bind(bill: BillEntity) {
            val context = itemView.context
            val intervals = reminderDays.invoke()

            billName.text = bill.name
            billStatusChip.text = BillUiFormatter.buildStatusChip(context, bill)
            billDueValue.text = BillUiFormatter.buildDueSummary(context, bill, intervals)
            billAmountValue.text = BillUiFormatter.formatAmount(context, bill.amount)
            billRecurrenceValue.text = context.getString(
                R.string.bill_recurrence_value,
                BillUiFormatter.formatRecurrence(context, bill.recurrence)
            )

            val notes = bill.notes?.trim().orEmpty()
            if (notes.isBlank()) {
                billNotesValue.visibility = View.GONE
            } else {
                billNotesValue.visibility = View.VISIBLE
                billNotesValue.text = notes
            }

            if (mode == BillListMode.ACTIVE) {
                actionRow.visibility = View.VISIBLE
                paidButton.visibility = View.VISIBLE
                snoozeButton.visibility = View.VISIBLE
                cancelButton.visibility = View.VISIBLE
                deleteButton.visibility = View.GONE

                paidButton.setOnClickListener { listener.onMarkPaid(bill) }
                snoozeButton.setOnClickListener { listener.onSnooze(bill) }
                cancelButton.setOnClickListener { listener.onCancel(bill) }
            } else {
                actionRow.visibility = View.VISIBLE
                paidButton.visibility = View.GONE
                snoozeButton.visibility = View.GONE
                cancelButton.visibility = View.GONE
                deleteButton.visibility = View.VISIBLE
                deleteButton.setOnClickListener { listener.onDelete(bill) }
            }
        }
    }
}

/**
 * Switches the bill row between the active-management and history layouts.
 */
enum class BillListMode {
    ACTIVE,
    HISTORY
}

/**
 * Callbacks fired when the user taps a bill action button.
 */
interface BillActionListener {
    fun onMarkPaid(bill: BillEntity)
    fun onSnooze(bill: BillEntity)
    fun onCancel(bill: BillEntity)
    fun onDelete(bill: BillEntity)
}
