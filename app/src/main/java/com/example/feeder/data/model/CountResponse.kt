package com.example.feeder.data.model

data class CountResponse(
    val resCode: Int, // 200
    val resData: ResData,
    val resMessage: String // Succuss
) {
    data class ResData(
        val `data`: List<Data>,
        val totalCount: Int // 3
    ) {
        data class Data(
            val consumerNumber: String, // 32510017002
            val createdOn: String, // 2026-02-02T11:32:09.693
            val dtcCode: String, // 356
            val dtcName: String, // ALIBAG3
            val feederId: String, // 013/223031/304
            val feeder_Name: String, // FEEDER-C
            val imageUrl: String, // /images/70881638-198d-428d-bc8c-253da2d2f988.jpg
            val latitude: String, // 28.6427572
            val location: String, // Karol Bagh, New Delhi, Delhi Division, Delhi
            val longitude: String, // 77.2051741
            val meterNumber: String, // 7474007004
            val mobileNo: Long, // 9653097406
            val phaseDesignation: String, // 1
            val sanctionedLoad: Double, // 2.5000
            val substation_Name: String, // ALIBAG SUBSTATION-2
            val userID: Int, // 1052
            val voltage: Int // 220
        )
    }
}