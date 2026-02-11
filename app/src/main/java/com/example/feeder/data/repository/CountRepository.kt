package com.example.feeder.data.repository

import com.example.feeder.data.model.CountResponse
import com.example.feeder.data.remote.ApiInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CountRepository(
    private val apiInterface: ApiInterface
) {

    fun getConsumerCount(token: String): Flow<RequestStatus<CountResponse>> = flow {
        emit(RequestStatus.Waiting)

        try {
            val response = apiInterface.getCountConsumer(token)

            if (response.isSuccessful && response.body() != null) {
                emit(RequestStatus.Success(response.body()!!))
            } else {
                emit(RequestStatus.Error(response.message()))
            }

        } catch (e: Exception) {
            emit(RequestStatus.Error(e.localizedMessage ?: "Something went wrong"))
        }
    }
}
