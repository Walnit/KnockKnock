package com.example.knockknock

import android.content.Context
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.knockknock.database.MessageDatabase
import com.example.knockknock.structures.KnockMessage
import com.google.protobuf.InvalidProtocolBufferException
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SignalMessage
import java.lang.reflect.InvocationTargetException
import java.nio.charset.StandardCharsets

class MessagesRecyclerAdapter(private val target: String, private val context: Context, private val sessionCipher : SessionCipher): RecyclerView.Adapter<MessagesRecyclerAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v : View = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_messages,parent,false)
            return ViewHolder(v, sessionCipher)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindItems(MessageDatabase.getMessages(target, context)!![position])
        }
        override fun getItemCount(): Int {
            val messages = MessageDatabase.getMessages(target, context)
            return messages?.size ?: 0
        }

        class ViewHolder(msgView: View, val sessionCipher: SessionCipher) : RecyclerView.ViewHolder(msgView) {
            private var msgSender : TextView
            private var msgText : TextView

            init {
                msgSender = msgView.findViewById(R.id.messages_card_sender)
                msgText = msgView.findViewById(R.id.messages_card_text)
            }
            fun bindItems(msg : KnockMessage){
                msgSender.text = msg.sender

                if (msg.type == KnockMessage.KnockMessageType.TEXT) {
                    msgText.text = String(msg.content, StandardCharsets.UTF_8)
                }

            }

        }

}