package com.example.feeder.data.repository

import com.example.feeder.data.body.ListBody
import com.example.feeder.data.model.ListResponse
import com.example.feeder.data.remote.ApiInterface
import retrofit2.Response

class ListRepository(private val api: ApiInterface) {
    suspend fun getConsumerList(token: String, body: ListBody): Response<ListResponse> {
        return api.getList(token, body)
    }
}
