package com.example.feeder.data.repository

import com.example.feeder.data.body.DtnameBody
import com.example.feeder.data.remote.ApiInterface

class DtnameRepository(
    private val apiService: ApiInterface
) {

    suspend fun getDtNames(
        token: String,
        feederId: String
    ) = apiService.getDTName(
        token,
        DtnameBody(feederId)
    )
}
