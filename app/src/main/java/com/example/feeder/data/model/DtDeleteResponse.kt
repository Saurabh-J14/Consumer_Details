package com.example.feeder.data.model

data class DtDeleteResponse(
    val resCode: Int, // 200
    val resData: ResData,
    val resMessage: String // Success
) {
    data class ResData(
        val `data`: String // Record Deleted Successfully
    )
}