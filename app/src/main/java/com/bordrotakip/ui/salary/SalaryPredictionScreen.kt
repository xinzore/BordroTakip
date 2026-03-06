package com.bordrotakip.ui.salary

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bordrotakip.domain.model.EmploymentType
import com.bordrotakip.ui.theme.BordroTakipTheme
import com.bordrotakip.util.formatLocalizedCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryPredictionScreen(
    onNavigateBack: () -> Unit,
    viewModel: SalaryPredictionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    SalaryPredictionContent(uiState, onNavigateBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryPredictionContent(
    uiState: SalaryPredictionUiState,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Maaş Tahmini") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Tahmini Net Maaş Kartı
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tahmini Net Maaş",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatLocalizedCurrency(uiState.estimatedNet),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "± %${(uiState.confidenceRate * 100).toInt()} güven aralığı",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Vergi Dilimi Kartı
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Vergi Dilimi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "%${uiState.currentTaxBracket}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                            Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Devreden (ay başı)", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = formatLocalizedCurrency(uiState.cumulativeTaxBaseBefore),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Ay sonu: ${formatLocalizedCurrency(uiState.cumulativeTaxBaseAfter)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { uiState.bracketProgress.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sonraki dilime ${uiState.monthsToNextBracket} ay kaldı",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Kesinti Detayları
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text(
                        text = "Kesinti Detayları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val socialSecurityLabel = when (uiState.employmentType) {
                        EmploymentType.RETIRED -> "SGDP (%7,5)"
                        EmploymentType.STANDARD -> "SGK (%14)"
                    }
                    
                    DeductionRow("Brüt Toplam", uiState.totalEarnings)
                    DeductionRow(socialSecurityLabel, -uiState.sgkDeduction)
                    if (uiState.employmentType != EmploymentType.RETIRED) {
                        DeductionRow("İşsizlik (%1)", -uiState.unemploymentDeduction)
                    }
                    DeductionRow("Gelir Vergisi", -uiState.incomeTax)
                    DeductionRow("Damga Vergisi", -uiState.stampTax)
                    if (uiState.besDeduction > 0) {
                        DeductionRow("BES (%3)", -uiState.besDeduction)
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Net Maaş", fontWeight = FontWeight.Bold)
                        Text(
                            text = formatLocalizedCurrency(uiState.estimatedNet),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeductionRow(label: String, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = formatLocalizedCurrency(amount),
            style = MaterialTheme.typography.bodyMedium,
            color = if (amount < 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}

// ============ PREVIEW ============

@Preview(showBackground = true, name = "Maaş Tahmini")
@Composable
fun SalaryPredictionScreenPreview() {
    BordroTakipTheme {
        SalaryPredictionContent(
            uiState = SalaryPredictionUiState(
                estimatedNet = 28078.0,
                totalEarnings = 36518.0,
                employmentType = EmploymentType.STANDARD,
                sgkDeduction = 5112.0,
                unemploymentDeduction = 365.0,
                incomeTax = 1787.0,
                stampTax = 79.0,
                besDeduction = 1095.0,
                currentTaxBracket = 20,
                cumulativeTaxBaseBefore = 180000.0,
                cumulativeTaxBaseAfter = 213000.0,
                bracketProgress = 0.85,
                monthsToNextBracket = 2
            ),
            onNavigateBack = {}
        )
    }
}
