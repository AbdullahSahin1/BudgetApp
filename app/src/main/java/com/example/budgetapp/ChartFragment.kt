package com.example.budgetapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.example.budgetapp.databinding.FragmentChartBinding
import com.github.mikephil.charting.formatter.PercentFormatter

class ChartFragment : Fragment() {
    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPieChart()
        updateChartData()
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleRadius(61f)
            setDrawCenterText(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400, Easing.EaseInOutQuad)
            legend.isEnabled = true
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
            setUsePercentValues(true)
            setDrawEntryLabels(true)
            setEntryLabelTextSize(12f)
            setEntryLabelColor(Color.WHITE)
            legend.textSize = 12f
            legend.textColor = Color.BLACK
            setHoleRadius(58f)
            setTransparentCircleRadius(61f)
            setDrawCenterText(true)
            setCenterText("Gelir/Gider")
            setCenterTextSize(16f)
            setCenterTextColor(Color.BLACK)
        }
    }

    private fun updateChartData() {
        // Örnek veri - gerçek uygulamada veritabanından alınacak
        val entries = listOf(
            PieEntry(70f, getString(R.string.income)),
            PieEntry(30f, getString(R.string.expense))
        )

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.rgb(76, 175, 80), // Gelir için yeşil
                Color.rgb(244, 67, 54)  // Gider için kırmızı
            )
            valueTextSize = 14f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter(binding.pieChart)
        }

        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.invalidate()

        // Özet bilgileri güncelle
        binding.totalIncomeText.text = "${getString(R.string.total_income)}: ₺700"
        binding.totalExpenseText.text = "${getString(R.string.total_expense)}: ₺300"
        binding.balanceText.text = "${getString(R.string.balance)}: ₺400"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 