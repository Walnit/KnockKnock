package com.example.knockknock.signal

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.knockknock.structures.KnockClient
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.ecc.ECKeyPair
import org.whispersystems.libsignal.state.*
import org.whispersystems.libsignal.state.impl.InMemorySessionStore
import org.whispersystems.libsignal.util.KeyHelper

class KnockSignalProtocolStore(val context: Context) : SignalProtocolStore {

    val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

    // Get Session store
    private val secureSessionStore = EncryptedSharedPreferences.create(
        context,
        "secure_sessionStore",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Get PreKey store
    private val securePreKeyStore = EncryptedSharedPreferences.create(
        context,
        "secure_pkStore",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Get Identity Key store
    private val secureIdentityKeyStore = EncryptedSharedPreferences.create(
        context,
        "secure_identityKeyStore",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Get secure prefs for Registration ID and IdentityKeyPair
    private val securePreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Get SignedPreKey store
    private val secureSignedPreKeyStore = EncryptedSharedPreferences.create(
        context,
        "secure_signedPkStore",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        return SignedPreKeyRecord(
            Base64.decode(
                secureSignedPreKeyStore.getString(signedPreKeyId.toString(), null), Base64.NO_WRAP
            )
        )
    }

    override fun loadSignedPreKeys(): MutableList<SignedPreKeyRecord> {
        val signedPreKeyRecords = mutableListOf<SignedPreKeyRecord>()
        secureSignedPreKeyStore.all.values.forEach { b64SignedPreKeyRecord ->
            signedPreKeyRecords.add(
                SignedPreKeyRecord(
                    Base64.decode(
                        b64SignedPreKeyRecord as String, Base64.NO_WRAP
                    )
                )
            )
        }
        return signedPreKeyRecords
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord?) {
        if (record != null) {
            secureSignedPreKeyStore.edit()
                .putString(
                    signedPreKeyId.toString(),
                    Base64.encodeToString(record.serialize(), Base64.NO_WRAP)
                )
                .apply()
        } else {
            removeSignedPreKey(signedPreKeyId)
        }
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        return secureSignedPreKeyStore.contains(signedPreKeyId.toString())
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        secureSignedPreKeyStore.edit().remove(signedPreKeyId.toString()).apply()
    }


    override fun getIdentityKeyPair(): IdentityKeyPair {
        return IdentityKeyPair(
            Base64.decode(
                securePreferences.getString("IKP", null),
                Base64.NO_WRAP
            )
        )
    }

    override fun getLocalRegistrationId(): Int {
        return securePreferences.getInt("RID", -1)
    }

    override fun saveIdentity(address: SignalProtocolAddress?, identityKey: IdentityKey?): Boolean {
        if (address != null) {
            val isReplacingPreviousKey =
                secureIdentityKeyStore.contains(address.name + "," + address.deviceId.toString())
            if (identityKey != null) {
                secureIdentityKeyStore.edit()
                    .putString(
                        address.name + "," + address.deviceId.toString(),
                        Base64.encodeToString(identityKey.serialize(), Base64.NO_WRAP)
                    )
                    .apply()
            } else {
                secureIdentityKeyStore.edit()
                    .remove(address.name + "," + address.deviceId.toString()).apply()
            }
            return isReplacingPreviousKey
        }
        return false
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress?,
        identityKey: IdentityKey?,
        direction: IdentityKeyStore.Direction?
    ): Boolean {
//        if (address != null) {
//            if (!secureIdentityKeyStore.contains(address.name + "," + address.deviceId.toString())) return true
//            else {
//                if (identityKey != null) {
//                    if (getIdentity(address)?.fingerprint == identityKey.fingerprint) {
//                        return true
//                    }
//                }
//            }
//        }
//        return false
        return true
    }

    override fun getIdentity(address: SignalProtocolAddress?): IdentityKey? {
        if (address != null) {
            return IdentityKey(
                Base64.decode(
                    secureIdentityKeyStore.getString(
                        address.name + "," + address.deviceId.toString(),
                        null
                    ), Base64.NO_WRAP
                ), 0
            )
        }
        return null
    }

    fun getLocalKnockClient(): KnockClient {
        return KnockClient.newClient(
            context, securePreferences.getString("name", null)!!,
            localRegistrationId, 1, loadSignedPreKeys()[0],
            identityKeyPair.publicKey
        )
    }

    fun getNewPreKeyID(): Int {
        val currentPreKeyID = securePreKeyStore.getInt("currentPreKeyID", 0)
        val maxPreKeyID = securePreKeyStore.getInt("maxPreKeyID", -1)
        if (currentPreKeyID > maxPreKeyID) {
            KeyHelper.generatePreKeys(currentPreKeyID + 1, maxPreKeyID + 100).forEach { preKey ->
                storePreKey(preKey.id, preKey)
            }
        }

        securePreKeyStore.edit().putInt("currentPreKeyID", currentPreKeyID + 1).apply()
        return currentPreKeyID + 1
    }

    fun setMaxPreKeyID(maxPreKeyID: Int) {
        securePreKeyStore.edit().putInt("maxPreKeyID", maxPreKeyID).apply()
    }

    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        return PreKeyRecord(
            Base64.decode(
                securePreKeyStore.getString(preKeyId.toString(), null), Base64.NO_WRAP
            )
        )
    }

    @SuppressLint("ApplySharedPref")
    override fun storePreKey(preKeyId: Int, record: PreKeyRecord?) {
        if (record != null) {
            if (preKeyId > securePreKeyStore.getInt("maxPreKeyID", -1)) {
                securePreKeyStore.edit().putInt("maxPreKeyID", preKeyId)
                    .commit() // Commit needed to make sure we don't skip pre keys
            }
            securePreKeyStore.edit()
                .putString(
                    preKeyId.toString(),
                    Base64.encodeToString(record.serialize(), Base64.NO_WRAP)
                )
                .apply()
        } else {
            removePreKey(preKeyId)
        }
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        return securePreKeyStore.contains(preKeyId.toString())
    }

    override fun removePreKey(preKeyId: Int) {
        securePreKeyStore.edit().remove(preKeyId.toString()).apply()
    }

    override fun loadSession(address: SignalProtocolAddress?): SessionRecord {
        var sessionRecord: SessionRecord? = null
        if (address != null) {
            if (containsSession(address)) {
                sessionRecord = SessionRecord(
                    Base64.decode(
                        secureSessionStore.getString(
                            address.name + "," + address.deviceId.toString(),
                            null
                        ), Base64.NO_WRAP
                    )
                )
                Log.i("TAG", "loading session for ${address?.name}")
            }
        }

        return sessionRecord ?: SessionRecord()
    }

    override fun getSubDeviceSessions(name: String?): MutableList<Int> {
        val subDeviceSessions = mutableListOf<Int>()
        secureSessionStore.all.keys.forEach { identifier ->
            if (identifier.split(",")[0] == name) {
                subDeviceSessions.add(identifier.split(",")[1].toInt())
            }
        }
        return subDeviceSessions
    }

    override fun storeSession(address: SignalProtocolAddress?, record: SessionRecord?) {
        if (address != null) {
            if (record != null) {
                secureSessionStore.edit()
                    .putString(
                        address.name + "," + address.deviceId.toString(),
                        Base64.encodeToString(record.serialize(), Base64.NO_WRAP)
                    )
                    .apply()
            } else {
                deleteSession(address)
            }
        }
    }

    override fun containsSession(address: SignalProtocolAddress?): Boolean {
        return if (address != null) {
            secureSessionStore.contains(address.name + "," + address.deviceId.toString())
        } else false
    }

    override fun deleteSession(address: SignalProtocolAddress?) {
        if (address != null) {
            secureSessionStore.edit().remove(address.name + "," + address.deviceId.toString())
                .apply()
        }
    }

    override fun deleteAllSessions(name: String?) {
        val storeEditor = secureSessionStore.edit()
        secureSessionStore.all.keys.forEach { identifier ->
            if (identifier.split(",")[0] == name) {
                storeEditor.remove(identifier)
            }
        }
        storeEditor.apply()
    }
}