package com.example.feeder.utils

import android.content.Context
import android.graphics.Bitmap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

object MultipartUtils {
    fun String.toPart(): RequestBody =
        this.toRequestBody("text/plain".toMediaTypeOrNull())

    fun createImagePart(context: Context, bitmap: Bitmap?): MultipartBody.Part {

        val file = File(context.cacheDir, "consumer_${System.currentTimeMillis()}.png")

        FileOutputStream(file).use {
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        val requestFile = file.asRequestBody("image/png".toMediaTypeOrNull())

        return MultipartBody.Part.createFormData(
            "Image",
            file.name,
            requestFile
        )
    }
}