package com.example.budgetapp

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetapp.adapter.TransactionAdapter
import com.example.budgetapp.data.AppDatabase
import com.example.budgetapp.databinding.ActivityMainBinding
import com.example.budgetapp.databinding.DialogAddTransactionBinding
import com.example.budgetapp.model.ExpenseCategory
import com.example.budgetapp.model.IncomeCategory
import com.example.budgetapp.model.Transaction
import com.example.budgetapp.model.TransactionType
import com.example.budgetapp.fragment.ChartFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import android.app.DatePickerDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.text.Editable

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TransactionAdapter
    private lateinit var database: AppDatabase
    private lateinit var totalBalanceText: TextView
    private lateinit var monthYearText: TextView
    private var selectedDate = Calendar.getInstance()
    private lateinit var bottomNavigation: BottomNavigationView
    private var allTransactions: List<Transaction> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        setupRecyclerView()
        setupClickListeners()
        observeTransactions()
        observeBalance()

        totalBalanceText = binding.totalBalanceText
        monthYearText = binding.monthYearText
        val themeToggleButton = binding.themeToggleButton
        val calendarButton = binding.calendarButton

        themeToggleButton.setOnClickListener {
            toggleTheme()
        }

        calendarButton.setOnClickListener {
            showMonthYearPicker()
        }

        updateMonthYearText()
        updateTotalBalance()

        bottomNavigation = binding.bottomNavigation
        setupNavigation()
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            onEditClick = { transaction ->
                showEditTransactionDialog(transaction)
            },
            onDeleteClick = { transaction ->
                showDeleteConfirmationDialog(transaction)
            }
        )
        binding.transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupClickListeners() {
        binding.fab.setOnClickListener {
            showAddTransactionDialog()
        }

        binding.calendarButton.setOnClickListener {
            showMonthYearPicker()
        }

        binding.themeToggleButton.setOnClickListener {
            toggleTheme()
        }
    }

    private fun showAddTransactionDialog() {
        showTransactionDialog(null)
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        showTransactionDialog(transaction)
    }

    private fun showTransactionDialog(transaction: Transaction? = null) {
        val dialogBinding = DialogAddTransactionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (transaction == null) R.string.new_transaction else R.string.edit_transaction)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .create()

        // Tarih seçici ayarları
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr"))
        
        // Tarih seçici butonuna tıklama
        dialogBinding.calendarButton.setOnClickListener {
            showDatePicker(calendar) { selectedDate ->
                dialogBinding.dateEditText.setText(dateFormat.format(selectedDate.time))
            }
        }

        // Tarih alanına tıklama
        dialogBinding.dateEditText.setOnClickListener {
            showDatePicker(calendar) { selectedDate ->
                dialogBinding.dateEditText.setText(dateFormat.format(selectedDate.time))
            }
        }

        // Kategori spinner'ını ayarla
        val categories = if (dialogBinding.incomeRadio.isChecked) {
            IncomeCategory.values().map { getString(it.stringResId) }
        } else {
            ExpenseCategory.values().map { getString(it.stringResId) }
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.categorySpinner.adapter = adapter

        // Kategori seçimi değiştiğinde diğer açıklama alanını göster/gizle
        dialogBinding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = parent?.getItemAtPosition(position).toString()
                dialogBinding.otherCategoryLayout.visibility = 
                    if (selectedCategory == getString(R.string.category_other) || 
                        selectedCategory == getString(R.string.category_subscription)) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                dialogBinding.otherCategoryLayout.visibility = View.GONE
            }
        }

        // İşlem tipi değiştiğinde kategorileri güncelle
        dialogBinding.transactionTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            val categories = if (checkedId == R.id.incomeRadio) {
                IncomeCategory.values().map { getString(it.stringResId) }
            } else {
                ExpenseCategory.values().map { getString(it.stringResId) }
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dialogBinding.categorySpinner.adapter = adapter
        }

        // Düzenleme modunda mevcut değerleri doldur
        transaction?.let {
            dialogBinding.amountEditText.setText(it.amount.toString())
            dialogBinding.descriptionEditText.setText(it.description)
            dialogBinding.dateEditText.setText(dateFormat.format(it.date))
            
            // İşlem tipini ayarla
            if (it.type == TransactionType.INCOME) {
                dialogBinding.incomeRadio.isChecked = true
            } else {
                dialogBinding.expenseRadio.isChecked = true
            }

            // Kategori seçimini ayarla
            val categoryName = it.category
            if (categoryName.startsWith(getString(R.string.category_other))) {
                // Diğer kategorisi ise
                val parts = categoryName.split(": ", limit = 2)
                if (parts.size == 2) {
                    dialogBinding.otherCategoryEditText.setText(parts[1])
                }
                dialogBinding.categorySpinner.setSelection(
                    if (it.type == TransactionType.INCOME) {
                        IncomeCategory.values().indexOf(IncomeCategory.OTHER)
                    } else {
                        ExpenseCategory.values().indexOf(ExpenseCategory.OTHER)
                    }
                )
            } else {
                // Normal kategori ise
                dialogBinding.categorySpinner.setSelection(
                    if (it.type == TransactionType.INCOME) {
                        IncomeCategory.values().indexOfFirst { category -> 
                            getString(category.stringResId) == categoryName 
                        }.coerceAtLeast(0)
                    } else {
                        ExpenseCategory.values().indexOfFirst { category -> 
                            getString(category.stringResId) == categoryName 
                        }.coerceAtLeast(0)
                    }
                )
            }
        }

        dialogBinding.saveButton.setOnClickListener {
            val amount = dialogBinding.amountEditText.text.toString().toDoubleOrNull()
            val description = dialogBinding.descriptionEditText.text.toString()
            val categoryName = dialogBinding.categorySpinner.selectedItem.toString()
            val isIncome = dialogBinding.incomeRadio.isChecked
            val otherCategoryDescription = dialogBinding.otherCategoryEditText.text.toString()
            val dateText = dialogBinding.dateEditText.text.toString()

            if (amount == null || description.isEmpty() || dateText.isEmpty() || 
                (categoryName == getString(R.string.category_other) && otherCategoryDescription.isEmpty())) {
                Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = if (isIncome) {
                IncomeCategory.fromStringResId(
                    when (categoryName) {
                        getString(R.string.category_salary) -> R.string.category_salary
                        getString(R.string.category_rent) -> R.string.category_rent
                        getString(R.string.category_side_job) -> R.string.category_side_job
                        else -> R.string.category_other
                    }
                ).getDisplayName(this)
            } else {
                ExpenseCategory.fromStringResId(
                    when (categoryName) {
                        getString(R.string.category_rent) -> R.string.category_rent
                        getString(R.string.category_bills) -> R.string.category_bills
                        getString(R.string.category_entertainment) -> R.string.category_entertainment
                        getString(R.string.category_shopping) -> R.string.category_shopping
                        getString(R.string.category_subscription) -> R.string.category_subscription
                        else -> R.string.category_other
                    }
                ).getDisplayName(this)
            }

            // Eğer diğer kategorisi seçildiyse, açıklamayı kategori ismine ekle
            val finalCategory = if (categoryName == getString(R.string.category_other)) {
                "$category: $otherCategoryDescription"
            } else if (categoryName == getString(R.string.category_subscription)) {
                "Abonelik Gideri: $otherCategoryDescription"
            } else {
                category
            }

            // Tarihi parse et
            val date = try {
                dateFormat.parse(dateText) ?: Date()
            } catch (e: Exception) {
                Date()
            }

            if (transaction == null) {
                // Yeni işlem ekle
                val newTransaction = Transaction(
                    amount = amount,
                    description = description,
                    type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                    category = finalCategory,
                    date = date
                )
                lifecycleScope.launch {
                    database.transactionDao().insertTransaction(newTransaction)
                    showHomeFragment()
                }
            } else {
                // Mevcut işlemi güncelle
                val updatedTransaction = transaction.copy(
                    amount = amount,
                    description = description,
                    type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                    category = finalCategory,
                    date = date
                )
                lifecycleScope.launch {
                    database.transactionDao().insertTransaction(updatedTransaction)
                    showHomeFragment()
                }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDatePicker(calendar: Calendar, onDateSelected: (Calendar) -> Unit) {
        val currentDate = Calendar.getInstance()
        val currentYear = currentDate.get(Calendar.YEAR)
        val currentMonth = currentDate.get(Calendar.MONTH)
        val currentDay = currentDate.get(Calendar.DAY_OF_MONTH)

        // Ayın başlangıç tarihini ayarla
        val minDate = Calendar.getInstance().apply {
            set(currentYear, currentMonth, 1)
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, day)
                }

                // Gelecek tarih kontrolü
                if (selectedDate.after(currentDate)) {
                    Toast.makeText(this, R.string.future_date_message, Toast.LENGTH_SHORT).show()
                    return@DatePickerDialog
                }

                calendar.set(year, month, day)
                onDateSelected(calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Maksimum tarihi bugün olarak ayarla
        datePickerDialog.datePicker.maxDate = currentDate.timeInMillis
        // Minimum tarihi ayın başlangıcı olarak ayarla
        datePickerDialog.datePicker.minDate = minDate.timeInMillis
        datePickerDialog.show()
    }

    private fun showDeleteConfirmationDialog(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_transaction)
            .setMessage(R.string.delete_confirmation)
            .setPositiveButton(R.string.yes) { _, _ ->
                lifecycleScope.launch {
                    database.transactionDao().deleteTransaction(transaction)
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun observeTransactions() {
        lifecycleScope.launch {
            database.transactionDao().getAllTransactions().collectLatest { transactions ->
                allTransactions = transactions
                adapter.submitList(transactions.filter { isTransactionInSelectedMonth(it.date) })
            }
        }
    }

    private fun observeBalance() {
        lifecycleScope.launch {
            database.transactionDao().getTotalIncome().collectLatest { totalIncome ->
                database.transactionDao().getTotalExpense().collectLatest { totalExpense ->
                    val income = totalIncome ?: 0.0
                    val expense = totalExpense ?: 0.0
                    val balance = income - expense
                    totalBalanceText.text = String.format("%.2f ₺", balance)
                    totalBalanceText.setTextColor(
                        if (balance >= 0)
                            android.graphics.Color.GREEN
                        else
                            android.graphics.Color.RED
                    )
                }
            }
        }
    }

    private fun toggleTheme() {
        when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            android.content.res.Configuration.UI_MODE_NIGHT_NO -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    private fun showMonthYearPicker() {
        val months = resources.getStringArray(R.array.months)
        val currentMonth = selectedDate.get(Calendar.MONTH)
        val currentYear = selectedDate.get(Calendar.YEAR)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_month)
            .setSingleChoiceItems(months, currentMonth) { dialog, which ->
                selectedDate.set(Calendar.MONTH, which)
                dialog.dismiss()
                showYearPicker()
            }
            .show()
    }

    private fun showYearPicker() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val selectedYear = selectedDate.get(Calendar.YEAR)
        val years = arrayOf(currentYear.toString())
        val selectedYearIndex = 0

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_year)
            .setSingleChoiceItems(years, selectedYearIndex) { dialog, which ->
                selectedDate.set(Calendar.YEAR, years[which].toInt())
                dialog.dismiss()
                updateMonthYearText()
                updateTotalBalance()
            }
            .show()
    }

    private fun updateMonthYearText() {
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("tr"))
        monthYearText.text = monthFormat.format(selectedDate.time)
    }

    private fun updateTotalBalance() {
        lifecycleScope.launch {
            database.transactionDao().getAllTransactions().collectLatest { transactions ->
                val currentDate = Calendar.getInstance()
                val selectedMonth = selectedDate.get(Calendar.MONTH)
                val selectedYear = selectedDate.get(Calendar.YEAR)
                val currentMonth = currentDate.get(Calendar.MONTH)
                val currentYear = currentDate.get(Calendar.YEAR)

                when {
                    // Gelecek ay/yıl kontrolü
                    selectedYear > currentYear || (selectedYear == currentYear && selectedMonth > currentMonth) -> {
                        totalBalanceText.text = getString(R.string.future_month_message)
                        totalBalanceText.setTextColor(android.graphics.Color.GRAY)
                    }
                    // Geçmiş ay/yıl kontrolü
                    selectedYear < currentYear || (selectedYear == currentYear && selectedMonth < currentMonth) -> {
                        val filteredTransactions = transactions.filter { isTransactionInSelectedMonth(it.date) }
                        if (filteredTransactions.isEmpty()) {
                            totalBalanceText.text = getString(R.string.no_transactions_message)
                            totalBalanceText.setTextColor(android.graphics.Color.GRAY)
                        } else {
                            val total = filteredTransactions.sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }
                            totalBalanceText.text = String.format("%.2f ₺", total)
                            totalBalanceText.setTextColor(
                                if (total >= 0)
                                    android.graphics.Color.GREEN
                                else
                                    android.graphics.Color.RED
                            )
                        }
                    }
                    // Mevcut ay/yıl
                    else -> {
                        val total = transactions
                            .filter { isTransactionInSelectedMonth(it.date) }
                            .sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }
                        totalBalanceText.text = String.format("%.2f ₺", total)
                        totalBalanceText.setTextColor(
                            if (total >= 0)
                                android.graphics.Color.GREEN
                            else
                                android.graphics.Color.RED
                        )
                    }
                }
            }
        }
    }

    private fun isTransactionInSelectedMonth(date: Date): Boolean {
        val transactionCalendar = Calendar.getInstance().apply { time = date }
        return transactionCalendar.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                transactionCalendar.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR)
    }

    private fun setupNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showHomeFragment()
                    true
                }
                R.id.nav_chart -> {
                    showChartFragment()
                    true
                }
                else -> false
            }
        }
        showHomeFragment()
    }

    private fun showHomeFragment() {
        binding.fragmentContainer.visibility = View.GONE
        binding.transactionsRecyclerView.visibility = View.VISIBLE
        binding.totalBalanceText.visibility = View.VISIBLE
        binding.monthYearText.visibility = View.VISIBLE
        binding.fab.visibility = View.VISIBLE
    }

    private fun showChartFragment() {
        binding.fragmentContainer.visibility = View.VISIBLE
        binding.transactionsRecyclerView.visibility = View.GONE
        binding.totalBalanceText.visibility = View.GONE
        binding.monthYearText.visibility = View.GONE
        binding.fab.visibility = View.GONE

        val chartFragment = ChartFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, chartFragment)
            .commit()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            showHomeFragment()
        } else {
            super.onBackPressed()
        }
    }
}