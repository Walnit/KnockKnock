package com.example.knockknock.networking.structures

data class GetAddContactResultRequest(
    val requestor: String,
    val requestee: String,
    val sig: String
)
