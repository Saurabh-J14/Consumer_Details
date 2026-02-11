package com.example.feeder.utils

import android.content.Context
import android.graphics.Bitmap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

object ResponseDataUtils {

    fun bitmapToPngFile(context: Context, bitmap: Bitmap): File {
        val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.png")
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()
        return file
    }

    fun createImageMultipart(file: File): MultipartBody.Part {
        val requestBody = file.asRequestBody("image/png".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(
            "image",          // API key name
            file.name,
            requestBody
        )
    }


}