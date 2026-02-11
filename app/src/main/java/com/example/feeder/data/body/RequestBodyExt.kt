package com.example.feeder.data.body

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

fun String.toRB(): RequestBody =
    this.toRequestBody("text/plain".toMediaTypeOrNull())
