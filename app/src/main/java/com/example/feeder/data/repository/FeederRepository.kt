package com.example.feeder.data.repository

import com.example.feeder.data.body.FeederIdBody
import com.example.feeder.data.remote.ApiInterface

class FeederRepository(
    private val apiService: ApiInterface
) {

    suspend fun getFeederIds(
        token: String,
        substationName: String
    ) = apiService.getFeederID(
        token,
        FeederIdBody(substationName)
    )
}
