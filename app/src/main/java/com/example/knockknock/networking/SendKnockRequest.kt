package com.example.knockknock.networking

import com.example.knockknock.networking.structures.AddContactRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface SendKnockRequest {
    @Headers("ngrok-skip-browser-warning: 1234")
    @POST("sendKnockRequest")
    fun addContact(@Body addContactRequest: AddContactRequest) : Call<ResponseBody>
}