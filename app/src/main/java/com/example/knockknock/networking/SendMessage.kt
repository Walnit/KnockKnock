package com.example.knockknock.networking

import com.example.knockknock.networking.structures.SendMessageRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface SendMessage {
    @Headers("ngrok-skip-browser-warning: 1234")
    @POST("sendMessage")
    fun sendMessage(@Body sendMessageRequest: SendMessageRequest) : Call<ResponseBody>
}