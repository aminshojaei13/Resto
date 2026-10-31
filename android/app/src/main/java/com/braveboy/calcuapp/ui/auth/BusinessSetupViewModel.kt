package com.braveboy.calcuapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.braveboy.calcuapp.data.repository.AuthRepository
import com.braveboy.calcuapp.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BusinessSetupUiState(
    val storeName: String = "",
    val ownerName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class BusinessSetupViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessSetupUiState())
    val uiState: StateFlow<BusinessSetupUiState> = _uiState.asStateFlow()

    fun onStoreNameChanged(value: String) {
        _uiState.value = _uiState.value.copy(storeName = value, errorMessage = null)
    }

    fun onOwnerNameChanged(value: String) {
        _uiState.value = _uiState.value.copy(ownerName = value, errorMessage = null)
    }

    fun setupBusiness() {
        val state = _uiState.value
        val storeName = state.storeName.trim()
        val ownerName = state.ownerName.trim()

        if (storeName.isBlank()) {
            _uiState.value = state.copy(errorMessage = "لطفاً نام فروشگاه یا کسب‌وکار خود را وارد کنید.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            val result = authRepository.setupLocalBusiness(
                storeName = storeName,
                ownerName = ownerName.ifBlank { "مدیر سیستم" }
            )
            when (result) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        errorMessage = null
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BusinessSetupViewModel(authRepository) as T
        }
    }
}
