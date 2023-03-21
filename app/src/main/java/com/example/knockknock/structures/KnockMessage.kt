package com.example.knockknock.structures

@kotlinx.serialization.Serializable
class KnockMessage (
    val sender: String,
    val timestamp: Long,
    val content: ByteArray,
    val type: KnockMessageType
) {
    enum class KnockMessageType() {
        TEXT
    }



}