package com.example.knockknock.networking

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ServerProperties {
    companion object {
        const val IP_ADDRESS: String = "f0b7-119-74-126-97.ap.ngrok.io"
        fun getRetrofitInstance(): Retrofit {
            return Retrofit.Builder().baseUrl("https://$IP_ADDRESS")
                .addConverterFactory(GsonConverterFactory.create()).build()
        }
    }
}