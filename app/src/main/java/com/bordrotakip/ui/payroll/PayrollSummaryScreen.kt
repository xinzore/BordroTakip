package com.bordrotakip.ui.payroll

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bordrotakip.data.repository.PayrollRecordRepository
import com.bordrotakip.util.formatLocalizedCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PayrollSummaryUiState(
    val isLoading: Boolean = true,
    val recordCount: Int = 0,
    val totalNetSalary: Double = 0.0,
    val totalGrossEarnings: Double = 0.0,
    val totalDeductions: Double = 0.0,
    val totalTax: Double = 0.0,
    val totalNonTaxDeductions: Double = 0.0,
    val socialSecurityTotal: Double = 0.0,
    val unemploymentTotal: Double = 0.0,
    val incomeTaxTotal: Double = 0.0,
    val stampTaxTotal: Double = 0.0,
    val besTotal: Double = 0.0,
    val otherDeductionsTotal: Double = 0.0
)

@HiltViewModel
class PayrollSummaryViewModel @Inject constructor(
    payrollRecordRepository: PayrollRecordRepository
) : ViewModel() {
    val uiState: StateFlow<PayrollSummaryUiState> = payrollRecordRepository.getAllRecords()
        .map { records ->
            val totalNet = records.sumOf { it.netSalary }
            val totalGross = records.sumOf { it.totalEarnings }
            val socialSecurity = records.sumOf { it.sgkEmployee }
            val unemployment = records.sumOf { it.unemployment }
            val incomeTax = records.sumOf { it.incomeTax }
            val stampTax = records.sumOf { it.stampTax }
            val bes = records.sumOf { it.bes }
            val other = records.sumOf { it.otherDeductions }
            val totalDeductions = records.sumOf { it.totalDeductions }
            val totalTax = incomeTax + stampTax

            PayrollSummaryUiState(
                isLoading = false,
                recordCount = records.size,
                totalNetSalary = totalNet,
                totalGrossEarnings = totalGross,
                totalDeductions = totalDeductions,
                totalTax = totalTax,
                totalNonTaxDeductions = (totalDeductions - totalTax).coerceAtLeast(0.0),
                socialSecurityTotal = socialSecurity,
                unemploymentTotal = unemployment,
                incomeTaxTotal = incomeTax,
                stampTaxTotal = stampTax,
                besTotal = bes,
                otherDeductionsTotal = other
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PayrollSummaryUiState()
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayrollSummaryScreen(
    onNavigateBack: () -> Unit,
    viewModel: PayrollSummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bordro Özeti") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Toplam ${uiState.recordCount} bordro kaydı",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SummaryValueCard(title = "Toplam Net Maaş", value = uiState.totalNetSalary)
            SummaryValueCard(title = "Toplam Vergi", value = uiState.totalTax)
            SummaryValueCard(title = "Vergi Dışı Kesinti", value = uiState.totalNonTaxDeductions)

            Spacer(modifier = Modifier.height(4.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Kesinti Dağılımı",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    DeductionProgressRow("SGK / SGDP", uiState.socialSecurityTotal, uiState.totalDeductions)
                    DeductionProgressRow("İşsizlik", uiState.unemploymentTotal, uiState.totalDeductions)
                    DeductionProgressRow("Gelir Vergisi", uiState.incomeTaxTotal, uiState.totalDeductions)
                    DeductionProgressRow("Damga Vergisi", uiState.stampTaxTotal, uiState.totalDeductions)
                    DeductionProgressRow("BES", uiState.besTotal, uiState.totalDeductions)
                    DeductionProgressRow("Diğer", uiState.otherDeductionsTotal, uiState.totalDeductions)

                    HorizontalDivider()
                    SummaryLine("Toplam Brüt", uiState.totalGrossEarnings)
                    SummaryLine("Toplam Kesinti", uiState.totalDeductions)
                }
            }
        }
    }
}

@Composable
private fun SummaryValueCard(
    title: String,
    value: Double
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatTry(value),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DeductionProgressRow(
    label: String,
    amount: Double,
    totalDeductions: Double
) {
    val progress = if (totalDeductions > 0.0) (amount / totalDeductions).coerceIn(0.0, 1.0) else 0.0

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${formatTry(amount)} • %${(progress * 100).toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { progress.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        )
    }
}

@Composable
private fun SummaryLine(
    label: String,
    amount: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatTry(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatTry(value: Double): String {
    return formatLocalizedCurrency(value)
}
