package com.example.feeder.ui.viewModel

import android.app.Application
import androidx.lifecycle.*
import com.example.feeder.data.body.LoginBody
import com.example.feeder.data.model.LoginResponse
import com.example.feeder.data.repository.AuthRepository
import com.example.feeder.data.repository.RequestStatus
import kotlinx.coroutines.launch

class LoginActivityViewModel(
    private val authRepository: AuthRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _user = MutableLiveData<LoginResponse?>()
    val user: LiveData<LoginResponse?> get() = _user

    fun loginUser(accessToken: String, body: LoginBody) {
        viewModelScope.launch {
            authRepository.loginUser(accessToken, body).collect { status ->
                when (status) {
                    is RequestStatus.Waiting -> _isLoading.value = true
                    is RequestStatus.Success -> {
                        _isLoading.value = false
                        _user.value = status.data
                    }
                    is RequestStatus.Error -> {
                        _isLoading.value = false
                        _errorMessage.value = status.message
                    }
                }
            }
        }
    }
}
