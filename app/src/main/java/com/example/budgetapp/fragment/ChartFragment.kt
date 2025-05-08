package com.example.budgetapp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.budgetapp.R
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.example.budgetapp.data.AppDatabase
import com.example.budgetapp.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class ChartFragment : Fragment() {
    private lateinit var pieChart: PieChart
    private lateinit var database: AppDatabase
    private lateinit var totalIncomeText: TextView
    private lateinit var totalExpenseText: TextView
    private lateinit var balanceText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        database = AppDatabase.getDatabase(requireContext())
        pieChart = view.findViewById(R.id.pieChart)
        totalIncomeText = view.findViewById(R.id.totalIncomeText)
        totalExpenseText = view.findViewById(R.id.totalExpenseText)
        balanceText = view.findViewById(R.id.balanceText)
        
        setupPieChart()
        loadChartData()
    }

    private fun setupPieChart() {
        pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(android.graphics.Color.WHITE)
            setTransparentCircleRadius(61f)
            setDrawCenterText(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            legend.isEnabled = true
            setEntryLabelColor(android.graphics.Color.BLACK)
            setEntryLabelTextSize(12f)
        }
    }

    private fun loadChartData() {
        CoroutineScope(Dispatchers.IO).launch {
            val transactions = database.transactionDao().getAllTransactions().first()
            val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
            val incomes = transactions.filter { it.type == TransactionType.INCOME }
            
            val totalIncome = incomes.sumOf { it.amount }
            val totalExpense = expenses.sumOf { it.amount }
            val balance = totalIncome - totalExpense
            
            val categoryMap = expenses.groupBy { it.category }
                .mapValues { it.value.sumOf { transaction -> transaction.amount } }
            
            val entries = categoryMap.map { PieEntry(it.value.toFloat(), it.key) }
            
            withContext(Dispatchers.Main) {
                // Grafik verilerini güncelle
                val dataSet = PieDataSet(entries, "Kategoriler")
                dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
                
                val data = PieData(dataSet)
                data.setValueTextSize(12f)
                data.setValueTextColor(android.graphics.Color.BLACK)
                
                pieChart.data = data
                pieChart.invalidate()

                // Özet bilgileri güncelle
                totalIncomeText.text = "Toplam Gelir: %.2f ₺".format(totalIncome)
                totalExpenseText.text = "Toplam Gider: %.2f ₺".format(totalExpense)
                balanceText.text = "Bakiye: %.2f ₺".format(balance)
                balanceText.setTextColor(
                    if (balance >= 0)
                        android.graphics.Color.GREEN
                    else
                        android.graphics.Color.RED
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::pieChart.isInitialized) {
            pieChart.clear()
            pieChart.invalidate()
        }
    }
} 