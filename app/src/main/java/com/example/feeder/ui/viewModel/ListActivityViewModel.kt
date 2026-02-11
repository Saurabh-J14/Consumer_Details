package com.example.feeder.ui.viewModel

import android.util.Log
import androidx.lifecycle.*
import com.example.feeder.data.body.ListBody
import com.example.feeder.data.repository.ListRepository
import kotlinx.coroutines.launch

class ListActivityViewModel(
    private val repository: ListRepository
) : ViewModel() {

    private val _consumerList = MutableLiveData<List<String>>()
    val consumerList: LiveData<List<String>> = _consumerList

    fun fetchConsumerList(token: String, feederId: String) {

        val body = ListBody(feederId)
        val authToken = "Bearer $token"

        viewModelScope.launch {
            try {
                val response = repository.getConsumerList(authToken, body)

                if (response.isSuccessful && response.body() != null) {
                    _consumerList.postValue(response.body()!!.resData.data)
                } else {
                    _consumerList.postValue(emptyList())
                }

            } catch (e: Exception) {
                _consumerList.postValue(emptyList())
            }
        }
    }
}