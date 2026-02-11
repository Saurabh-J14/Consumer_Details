package com.example.feeder.data.repository

import com.example.feeder.data.body.LoginBody
import com.example.feeder.data.model.LoginResponse
import com.example.feeder.data.remote.ApiInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AuthRepository(private val apiInterface: ApiInterface) {

    fun loginUser(accessToken: String, body: LoginBody): Flow<RequestStatus<LoginResponse>> = flow {
        emit(RequestStatus.Waiting)

        try {
            val response = apiInterface.loginUser("Bearer $accessToken", body)
            if (response.isSuccessful && response.body() != null) {
                emit(RequestStatus.Success(response.body()!!))
            } else {
                emit(RequestStatus.Error(response.message() ?: "Login failed"))
            }
        } catch (e: Exception) {
            emit(RequestStatus.Error(e.localizedMessage ?: "Unknown error"))
        }
    }
}
