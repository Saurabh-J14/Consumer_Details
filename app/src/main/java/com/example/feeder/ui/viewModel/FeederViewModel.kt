package com.example.feeder.ui.viewModel

import androidx.lifecycle.*
import com.example.feeder.data.repository.FeederRepository
import kotlinx.coroutines.launch

class FeederViewModel(
    private val repository: FeederRepository
) : ViewModel() {

    private val _feederList = MutableLiveData<List<String>>()
    val feederList: LiveData<List<String>> = _feederList

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun fetchFeederIds(token: String, substationName: String) {
        viewModelScope.launch {
            try {
                val response = repository.getFeederIds(
                    "Bearer $token",
                    substationName
                )

                if (response.isSuccessful && response.body()?.resCode == 200) {
                    _feederList.value =
                        response.body()?.resData?.data ?: emptyList()
                } else {
                    _error.value = response.body()?.resMessage ?: "API Error"
                }

            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Something went wrong"
            }
        }
    }
    fun clearFeederList() {
        _feederList.value = emptyList()
    }
}
