package com.example.feeder.ui.viewModel

import SubstationRepository
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SubstationActivityViewModel(
    private val repository: SubstationRepository
) : ViewModel() {
    private val _substationList = MutableLiveData<List<String>>()
    val substationList: LiveData<List<String>> = _substationList

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun fetchSubstations(token: String) {
        viewModelScope.launch {
            try {
                val response = repository.getSubstation("Bearer $token")

                if (response.isSuccessful && response.body()?.resCode == 200) {
                    _substationList.value =
                        response.body()?.resData?.data ?: emptyList()
                } else {
                    _errorMessage.value =
                        response.body()?.resMessage ?: "API Error"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Unknown error"
            }
        }
    }
    fun clearSubstationList() {
        _substationList.value = emptyList()
    }
}
