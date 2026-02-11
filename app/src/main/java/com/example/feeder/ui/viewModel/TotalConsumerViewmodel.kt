package com.example.feeder.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feeder.data.model.TotalConsumerResponse
import com.example.feeder.data.repository.RequestStatus
import com.example.feeder.data.repository.TotalConsumerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TotalConsumerViewModel(
    private val repository: TotalConsumerRepository
) : ViewModel() {

    private val _consumerResult =
        MutableStateFlow<RequestStatus<TotalConsumerResponse>>(RequestStatus.Waiting)

    val consumerResult: StateFlow<RequestStatus<TotalConsumerResponse>> =
        _consumerResult

    fun getTotalConsumer(token: String) {
        viewModelScope.launch {
            try {
                _consumerResult.value = RequestStatus.Waiting

                val response = repository.getTotalConsumer(token)

                if (response.isSuccessful && response.body() != null) {
                    _consumerResult.value =
                        RequestStatus.Success(response.body()!!)
                } else {
                    _consumerResult.value =
                        RequestStatus.Error("Error ${response.code()}")
                }

            } catch (e: Exception) {
                _consumerResult.value =
                    RequestStatus.Error(e.message ?: "Network error")
            }
        }
    }
}
