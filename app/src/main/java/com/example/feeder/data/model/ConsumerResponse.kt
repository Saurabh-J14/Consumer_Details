package com.example.feeder.data.model

data class ConsumerResponse(
    val resCode: Int, // 200
    val resData: ResData,
    val resMessage: String // Success!
) {
    data class ResData(
        val `data`: List<Data>
    ) {
        data class Data(
            val circle_Name: String, // PEN CIRCLE
            val consumerNumber: String, // 32510017002
            val consumerStatus: String, // No
            val createdOn: String, // 2026-01-13T12:51:17.617
            val division_Name: String, // ALIBAG DIVISION
            val dtC_Name: String, // ALIBAG2
            val feederId: String, // 013/223031/302
            val feeder_Name: String, // FEEDER-B
            val meterNumber: String, // 7474007002
            val mobileNo: Long, // 9653097406
            val phase: String, // R
            val region_Name: String, // KONKAN
            val sanctionedLoad: Double, // 2.0000
            val substation_Name: String, // ALIBAG SUBSTATION-1
            val voltage: Int, // 220
            val zone_Name: String // KALYAN ZONE
        )
    }
}