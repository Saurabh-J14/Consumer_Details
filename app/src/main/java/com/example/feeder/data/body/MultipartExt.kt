package com.example.feeder.data.body


import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

fun createImagePart(path: String): MultipartBody.Part {
    val file = File(path)

    val requestBody =
        file.asRequestBody("image/*".toMediaTypeOrNull())

    return MultipartBody.Part.createFormData(
        "Image",      // ⚠️ API key (Postman jaisa hi hona chahiye)
        file.name,
        requestBody
    )
}
