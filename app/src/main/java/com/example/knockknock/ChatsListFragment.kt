package com.example.knockknock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ChatsListFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.fragment_chatslist, container, false )

        val navController = findNavController()

        view.findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            navController.navigate(R.id.action_ChatsList_to_addContactFragment)
        }

        // Populate RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.chatslist_recyclerview)
        val layoutManager = LinearLayoutManager(context)
        val adapter = ChatsListRecyclerAdapter(arrayListOf("TextView","janjan","","",""))
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        return view
    }
}