package com.example.knockknock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.knockknock.database.MessageDatabase
import com.example.knockknock.structures.KnockMessage
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

class MessagesFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.fragment_messages, container, false )

        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        val target = requireArguments().getString("name")

        if (target != null) {

            val recyclerView = view.findViewById<RecyclerView>(R.id.messages_recyclerview)

            val layoutManager = LinearLayoutManager(requireContext())
            layoutManager.stackFromEnd = true
            recyclerView.layoutManager = layoutManager

            recyclerView.adapter = MessageDatabase.getMessages(target, requireContext())
            ?.let { MessagesRecyclerAdapter(it) }

            view.findViewById<ImageButton>(R.id.messages_send_imgbtn).setOnClickListener {
                val editText = view.findViewById<TextInputEditText>(R.id.messages_edittext)
                if (!editText.text.isNullOrBlank()) {
                    val editTextContent : String = editText.text.toString()
                    CoroutineScope(IO).launch {
                        MessageDatabase.writeMessages(target, arrayOf(KnockMessage(
                            target,
                            System.currentTimeMillis(),
                            editTextContent.toByteArray(StandardCharsets.UTF_8),
                            KnockMessage.KnockMessageType.TEXT
                        )), requireContext())
                        val newAdapter = MessageDatabase.getMessages(target, requireContext())
                            ?.let { MessagesRecyclerAdapter(it) }
                        withContext(Main) {
                            recyclerView.adapter = newAdapter
                        }
                    }
                    editText.text!!.clear()
                }
            }
        }

        return view
    }
}