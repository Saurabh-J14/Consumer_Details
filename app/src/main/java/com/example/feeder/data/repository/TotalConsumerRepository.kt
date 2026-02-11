package com.example.feeder.data.repository

import com.example.feeder.data.model.TotalConsumerResponse
import com.example.feeder.data.remote.ApiInterface
import retrofit2.Response

class TotalConsumerRepository(
    private val apiInterface: ApiInterface
) {

    suspend fun getTotalConsumer(token: String): Response<TotalConsumerResponse> {
        return apiInterface.getTotalConsumer(token)
    }
}
