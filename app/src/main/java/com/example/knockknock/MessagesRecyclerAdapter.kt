package com.example.knockknock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.knockknock.structures.KnockMessage
import java.nio.charset.StandardCharsets

class MessagesRecyclerAdapter(private val messageList: Array<KnockMessage>): RecyclerView.Adapter<MessagesRecyclerAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v : View = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_messages,parent,false)
            return ViewHolder(v)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindItems(messageList[position])
        }
        override fun getItemCount() = messageList.size

        class ViewHolder(msgView: View) : RecyclerView.ViewHolder(msgView) {
            private var msgSender : TextView
            private var msgText : TextView

            init {
                msgSender = msgView.findViewById(R.id.messages_card_sender)
                msgText = msgView.findViewById(R.id.messages_card_text)
            }
            fun bindItems(msg : KnockMessage){
                msgSender.text = msg.sender
                msgText.text = String(msg.content, StandardCharsets.UTF_8)
            }

        }

}