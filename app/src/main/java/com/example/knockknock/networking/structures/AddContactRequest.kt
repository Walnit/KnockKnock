package com.example.knockknock.networking.structures

data class AddContactRequest(
    val requestor: String,
    val requestee: String,
    val knockClient: String,
    val sig: String
)
