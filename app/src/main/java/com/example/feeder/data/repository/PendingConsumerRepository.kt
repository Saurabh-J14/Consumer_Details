package com.example.feeder.data.repository

import com.example.feeder.data.model.PendingConsumerResponse
import com.example.feeder.data.remote.ApiInterface
import retrofit2.Response

class PendingConsumerRepository(
    private val apiInterface: ApiInterface
) {

    suspend fun getPendingConsumers(token: String):
            Response<PendingConsumerResponse> {
        return apiInterface.getPendingConsumer(token)
    }
}
