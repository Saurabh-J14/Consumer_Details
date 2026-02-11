package com.example.feeder.data.model

data class ConsumerUpdateResponse(
    val resCode: Int, // 200
    val resData: ResData,
    val resMessage: String // Consumer details updated successfully.
) {
    data class ResData(
        val resCode: Int, // 200
        val resData: ResData,
        val resMessage: String // Success!
    ) {
        data class ResData(
            val buildingID: String, // PE2P10Baa-120
            val circle_Name: String, // PEN CIRCLE
            val consumerNumber: String, // 32510031856
            val consumerStatus: String, // Yes
            val division_Name: String, // ALIBAG DIVISION
            val dtcCode: String, // 414201133
            val dtcName: String,
            val feederId: String, // 013/223003/307
            val meterNumber: String,
            val mobileNo: String,
            val phaseDesignation: String,
            val region_Name: String,
            val sanctionedLoad: String,
            val voltage: String,
            val zone_Name: String
        )
    }
}