package com.example.knockknock.networking

import com.example.knockknock.networking.structures.AddUserRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface SendAddUser {
    @POST("addUser")
    fun addUser(@Body addUserRequest: AddUserRequest) : Call<ResponseBody>
}