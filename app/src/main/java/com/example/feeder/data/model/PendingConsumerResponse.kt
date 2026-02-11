package com.example.feeder.data.model

data class PendingConsumerResponse(
    val resCode: Int, // 200
    val resData: ResData,
    val resMessage: String // Succuss
) {
    data class ResData(
        val `data`: List<Data>,
        val totalCount: Int // 13
    ) {
        data class Data(
            val circle_Name: String, // PEN CIRCLE
            val consumerNumber: String, // 3251001700121
            val consumerStatus: String, // No
            val createdOn: String, // 2026-01-13T12:51:17.617
            val division_Name: String, // ALIBAG DIVISION
            val dtC_Code: String, // 4142001
            val dtC_Name: String, // ALIBAG1
            val feederID: String, // 013/223031/301
            val feeder_Name: String, // FEEDER-A
            val meterNumber: String, // 74740070001
            val mobileNo: Long, // 9653097406
            val phaseDesignation: String, // 3
            val region_Name: String, // KONKAN
            val sanctionedLoad: Double, // 1.5000
            val substation_Name: String, // ALIBAG SUBSTATION-1
            val voltage: Int, // 230
            val zone_Name: String // KALYAN ZONE
        )
    }
}