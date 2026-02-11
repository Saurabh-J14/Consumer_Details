package com.example.feeder.data.model

data class UpdateResponse(
    val resCode: Int,
    val resData: ResData,
    val resMessage: String
) {
    data class ResData(
        val consumerNumber: String,
        val createdOn: String,
        val dtcCode: String,
        val dtcName: String,
        val feederId: String,
        val feeder_Name: String,
        val imageUrl: String,
        val latitude: String,
        val location: String,
        val longitude: String,
        val meterNumber: String,
        val mobileNo: Long,
        val phaseDesignation: String,
        val sanctionedLoad: Double,
        val substation_Name: String,
        val userID: Int,
        val voltage: Int
    )
}