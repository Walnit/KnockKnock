package com.example.knockknock.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class KnockMessage (
    val sender: String,
    @PrimaryKey val timestamp: Long,
    val content: ByteArray,
    val type: KnockMessageType
) {
    enum class KnockMessageType {
        TEXT, IMAGE
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KnockMessage

        if (sender != other.sender) return false
        if (timestamp != other.timestamp) return false
        if (!content.contentEquals(other.content)) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sender.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}