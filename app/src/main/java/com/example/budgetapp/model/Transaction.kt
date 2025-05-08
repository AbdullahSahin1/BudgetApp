package com.example.budgetapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.budgetapp.util.Converters
import java.util.Date

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val type: TransactionType,
    val date: Date,
    val category: String
)

enum class TransactionType {
    INCOME,
    EXPENSE
} 