package com.example.knockknock.networking

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

@Suppress("unused")
interface GetMessagesForClient {
    @GET("knock/receiveMessages/{receiver}/{sender}")
    fun getMessageList(@Path("receiver") receiverName: String, @Path("sender") senderName: String) : Call<ResponseBody>
}