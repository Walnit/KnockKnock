package com.example.knockknock.networking

import com.example.knockknock.networking.structures.UserExistsRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface GetUsernameExists {
    @Headers("ngrok-skip-browser-warning: 1234")
    @POST("userExists")
    fun usernameExists(@Body userExistsRequest: UserExistsRequest) : Call<ResponseBody>
}