package com.example.feeder.ui.viewModel

import android.util.Log
import androidx.lifecycle.*
import com.example.feeder.data.body.ConsumerBody
import com.example.feeder.data.model.ConsumerResponse
import com.example.feeder.data.repository.ConsumerRepository
import com.example.feeder.data.repository.RequestStatus
import kotlinx.coroutines.launch

class ConsumerActivityViewModel(private val repository: ConsumerRepository) : ViewModel() {

    val isLoading = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String>()
    val consumerData = MutableLiveData<ConsumerResponse.ResData.Data>()

    fun fetchConsumer(token: String, consumerNumber: String, dtcName: String) {
        viewModelScope.launch {
            repository.getConsumerFlow(token, ConsumerBody(consumerNumber, dtcName))
                .collect { status ->
                    when (status) {
                        is RequestStatus.Waiting -> isLoading.value = true
                        is RequestStatus.Success -> {
                            isLoading.value = false
                            consumerData.value = status.data
                        }
                        is RequestStatus.Error -> {
                            isLoading.value = false
                            errorMessage.value = status.message
                        }
                    }
                }
        }
    }
}