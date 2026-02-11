package com.example.feeder.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feeder.data.model.CountResponse
import com.example.feeder.data.repository.CountRepository
import com.example.feeder.data.repository.RequestStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class CountConsumerViewModel(
    private val repository: CountRepository
) : ViewModel() {

    private val _countResult =
        MutableStateFlow<RequestStatus<CountResponse>>(RequestStatus.Waiting)

    val countResult: StateFlow<RequestStatus<CountResponse>>
        get() = _countResult

    fun getConsumerCount(token: String) {
        viewModelScope.launch {
            _countResult.value = RequestStatus.Waiting
            try {
                repository.getConsumerCount(token).collect { result ->
                    _countResult.value = result
                }
            } catch (e: IOException) {
                _countResult.value = RequestStatus.Error("Network Error: ${e.localizedMessage}")
            } catch (e: HttpException) {
                _countResult.value = RequestStatus.Error("Server Error: ${e.code()}")
            } catch (e: Exception) {
                _countResult.value = RequestStatus.Error("Unexpected Error: ${e.localizedMessage}")
            }
        }
    }
}
