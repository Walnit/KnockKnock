package com.example.knockknock.structures

@kotlinx.serialization.Serializable
data class KnockClientSerializable(
    val name : String,
    val registrationID : Int,
    val deviceID : Int,
    val preKeyID : Int,
    val preKeyPublic : ByteArray,
    val signedPreKeyId : Int,
    val signedPreKeyPublic : ByteArray,
    val signedPreKeySignature : ByteArray,
    val identityKey : ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KnockClientSerializable

        if (name != other.name) return false
        if (registrationID != other.registrationID) return false
        if (deviceID != other.deviceID) return false
        if (preKeyID != other.preKeyID) return false
        if (!preKeyPublic.contentEquals(other.preKeyPublic)) return false
        if (signedPreKeyId != other.signedPreKeyId) return false
        if (!signedPreKeyPublic.contentEquals(other.signedPreKeyPublic)) return false
        if (!signedPreKeySignature.contentEquals(other.signedPreKeySignature)) return false
        if (!identityKey.contentEquals(other.identityKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + registrationID
        result = 31 * result + deviceID
        result = 31 * result + preKeyID
        result = 31 * result + preKeyPublic.contentHashCode()
        result = 31 * result + signedPreKeyId
        result = 31 * result + signedPreKeyPublic.contentHashCode()
        result = 31 * result + signedPreKeySignature.contentHashCode()
        result = 31 * result + identityKey.contentHashCode()
        return result
    }


}