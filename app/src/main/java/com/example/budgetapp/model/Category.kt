package com.example.budgetapp.model

import android.content.Context
import com.example.budgetapp.R

enum class ExpenseCategory(val stringResId: Int, val iconResId: Int) {
    RENT(R.string.category_rent, android.R.drawable.ic_menu_compass),
    BILLS(R.string.category_bills, android.R.drawable.ic_menu_edit),
    ENTERTAINMENT(R.string.category_entertainment, android.R.drawable.ic_menu_share),
    SHOPPING(R.string.category_shopping, android.R.drawable.ic_menu_gallery),
    SUBSCRIPTION(R.string.category_subscription, android.R.drawable.ic_menu_recent_history),
    OTHER(R.string.category_other, android.R.drawable.ic_menu_help);

    fun getDisplayName(context: Context): String {
        return context.getString(stringResId)
    }

    companion object {
        fun fromDisplayName(context: Context, displayName: String): ExpenseCategory {
            return values().find { it.getDisplayName(context) == displayName } ?: OTHER
        }

        fun fromStringResId(resId: Int): ExpenseCategory {
            return values().find { it.stringResId == resId } ?: OTHER
        }
    }
}

enum class IncomeCategory(val stringResId: Int, val iconResId: Int) {
    SALARY(R.string.category_salary, android.R.drawable.ic_menu_send),
    RENT(R.string.category_rent, android.R.drawable.ic_menu_compass),
    SIDE_JOB(R.string.category_side_job, android.R.drawable.ic_menu_myplaces),
    OTHER(R.string.category_other, android.R.drawable.ic_menu_help);

    fun getDisplayName(context: Context): String {
        return context.getString(stringResId)
    }

    companion object {
        fun fromDisplayName(context: Context, displayName: String): IncomeCategory {
            return values().find { it.getDisplayName(context) == displayName } ?: OTHER
        }

        fun fromStringResId(resId: Int): IncomeCategory {
            return values().find { it.stringResId == resId } ?: OTHER
        }
    }
} 