package com.example.knockknock

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.knockknock.database.MessageDatabase
import com.example.knockknock.networking.GetMessages
import com.example.knockknock.networking.SendMessage
import com.example.knockknock.networking.ServerProperties
import com.example.knockknock.networking.structures.GetMessagesRequest
import com.example.knockknock.networking.structures.SendMessageRequest
import com.example.knockknock.signal.KnockIdentityKeyStore
import com.example.knockknock.structures.KnockMessage
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.whispersystems.libsignal.ecc.Curve
import java.net.ConnectException
import java.nio.charset.StandardCharsets

class MessageSyncService : Service() {



    lateinit var masterKey: MasterKey

    lateinit var securePreferences: SharedPreferences
    val retrofit = ServerProperties.getRetrofitInstance()

    lateinit var syncJob: Job;

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        securePreferences = EncryptedSharedPreferences.create(
                this,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val getMessageRequest = GetMessagesRequest(
            securePreferences.getString("name", null)!!,
            Base64.encodeToString(
                Curve.calculateSignature(
                    KnockIdentityKeyStore(applicationContext).identityKeyPair.privateKey,
                    securePreferences.getString("name", null)!!
                        .toByteArray(StandardCharsets.UTF_8)
                ), Base64.NO_WRAP
            )
        )

        syncJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                val call = retrofit.create(GetMessages::class.java).getMessages(getMessageRequest)
                try {
                    val result = call.execute()
                    if (result.code() == 200) {
                        val resultStr: String = result.body()!!.string()
                        Json.parseToJsonElement(resultStr).jsonObject.toMap()
                            .forEach { (target, messages) ->
                                var knockMessages: Array<KnockMessage> = arrayOf()
                                messages.jsonArray.forEach { message ->
                                    knockMessages = knockMessages.plus(
                                        KnockMessage(
                                            target,
                                            System.currentTimeMillis(),
                                            Base64.decode(
                                                message.jsonPrimitive.content,
                                                Base64.NO_WRAP
                                            ),
                                            KnockMessage.KnockMessageType.TEXT
                                        )
                                    )
                                }
                                MessageDatabase.writeMessages(
                                    target, knockMessages, applicationContext
                                )
                            }
                    } else if (result.code() == 403) {
                        withContext(Dispatchers.Main) {
                            //                        Snackbar.make(
                            //                            recyclerView,
                            //                            "Authentication Error",
                            //                            Snackbar.LENGTH_LONG
                            //                        ).show()
                        }
                    } else if (result.code() != 204) {
//                        withContext(Dispatchers.Main) {
//                            //                        Snackbar.make(
//                            //                            recyclerView,
//                            //                            "Client out of date",
//                            //                            Snackbar.LENGTH_SHORT
//                            //                        ).show()
//                        }
                    }
                } catch (e: ConnectException) {
                    //                Snackbar.make(recyclerView, "Network Error", Snackbar.LENGTH_SHORT)
                    //                    .show()


                }

                delay(100)
            }

        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        syncJob.cancel()
        super.onDestroy()
    }

}