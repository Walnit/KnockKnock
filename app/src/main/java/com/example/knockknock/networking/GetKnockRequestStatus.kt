package com.example.knockknock.networking

import com.example.knockknock.networking.structures.GetAddContactResultRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface GetKnockRequestStatus {
    @Headers("ngrok-skip-browser-warning: 1234")
    @POST("getKnockRequestStatus")
    fun getRequestStatus(@Body getAddContactResultRequest: GetAddContactResultRequest) : Call<ResponseBody>
}