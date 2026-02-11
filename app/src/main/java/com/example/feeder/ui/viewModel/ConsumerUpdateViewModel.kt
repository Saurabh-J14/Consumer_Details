package com.example.feeder.ui.viewModel

import ConsumerUpdateBody
import ConsumerUpdateRepository
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feeder.data.model.ConsumerUpdateResponse
import kotlinx.coroutines.launch

class ConsumerUpdateViewModel(
    private val repository: ConsumerUpdateRepository
) : ViewModel() {

    val updateResponse = MutableLiveData<ConsumerUpdateResponse>()
    val error = MutableLiveData<String>()

    fun updateConsumer(
        context: Context,
        token: String,
        body: ConsumerUpdateBody,
        bitmap: Bitmap?
    ) {
        viewModelScope.launch {
            try {
                val response = repository.updateConsumer(context, token, body, bitmap)

                if (response.isSuccessful && response.body() != null) {
                    updateResponse.postValue(response.body())
                } else {
                    when (response.code()) {
                        400 -> error.postValue("400")
                        401 -> error.postValue("401")
                        500 -> error.postValue("500")
                        else -> error.postValue("Error ${response.code()}")
                    }

                    Log.e(
                        "API_ERROR", "Code=${response.code()} Body=${response.errorBody()?.string()}"
                    )
                }

            } catch (e: Exception) {
                error.postValue(e.localizedMessage ?: "Unknown error")
            }
        }
    }
}

