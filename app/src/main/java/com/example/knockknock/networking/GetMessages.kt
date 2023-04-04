package com.example.knockknock.networking

import com.example.knockknock.networking.structures.GetMessagesRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface GetMessages {
    @POST("getMessages")
    fun getMessages(@Body getMessagesRequest: GetMessagesRequest) : Call<ResponseBody>
}