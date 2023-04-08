package com.example.knockknock

import android.content.Context
import android.content.res.Resources
import android.content.res.Resources.Theme
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.knockknock.database.MessageDatabase
import com.example.knockknock.database.KnockMessage
import com.example.knockknock.utils.PrefsHelper
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

class MessagesRecyclerAdapter(private val target: String, private val context: Context): RecyclerView.Adapter<MessagesRecyclerAdapter.ViewHolder>() {
    private val username = PrefsHelper(context).openEncryptedPrefs("secure_prefs").getString("name", null)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v : View = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_messages,parent,false)
        return ViewHolder(v)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val messages = MessageDatabase.getMessages(target, context)!!
        holder.bindItems(messages[position], username!!, (position > 0 && messages[position-1].sender == messages[position].sender))
    }
    override fun getItemCount(): Int {
        val messages = MessageDatabase.getMessages(target, context)
        return messages?.size ?: 0
    }

    class ViewHolder(msgView: View) : RecyclerView.ViewHolder(msgView) {
        private var msgSender : TextView
        private var msgText : TextView
        private var msgTime : TextView
        private var imgView: ImageView
        private var cardView: CardView
        private var res: Resources
        private var theme: Theme

        private val stackedParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        private val normalParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        init {
            msgSender = msgView.findViewById(R.id.messages_card_sender)
            msgText = msgView.findViewById(R.id.messages_card_text)
            msgTime = msgView.findViewById(R.id.messages_card_time)
            imgView = msgView.findViewById(R.id.messages_card_image)
            cardView = msgView as CardView
            res = msgView.context.resources
            theme = msgView.context.theme
            stackedParams.marginStart = (res.displayMetrics.density * 12).toInt()
            stackedParams.topMargin = (res.displayMetrics.density * 3).toInt()

            normalParams.marginStart = (res.displayMetrics.density * 12).toInt()
            normalParams.topMargin = (res.displayMetrics.density * 8).toInt()
        }

        fun bindItems(msg : KnockMessage, username: String, condense: Boolean){

            if (msg.sender == username) {
                msgSender.setTextColor(res.getColor(R.color.orange_700, theme))
            } else {
                msgSender.setTextColor(res.getColor(R.color.teal_500, theme))
            }

            if (condense) {
                msgSender.visibility = View.GONE
                cardView.layoutParams = stackedParams
            }
            else {
                msgSender.visibility = View.VISIBLE
                cardView.layoutParams = normalParams
            }

            msgSender.text = msg.sender
            msgTime.text = SimpleDateFormat("HH:mm", res.configuration.locales.get(0)).format(Date(msg.timestamp))
            if (msg.type == KnockMessage.KnockMessageType.TEXT) {
                imgView.visibility = View.GONE
                msgText.visibility = View.VISIBLE
                msgText.text = String(msg.content, StandardCharsets.UTF_8)
            } else if (msg.type == KnockMessage.KnockMessageType.IMAGE) {
                imgView.visibility = View.VISIBLE
                msgText.visibility = View.GONE
                val bmp = BitmapFactory.decodeByteArray(msg.content, 0, msg.content.size)
                val imgDims = (res.displayMetrics.density * 300).toInt()
                if (bmp.width > bmp.height) {
                    imgView.setImageBitmap(Bitmap.createScaledBitmap(bmp, imgDims, (imgDims*(bmp.height.toFloat()/bmp.width)).toInt(), false))
                } else {
                    imgView.setImageBitmap(Bitmap.createScaledBitmap(bmp, (imgDims*(bmp.width.toFloat()/bmp.height)).toInt(), imgDims, false))
                }
            }
        }
    }
}
