package com.example.knockknock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView

class ChatsListRecyclerAdapter(val chats: ArrayList<String>) : RecyclerView.Adapter<ChatsListRecyclerAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v : View = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_chatslist,parent,false)
        return ViewHolder(v)

    }

    override fun getItemCount(): Int {
        return chats.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(chats[position])
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var chatProfile: ImageView
        var chatTitle: TextView
        var chatPreview: TextView
        init {
            chatProfile = itemView.findViewById(R.id.chatslist_card_profile)
            chatTitle = itemView.findViewById(R.id.chatslist_card_title)
            chatPreview = itemView.findViewById(R.id.chatslist_card_preview)
            itemView.setOnClickListener{ view ->
                val bundle = bundleOf("name" to chatTitle.text)
                view.findNavController().navigate(R.id.action_ChatsList_to_messagesFragment, args=bundle)
            }

        }
        fun bindItems(chat: String){
//            chatTitle.text = chp.title
//            chatD.text = chp.detail
//            itemImage.setImageResource(chp.images)
        }
    }

}