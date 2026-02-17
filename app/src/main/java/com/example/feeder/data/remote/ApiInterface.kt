package com.example.feeder.data.remote

import com.example.feeder.data.body.AddDtNameBody
import com.example.feeder.data.body.ConsumerBody
import com.example.feeder.data.body.DtnameBody
import com.example.feeder.data.body.FeederIdBody
import com.example.feeder.data.body.ListBody
import com.example.feeder.data.body.LoginBody
import com.example.feeder.data.model.AddDtNameResponse
import com.example.feeder.data.model.ConsumerResponse
import com.example.feeder.data.model.ConsumerUpdateResponse
import com.example.feeder.data.model.CountResponse
import com.example.feeder.data.model.DtDeleteResponse
import com.example.feeder.data.model.DtNameResponse
import com.example.feeder.data.model.FeederResponse
import com.example.feeder.data.model.ListResponse
import com.example.feeder.data.model.LoginResponse
import com.example.feeder.data.model.PendingConsumerResponse
import com.example.feeder.data.model.SubstationResponse
import com.example.feeder.data.model.TotalConsumerResponse
import com.example.feeder.data.model.UpdateResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiInterface {

@POST("api/Consumer/Login")
suspend fun loginUser(
    @Header("Authorization") accessToken: String,
    @Body body: LoginBody
): Response<LoginResponse>

    @GET("api/Consumer/GetAllSubstation")
    suspend fun getSubstation(
        @Header("Authorization") token: String
    ): Response<SubstationResponse>

    @POST("api/Consumer/GetFeederIDBySubstationName")
    suspend fun getFeederID(
        @Header("Authorization") token: String,
        @Body body: FeederIdBody
    ): Response<FeederResponse>

    @POST("api/Consumer/GetDTCNAMEByFeederID")
    suspend fun getDTName(
        @Header("Authorization") token: String,
        @Body body: DtnameBody
    ): Response<DtNameResponse>

    @POST("api/Consumer/CreateNewDT")
    suspend fun getAddDtName(
        @Header("Authorization") token: String,
        @Body body: AddDtNameBody
    ): Response<AddDtNameResponse>

    @POST("api/Consumer/GetConsumerListByFeederID")
    suspend fun getList(
        @Header("Authorization") token: String,
        @Body body: ListBody
    ): Response<ListResponse>

    @POST("api/Consumer/GetConsumerDataByConsumerNumber")
    suspend fun consumerNumber(
        @Header("Authorization") token: String,
        @Body body: ConsumerBody
    ): Response<ConsumerResponse>


    @Multipart
    @PUT("api/Consumer/EditConsumerDetails?")
//    @FormUrlEncoded
    suspend fun updateConsumer(
        @Header("Authorization") token: String,
        @Query("ConsumerNumber") consumerNumber: String,
        @Part("MeterNumber") MeterNumber: RequestBody,
        @Part("FeederId") FeederId: RequestBody,
        @Part("Feeder_Name") Feeder_Name: RequestBody,
        @Part("Substation_Name") Substation_Name: RequestBody,
        @Part("PhaseDesignation") PhaseDesignation: RequestBody,
        @Part("Voltage") Voltage: RequestBody,
        @Part("DTCName") DTCName: RequestBody,
        @Part("DTCCode") DTCCode: RequestBody,
        @Part("Latitude") Latitude: RequestBody,
        @Part("Longitude") Longitude: RequestBody,
        @Part("Location") Location: RequestBody,
        @Part("UserID") UserID: RequestBody,
        @Part("SanctionedLoad") SanctionedLoad: RequestBody,
        @Part("MobileNo") MobileNo: RequestBody,
        @Part("CreatedOn") CreatedOn: RequestBody,
        @Part Image: MultipartBody.Part
    ): Response<ConsumerUpdateResponse>

    @DELETE("api/Consumer/DeleteDTCNAME")
    suspend fun deleteDtName(
        @Header("Authorization") token: String,
        @Query("DTCNAME") dtName: String
    ): Response<DtDeleteResponse>

    @GET("api/Consumer/GetCompletedCount")
    suspend fun getCountConsumer(
        @Header("Authorization")token: String
    ): Response<CountResponse>

    @GET("api/Consumer/TotalConsumer")
    suspend fun getTotalConsumer(
        @Header("Authorization")token: String
    ): Response<TotalConsumerResponse>

    @GET("/api/Consumer/PendingTotalConsumer")
    suspend fun getPendingConsumer(
        @Header("Authorization")token: String
    ): Response<PendingConsumerResponse>

}
