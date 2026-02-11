package com.example.feeder.ui.viewModel

import androidx.lifecycle.*
import com.example.feeder.data.repository.DtnameRepository
import kotlinx.coroutines.launch

class DtNameActivityViewModel(
    private val repository: DtnameRepository
) : ViewModel() {

    private val _dtNameList = MutableLiveData<List<String>>()
    val dtNameList: LiveData<List<String>> = _dtNameList

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun fetchDtNames(token: String, feederId: String) {
        viewModelScope.launch {
            try {
                val response = repository.getDtNames(
                    "Bearer $token",
                    feederId
                )

                if (response.isSuccessful && response.body()?.resCode == 200) {
                    _dtNameList.value =
                        response.body()?.resData?.data ?: emptyList()
                } else {
                    _errorMessage.value =
                        response.body()?.resMessage ?: "DT API Error"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Something went wrong"
            }
        }
    }
    fun clearDtNameList() {
        _dtNameList.value = emptyList()
    }
}
