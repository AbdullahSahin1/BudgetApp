package com.example.budgetapp

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetapp.databinding.FragmentMonthlySummaryBinding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.Calendar

class MonthlySummaryFragment : Fragment() {
    private var _binding: FragmentMonthlySummaryBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TransactionViewModel
    private lateinit var incomeAdapter: CategorySummaryAdapter
    private lateinit var expenseAdapter: CategorySummaryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMonthlySummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]

        setupRecyclerViews()
        setupCharts()
        observeTransactions()
    }

    private fun setupRecyclerViews() {
        incomeAdapter = CategorySummaryAdapter()
        expenseAdapter = CategorySummaryAdapter()

        binding.incomeRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = incomeAdapter
        }

        binding.expenseRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = expenseAdapter
        }
    }

    private fun setupCharts() {
        setupPieChart(binding.incomePieChart)
        setupPieChart(binding.expensePieChart)
    }

    private fun setupPieChart(chart: PieChart) {
        chart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            legend.isEnabled = true
            legend.textColor = getTextColor()
            setEntryLabelColor(getTextColor())
            setEntryLabelTextSize(12f)
        }
    }

    private fun getTextColor(): Int {
        return if (isDarkMode()) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }

    private fun isDarkMode(): Boolean {
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
    }

    private fun observeTransactions() {
        viewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            updateCharts(transactions)
            updateRecyclerViews(transactions)
        }
    }

    private fun updateCharts(transactions: List<Transaction>) {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        val monthlyTransactions = transactions.filter {
            val cal = Calendar.getInstance()
            cal.time = it.date
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }

        val incomeEntries = mutableListOf<PieEntry>()
        val expenseEntries = mutableListOf<PieEntry>()

        monthlyTransactions.forEach { transaction ->
            when (transaction.type) {
                TransactionType.INCOME -> {
                    val existingEntry = incomeEntries.find { it.label == transaction.category }
                    if (existingEntry != null) {
                        existingEntry.y += transaction.amount.toFloat()
                    } else {
                        incomeEntries.add(PieEntry(transaction.amount.toFloat(), transaction.category))
                    }
                }
                TransactionType.EXPENSE -> {
                    val existingEntry = expenseEntries.find { it.label == transaction.category }
                    if (existingEntry != null) {
                        existingEntry.y += transaction.amount.toFloat()
                    } else {
                        expenseEntries.add(PieEntry(transaction.amount.toFloat(), transaction.category))
                    }
                }
            }
        }

        updatePieChart(binding.incomePieChart, incomeEntries, "Gelirler")
        updatePieChart(binding.expensePieChart, expenseEntries, "Giderler")
    }

    private fun updatePieChart(chart: PieChart, entries: List<PieEntry>, title: String) {
        if (entries.isEmpty()) {
            chart.clear()
            return
        }

        val dataSet = PieDataSet(entries, title)
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = getTextColor()
        dataSet.valueTextSize = 12f

        val data = PieData(dataSet)
        chart.data = data
        chart.invalidate()
    }

    private fun updateRecyclerViews(transactions: List<Transaction>) {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        val monthlyTransactions = transactions.filter {
            val cal = Calendar.getInstance()
            cal.time = it.date
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }

        val incomeSummaries = monthlyTransactions
            .filter { it.type == TransactionType.INCOME }
            .groupBy { it.category }
            .map { (category, transactions) ->
                CategorySummary(
                    category,
                    transactions.sumOf { it.amount },
                    transactions.size
                )
            }

        val expenseSummaries = monthlyTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .map { (category, transactions) ->
                CategorySummary(
                    category,
                    transactions.sumOf { it.amount },
                    transactions.size
                )
            }

        incomeAdapter.submitList(incomeSummaries)
        expenseAdapter.submitList(expenseSummaries)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 