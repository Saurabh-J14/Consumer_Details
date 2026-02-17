package com.example.feeder.data.model

data class ConsumerUpdateResponse(
    val resCode: Int,
    val resData: InnerData,
    val resMessage: String
)

data class InnerData(
    val resCode: Int,
    val resData: ConsumerData,
    val resMessage: String
)

data class ConsumerData(
    val buildingID: String,
    val circle_Name: String,
    val consumerNumber: String,
    val consumerStatus: String,
    val division_Name: String,
    val dtcCode: String,
    val dtcName: String,
    val feederId: String,
    val meterNumber: String,
    val mobileNo: String,
    val phaseDesignation: String,
    val region_Name: String,
    val sanctionedLoad: String,
    val voltage: String,
    val zone_Name: String
)
