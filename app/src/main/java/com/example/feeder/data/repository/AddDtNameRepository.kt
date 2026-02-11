package com.example.feeder.data.repository

import com.example.feeder.data.body.AddDtNameBody
import com.example.feeder.data.remote.ApiInterface

class AddDtNameRepository(
    private val api: ApiInterface
) {
    suspend fun addDtName(token: String, body: AddDtNameBody) =
        api.getAddDtName(token, body)
}
