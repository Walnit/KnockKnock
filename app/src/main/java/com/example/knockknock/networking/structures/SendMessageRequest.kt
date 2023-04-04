package com.example.knockknock.networking.structures

data class SendMessageRequest(
    val sender: String,
    val sendto: String,
    val content: String,
    val sig: String
)
