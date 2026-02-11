package com.example.feeder.data.repository

import com.example.feeder.data.body.ConsumerBody
import com.example.feeder.data.model.ConsumerResponse
import com.example.feeder.data.remote.ApiInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ConsumerRepository(private val apiInterface: ApiInterface) {

    fun getConsumerFlow(token: String, body: ConsumerBody): Flow<RequestStatus<ConsumerResponse.ResData.Data>> = flow {
        emit(RequestStatus.Waiting)
        try {
            val response = apiInterface.consumerNumber("Bearer $token", body)
            if (response.isSuccessful && response.body() != null) {
                val apiBody = response.body()!!
                if (apiBody.resCode == 200) {
                    val list = apiBody.resData.data
                    if (list.isNotEmpty()) {
                        emit(RequestStatus.Success(list[0]))
                    } else {
                        emit(RequestStatus.Error("No Consumer Found"))
                    }
                } else {
                    emit(RequestStatus.Error(apiBody.resMessage))
                }
            } else {
                emit(RequestStatus.Error(response.message()))
            }
        } catch (e: Exception) {
            emit(RequestStatus.Error(e.localizedMessage ?: "Something went wrong"))
        }
    }
}


