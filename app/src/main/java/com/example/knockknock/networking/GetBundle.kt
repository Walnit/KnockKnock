package com.example.knockknock.networking

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

@Suppress("unused")
interface GetBundle {
    @GET("knock/getBundle/{name}")
    fun getBundle(@Path("name") name: String) : Call<ResponseBody>
}