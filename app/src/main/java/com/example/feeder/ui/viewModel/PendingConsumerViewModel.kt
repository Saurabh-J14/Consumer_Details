package com.example.feeder.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feeder.data.model.PendingConsumerResponse
import com.example.feeder.data.repository.PendingConsumerRepository
import com.example.feeder.data.repository.RequestStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PendingConsumerViewModel(
    private val repository: PendingConsumerRepository
) : ViewModel() {

    private val _pendingResult =
        MutableStateFlow<RequestStatus<PendingConsumerResponse>>(RequestStatus.Waiting)

    val pendingResult: StateFlow<RequestStatus<PendingConsumerResponse>> = _pendingResult

    fun getPendingConsumers(token: String) {
        viewModelScope.launch {
            _pendingResult.value = RequestStatus.Waiting

            try {
                val response = repository.getPendingConsumers(token)

                if (response.isSuccessful && response.body() != null) {
                    _pendingResult.value =
                        RequestStatus.Success(response.body()!!)
                } else {
                    _pendingResult.value =
                        RequestStatus.Error(
                            "Error ${response.code()}"
                        )
                }
            } catch (e: Exception) {
                _pendingResult.value =
                    RequestStatus.Error(e.localizedMessage ?: "Something went wrong")
            }
        }
    }
}
