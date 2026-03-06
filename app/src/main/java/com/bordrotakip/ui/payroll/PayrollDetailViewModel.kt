package com.bordrotakip.ui.payroll

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bordrotakip.data.repository.PayrollRecordRepository
import com.bordrotakip.domain.model.PayrollRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PayrollDetailUiState(
    val record: PayrollRecord? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PayrollDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val payrollRecordRepository: PayrollRecordRepository
) : ViewModel() {
    private val payrollId: Long = savedStateHandle.get<Long>("payrollId") ?: 0L

    private val _uiState = MutableStateFlow(PayrollDetailUiState())
    val uiState: StateFlow<PayrollDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val record = payrollRecordRepository.getById(payrollId)
            if (record == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Kayıt bulunamadı.") }
            } else {
                _uiState.update { it.copy(isLoading = false, record = record) }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            payrollRecordRepository.deleteById(payrollId)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }
}

