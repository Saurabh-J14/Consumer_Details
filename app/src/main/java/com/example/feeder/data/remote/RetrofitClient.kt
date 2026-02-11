package com.example.feeder.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

 object RetrofitClient {
 // private const val BASE_URL = "http://192.168.1.13:2908/"
 // private const val BASE_URL = "http://192.168.1.13:2933/"
  private const val BASE_URL = "http://103.8.43.35:2933/"

    fun getServices(): ApiInterface {

     val loggingInterceptor = HttpLoggingInterceptor { message ->
      android.util.Log.d("API_LOG", message)
     }.apply {
      level = HttpLoggingInterceptor.Level.BODY
     }

      val client: OkHttpClient = OkHttpClient.Builder()
      .addInterceptor(loggingInterceptor)
      .connectTimeout(60, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .writeTimeout(60, TimeUnit.SECONDS)
      .build()

     val builder: Retrofit.Builder = Retrofit.Builder()
      .baseUrl(BASE_URL)
      .client(client)
      .addConverterFactory(GsonConverterFactory.create())

     val retrofit: Retrofit = builder.build()
     return retrofit.create(ApiInterface::class.java)
   }

 }