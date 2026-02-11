package com.example.feeder.ui.viewModel

import androidx.lifecycle.*
import com.example.feeder.data.repository.DtDeleteRepository
import kotlinx.coroutines.launch

class DtDeleteViewModel(
    private val repository: DtDeleteRepository
) : ViewModel() {

    private val _deleteResult = MutableLiveData<Result<String>>()
    val deleteResult: LiveData<Result<String>> = _deleteResult

    fun deleteDt(token: String, dtName: String) {

        viewModelScope.launch {
            try {
                val response = repository.deleteDt(token, dtName)

                when {
                    response.isSuccessful && response.body()?.resCode == 200 -> {
                        val msg = response.body()?.resData?.data
                            ?: response.body()?.resMessage
                            ?: "Deleted Successfully"

                        _deleteResult.postValue(Result.success(msg))
                    }

                    response.code() == 401 ->
                        _deleteResult.postValue(
                            Result.failure(Exception("Session expired"))
                        )

                    else ->
                        _deleteResult.postValue(
                            Result.failure(
                                Exception(response.body()?.resMessage ?: "Delete failed")
                            )
                        )
                }

            } catch (e: Exception) {
                _deleteResult.postValue(
                    Result.failure(Exception(e.localizedMessage ?: "Network error"))
                )
            }
        }
    }
}
