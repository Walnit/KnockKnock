package com.example.knockknock.structures

import android.content.Context
import com.example.knockknock.signal.KnockSignalProtocolStore
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.ecc.ECPublicKey
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord

@Suppress("unused")
class KnockClient(
    val name : String,
    private val registrationID : Int,
    private val deviceID : Int,
    private val preKeyID : Int,
    private val preKeyPublic : ECPublicKey,
    private val signedPreKeyId : Int,
    private val signedPreKeyPublic : ECPublicKey,
    private val signedPreKeySignature : ByteArray,
    private val identityKey : IdentityKey
) {
    companion object {
        fun newClient(context: Context, name: String, registrationID: Int, deviceID: Int, signedPreKeyRecord: SignedPreKeyRecord, identityKey: IdentityKey, preKeyRecord: PreKeyRecord? = null) : KnockClient {
            return if (preKeyRecord == null) {
                val store = KnockSignalProtocolStore(context)
                val preKeyID = store.getNewPreKeyID()
                KnockClient(name, registrationID, deviceID, preKeyID, store.loadPreKey(preKeyID).keyPair.publicKey,
                    signedPreKeyRecord.id, signedPreKeyRecord.keyPair.publicKey, signedPreKeyRecord.signature, identityKey)
            } else {
                KnockClient(name, registrationID, deviceID, preKeyRecord.id, preKeyRecord.keyPair.publicKey,
                    signedPreKeyRecord.id, signedPreKeyRecord.keyPair.publicKey, signedPreKeyRecord.signature, identityKey)
            }
        }
        fun fromSerialized(serializedClient: KnockClientSerializable) : KnockClient {
            return KnockClient(serializedClient.name, serializedClient.registrationID, serializedClient.deviceID,
                serializedClient.preKeyID, Curve.decodePoint(serializedClient.preKeyPublic, 0),
                serializedClient.signedPreKeyId, Curve.decodePoint(serializedClient.signedPreKeyPublic, 0),
                serializedClient.signedPreKeySignature,
                IdentityKey(serializedClient.identityKey, 0))
        }
    }

    fun getPreKeyBundle() : PreKeyBundle {
        return PreKeyBundle(registrationID, deviceID, preKeyID, preKeyPublic, signedPreKeyId, signedPreKeyPublic, signedPreKeySignature, identityKey)
    }

    fun toSerializableClient() : KnockClientSerializable {
        return KnockClientSerializable(name, registrationID, deviceID, preKeyID, preKeyPublic.serialize(), signedPreKeyId, signedPreKeyPublic.serialize(), signedPreKeySignature, identityKey.serialize())
    }
}