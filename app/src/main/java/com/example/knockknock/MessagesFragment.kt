package com.example.knockknock

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.knockknock.database.MessageDatabase
import com.example.knockknock.networking.SendMessage
import com.example.knockknock.networking.ServerProperties
import com.example.knockknock.networking.structures.SendMessageRequest
import com.example.knockknock.signal.*
import com.example.knockknock.structures.KnockClient
import com.example.knockknock.structures.KnockClientSerializable
import com.example.knockknock.structures.KnockMessage
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SignalMessage
import java.net.ConnectException
import java.nio.charset.StandardCharsets

class MessagesFragment : Fragment() {

    lateinit var msgSyncJob: Job
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_messages, container, false)

        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        val target = requireArguments().getString("name")

        if (target != null) {

            val recyclerView = view.findViewById<RecyclerView>(R.id.messages_recyclerview)
            val retrofit = ServerProperties.getRetrofitInstance()

            // Signal magic as well

            val masterKey = MasterKey.Builder(requireContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

            var securePreferences = EncryptedSharedPreferences.create(
                requireContext(),
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val secureContacts = EncryptedSharedPreferences.create(
                requireContext(),
                "secure_contacts",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            if (!secureContacts.contains(target)) {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Error!")
                builder.setMessage("You have not requested to contact this user!")
                builder.setIcon(android.R.drawable.ic_dialog_alert)
                builder.setPositiveButton("Ok") {_,_->}
                builder.create().show()

                navController.navigate(R.id.action_messagesFragment_to_ChatsList)

            } else {

                val store = KnockSignalProtocolStore(requireContext())

                val sessionCipher = SessionCipher(
                    store,
                    SignalProtocolAddress(target, 1)
                )

                val layoutManager = LinearLayoutManager(requireContext())
                layoutManager.stackFromEnd = true
                recyclerView.layoutManager = layoutManager

                recyclerView.adapter = MessagesRecyclerAdapter(target, requireContext(), sessionCipher)

                view.findViewById<ImageButton>(R.id.messages_send_imgbtn).setOnClickListener {
                    val editText = view.findViewById<TextInputEditText>(R.id.messages_edittext)
                    if (!editText.text.isNullOrBlank()) {
                        val editTextContent: String = editText.text.toString()
                        CoroutineScope(IO).launch {

                            val cipherMessage = sessionCipher.encrypt(
                                    editTextContent.toByteArray(StandardCharsets.UTF_8)
                                )

                            val messageType: String
                            if (cipherMessage is PreKeySignalMessage) {
                                messageType = "P"
                                Log.i("TAG", "prekeymessage")
                            } else if (cipherMessage is SignalMessage){
                                messageType = "M"
                                Log.i("TAG", "message")
                            } else {
                                messageType = "O"
                            }

                            val serMessage = (messageType + Base64.encodeToString(cipherMessage.serialize(), Base64.NO_WRAP)).toByteArray(StandardCharsets.UTF_8)

                            val call = retrofit.create(SendMessage::class.java).sendMessage(
                                SendMessageRequest(
                                    securePreferences.getString("name", null)!!,
                                    target,
                                    Base64.encodeToString(serMessage, Base64.NO_WRAP),
                                    Base64.encodeToString(
                                        Curve.calculateSignature(
                                            store.identityKeyPair.privateKey,
                                            serMessage
                                        ), Base64.NO_WRAP
                                    )
                                )
                            )

                            try {
                                val result = call.execute()
                                if (result.code() == 200) {
                                    MessageDatabase.writeMessages(
                                        target, arrayOf(
                                            KnockMessage(
                                                store.getLocalKnockClient().name,
                                                System.currentTimeMillis(),
                                                editTextContent.toByteArray(StandardCharsets.UTF_8),
                                                KnockMessage.KnockMessageType.TEXT
                                            )
                                        ), requireContext()
                                    )

                                } else if (result.code() == 403) {
                                    withContext(Dispatchers.Main) {
                                        Snackbar.make(
                                            recyclerView,
                                            "Authentication Error",
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Snackbar.make(
                                            recyclerView,
                                            "Client out of date",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } catch (e: ConnectException) {
                                Snackbar.make(recyclerView, "Network Error", Snackbar.LENGTH_SHORT)
                                    .show()
                            }

                        }
                        editText.text!!.clear()
                    }
                }

                // Refresh views to get new messages

                msgSyncJob = CoroutineScope(IO).launch {
                    while (true) {
                        withContext(Main) {
                            (recyclerView.adapter as MessagesRecyclerAdapter).notifyItemRangeChanged(0, (recyclerView.adapter as MessagesRecyclerAdapter).itemCount)

                        }
                        delay(100)
                    }
                }
            }
        }

        return view
    }

    override fun onDestroy() {
        msgSyncJob.cancel()
        super.onDestroy()
    }

}