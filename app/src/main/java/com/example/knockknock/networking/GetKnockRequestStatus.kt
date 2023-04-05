package com.example.knockknock.networking

import com.example.knockknock.networking.structures.AddContactRequest
import com.example.knockknock.networking.structures.GetAddContactResultRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface GetKnockRequestStatus {
    @POST("getKnockRequestStatus")
    fun getRequestStatus(@Body getAddContactResultRequest: GetAddContactResultRequest) : Call<ResponseBody>
}