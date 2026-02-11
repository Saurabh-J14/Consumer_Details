package com.example.feeder.data.model

data class AddDtNameResponse(
    val resCode: Int, // 200
    val resData: Any, // null
    val resMessage: String // DTC Name already exists!
)