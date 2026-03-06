package com.bordrotakip.ui.payroll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bordrotakip.data.repository.PayrollRecordRepository
import com.bordrotakip.domain.model.PayrollRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PayrollListUiState(
    val records: List<PayrollRecord> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class PayrollListViewModel @Inject constructor(
    payrollRecordRepository: PayrollRecordRepository
) : ViewModel() {
    val uiState: StateFlow<PayrollListUiState> = payrollRecordRepository.getAllRecords()
        .map { records -> PayrollListUiState(records = records, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PayrollListUiState()
        )
}

