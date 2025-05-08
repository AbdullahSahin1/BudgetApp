package com.example.budgetapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetapp.databinding.ItemTransactionBinding
import com.example.budgetapp.model.Transaction
import com.example.budgetapp.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        fun bind(transaction: Transaction) {
            binding.apply {
                amountText.text = String.format("%.2f ₺", transaction.amount)
                descriptionText.text = transaction.description
                categoryText.text = transaction.category
                dateText.text = dateFormat.format(transaction.date)
                typeText.text = if (transaction.type == TransactionType.INCOME) "Gelir" else "Gider"
                amountText.setTextColor(
                    if (transaction.type == TransactionType.INCOME)
                        android.graphics.Color.GREEN
                    else
                        android.graphics.Color.RED
                )

                editButton.setOnClickListener {
                    onEditClick(transaction)
                }

                deleteButton.setOnClickListener {
                    onDeleteClick(transaction)
                }
            }
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
} 