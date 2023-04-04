package com.example.knockknock.networking

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ServerProperties {
    companion object {
        const val IP_ADDRESS: String = "192.168.1.127:56743"
        fun getRetrofitInstance(): Retrofit {
            return Retrofit.Builder().baseUrl("http://$IP_ADDRESS")
                .addConverterFactory(GsonConverterFactory.create()).build()
        }
    }
}