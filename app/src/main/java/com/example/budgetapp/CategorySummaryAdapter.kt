package com.example.budgetapp

import android.content.res.Configuration
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetapp.databinding.ItemCategorySummaryBinding

class CategorySummaryAdapter : ListAdapter<CategorySummary, CategorySummaryAdapter.ViewHolder>(CategorySummaryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategorySummaryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemCategorySummaryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(summary: CategorySummary) {
            binding.apply {
                categoryName.text = summary.category
                totalAmount.text = String.format("₺%.2f", summary.total)
                transactionCount.text = "${summary.count} işlem"
                
                // Dark mode desteği
                val textColor = if (isDarkMode()) {
                    Color.WHITE
                } else {
                    Color.BLACK
                }
                
                categoryName.setTextColor(textColor)
                totalAmount.setTextColor(textColor)
                transactionCount.setTextColor(textColor)
            }
        }

        private fun isDarkMode(): Boolean {
            return when (itemView.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> true
                else -> false
            }
        }
    }
}

class CategorySummaryDiffCallback : DiffUtil.ItemCallback<CategorySummary>() {
    override fun areItemsTheSame(oldItem: CategorySummary, newItem: CategorySummary): Boolean {
        return oldItem.category == newItem.category
    }

    override fun areContentsTheSame(oldItem: CategorySummary, newItem: CategorySummary): Boolean {
        return oldItem == newItem
    }
} 