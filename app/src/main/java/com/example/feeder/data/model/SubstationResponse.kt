package com.example.feeder.data.model

data class SubstationResponse(
    val resCode: Int,
    val resData: ResData,
    val resMessage: String
) {
    data class ResData(
        val `data`: List<String>
    )
}