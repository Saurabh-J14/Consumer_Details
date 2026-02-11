package com.example.feeder.ui.viewModel

import androidx.lifecycle.*
import com.example.feeder.data.body.AddDtNameBody
import com.example.feeder.data.model.AddDtNameResponse
import com.example.feeder.data.repository.AddDtNameRepository
import kotlinx.coroutines.launch

class AddDtNameActivityViewModel(
    private val repo: AddDtNameRepository
) : ViewModel() {

    private val _addDtResult = MutableLiveData<Result<AddDtNameResponse>>()
    val addDtResult: LiveData<Result<AddDtNameResponse>> = _addDtResult

    fun addDt(token: String, body: AddDtNameBody) {
        viewModelScope.launch {
            try {
                val response = repo.addDtName(token, body)

                if (response.isSuccessful && response.body() != null) {

                    val result = response.body()!!

                    if (result.resCode == 200) {
                        _addDtResult.postValue(Result.success(result))
                    } else {
                        _addDtResult.postValue(
                            Result.failure(Exception(result.resMessage))
                        )
                    }

                } else {
                    _addDtResult.postValue(
                        Result.failure(Exception("Server error"))
                    )
                }

            } catch (e: Exception) {
                _addDtResult.postValue(Result.failure(e))
            }
        }
    }
}
