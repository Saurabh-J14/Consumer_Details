package com.example.feeder.data.model

data class LoginResponse(
    val resCode: Int, // 200
    val resData: ResData,
    val resMessage: String // Login Successful
) {
    data class ResData(
        val mobileNo: Long, // 9599309421
        val password: String, // ttpl@123
        val token: String, // eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVc2VySUQiOiIxMDUyIiwiVXNlck5hbWUiOiJNYWhlbmRlciIsIk1vYmlsZU5vLiI6Ijk1OTkzMDk0MjEiLCJuYmYiOjE3NjgzNzE0ODQsImV4cCI6MTc3MTA0OTg4NCwiaWF0IjoxNzY4MzcxNDg0fQ.dh_nPPzifhLaIEMVFjsF8YBM3a_k1Bv4_rIMOd_BCKk
        val userId: Int, // 1052
        val userName: String // Mahender
    )
}