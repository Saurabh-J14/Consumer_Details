package com.example.feeder.data.repository

import com.example.feeder.data.model.DtDeleteResponse
import com.example.feeder.data.remote.ApiInterface
import retrofit2.Response

class DtDeleteRepository(
    private val api: ApiInterface
) {

    suspend fun deleteDt(
        token: String,
        dtName: String
    ): Response<DtDeleteResponse> {

        return api.deleteDtName(
            "Bearer $token",
            dtName
        )
    }
}


