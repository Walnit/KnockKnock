package com.example.knockknock

import android.app.NotificationManager
import android.content.Context
import android.util.Base64
import androidx.work.*
import com.example.knockknock.database.MessageDatabase
import com.example.knockknock.networking.GetMessages
import com.example.knockknock.networking.ServerProperties
import com.example.knockknock.networking.structures.GetMessagesRequest
import com.example.knockknock.signal.KnockSignalProtocolStore
import com.example.knockknock.database.KnockMessage
import com.example.knockknock.utils.KnockNotificationManager
import com.example.knockknock.utils.PrefsHelper
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SignalMessage
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets

class MessageSyncWorker(context: Context, workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val retrofit = ServerProperties.getRetrofitInstance()
        val securePreferences = PrefsHelper(applicationContext).openEncryptedPrefs("secure_prefs")
        while (securePreferences.getString("name", null) == null) {
            // if this is true, account has not been set up yet
            delay(1000)
        }

        val getMessageRequest = GetMessagesRequest(
            securePreferences.getString("name", null)!!,
            Base64.encodeToString(
                Curve.calculateSignature(
                    KnockSignalProtocolStore(applicationContext).identityKeyPair.privateKey,
                    securePreferences.getString("name", null)!!
                        .toByteArray(StandardCharsets.UTF_8)
                ), Base64.NO_WRAP
            )
        )

        while (true) {
            val call = retrofit.create(GetMessages::class.java).getMessages(getMessageRequest)
            try {
                val result = call.execute()
                if (result.code() == 200) { // Means that we have new messages
                    val store = KnockSignalProtocolStore(applicationContext)
                    val secureContacts =
                        PrefsHelper(applicationContext).openEncryptedPrefs("secure_contacts")
                    val hiddenContacts =
                        PrefsHelper(applicationContext).openEncryptedPrefs("hidden_contacts")
                    val resultStr: String = result.body()!!.string() // This is a JSON string
                    // Structure of JSON is username mapped to an array of new messages
                    Json.parseToJsonElement(resultStr).jsonObject.toMap()
                        .forEach { (target, messages) ->
                            // For each target
                            val sessionCipher =
                                SessionCipher(store, SignalProtocolAddress(target, 1))
                            val knockMessages: ArrayList<KnockMessage> =
                                arrayListOf() // store plaintext messages
                            messages.jsonArray.forEach { message ->
                                // For each message

                                val scuffedContent = String(
                                    Base64.decode(
                                        message.jsonPrimitive.content,
                                        Base64.NO_WRAP
                                    )
                                ) // decrypt scuffed, raw data

                                // data is now 1 char which determines type of message
                                // and the rest is b64 data ciphertext
                                // below is to get ciphertext
                                val actualContent =
                                    scuffedContent.slice(1 until scuffedContent.length)

                                if (scuffedContent[0] == 'M') { // Its just a normal text message
                                    val plaintext: ByteArray = sessionCipher.decrypt(
                                        SignalMessage(
                                            Base64.decode(
                                                actualContent,
                                                Base64.NO_WRAP
                                            )
                                        )
                                    )

                                    knockMessages.add(
                                        KnockMessage(
                                            target,
                                            System.currentTimeMillis(),
                                            plaintext,
                                            KnockMessage.KnockMessageType.TEXT
                                        )
                                    )
                                } else if (scuffedContent[0] == 'P') { // Its a PreKeySignalMessage
                                    // This means its probably your first time interacting with the target

                                    if (!secureContacts.contains(target) && !hiddenContacts.all.values.contains(
                                            target
                                        )
                                    ) {
                                        // target not in contacts or hidden, must be new
                                        secureContacts.edit().putString(target, actualContent)
                                            .commit()
                                    }

                                    val plaintext: ByteArray = sessionCipher.decrypt(
                                        PreKeySignalMessage(
                                            Base64.decode(
                                                actualContent,
                                                Base64.NO_WRAP
                                            )
                                        )
                                    )

                                    knockMessages.add(
                                        KnockMessage(
                                            target,
                                            System.currentTimeMillis(),
                                            plaintext,
                                            KnockMessage.KnockMessageType.TEXT
                                        )
                                    )
                                } else if (scuffedContent[0] == 'I') { // ooh its an image
                                    val plaintext: ByteArray = sessionCipher.decrypt(
                                        SignalMessage(
                                            Base64.decode(
                                                actualContent,
                                                Base64.NO_WRAP
                                            )
                                        )
                                    )

                                    knockMessages.add(
                                        KnockMessage(
                                            target,
                                            System.currentTimeMillis(),
                                            plaintext,
                                            KnockMessage.KnockMessageType.IMAGE
                                        )
                                    )
                                }

                            }

                            MessageDatabase.writeMessages(
                                target, knockMessages.toTypedArray(), applicationContext
                            )

                            // if not hidden (aka in open contacts)
                            if (secureContacts.contains(target)) {
                                if (securePreferences.contains("current")) {
                                    if (securePreferences.getString("current", null) != target) {
                                        // If the person is chatting with the person we dont send any notif
                                        // User is NOT currently chatting with this person, but still using app
                                        // Dont vibrate but send notif now

                                        // send notification to alert of new messages
                                        // alert will not include chat content for privacy
                                        KnockNotificationManager.sendChatReplyNotification(
                                            KnockNotificationManager.createChatNotificationChannel(
                                                "com.example.knockknock.chatnotif",
                                                "New Message",
                                                "New Message from Knock Knock",
                                                NotificationManager.IMPORTANCE_HIGH,
                                                false,
                                                applicationContext
                                            ),
                                            "Message from $target",
                                            applicationContext
                                        )
                                    }
                                } else {
                                    // user is not chatting
                                    // full out notifs
                                    KnockNotificationManager.sendChatReplyNotification(
                                        KnockNotificationManager.createChatNotificationChannel(
                                            "com.example.knockknock.chatnotif",
                                            "New Message",
                                            "New Message from Knock Knock",
                                            NotificationManager.IMPORTANCE_HIGH,
                                            true,
                                            applicationContext
                                        ),
                                        "Message from $target",
                                        applicationContext
                                    )
                                }

                                // do preview text for chatslist if applicable
                                if (knockMessages.last().type == KnockMessage.KnockMessageType.TEXT) {
                                    secureContacts.edit()
                                        .putString(target, String(knockMessages.last().content))
                                        .apply()
                                } else {
                                    secureContacts.edit()
                                        .putString(target, "Image")
                                        .apply()
                                }
                            }

                        }
                } else if (result.code() == 204) {
                    // No new messages
                } else if (result.code() == 403) {
                    KnockNotificationManager.sendSystemNotification(
                        KnockNotificationManager.createSystemNotificationChannel(
                            applicationContext
                        ),
                        "Authentication Error",
                        "Have you set up the app correctly?",
                        applicationContext
                    )
                }
            } catch (e: ConnectException) {
                KnockNotificationManager.sendSystemNotification(
                    KnockNotificationManager.createSystemNotificationChannel(applicationContext),
                    "Network Error",
                    "The server may be offline!",
                    applicationContext
                )
            } catch (e: SocketTimeoutException) {
                e.printStackTrace()
                KnockNotificationManager.sendSystemNotification(
                    KnockNotificationManager.createSystemNotificationChannel(applicationContext),
                    "Network Error",
                    "Please check that you are connected to the internet!",
                    applicationContext
                )
            } catch (e: UnknownHostException) {
                e.printStackTrace()
                KnockNotificationManager.sendSystemNotification(
                    KnockNotificationManager.createSystemNotificationChannel(applicationContext),
                    "Network Error",
                    "Please check that you are connected to the internet!",
                    applicationContext
                )
            }
            delay(250)
        }
    }

}