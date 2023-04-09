package com.example.knockknock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.transition.TransitionInflater
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*

class ChatsListFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.fragment_chatslist, container, false )
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.fade)
        val navController = findNavController()

        view.findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            navController.navigate(R.id.action_ChatsList_to_addContactFragment)
        }

        // Populate RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.chatslist_recyclerview)
        val layoutManager = LinearLayoutManager(context)

        val adapter = ChatsListRecyclerAdapter(requireContext())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        if (adapter.itemCount > 4) {
            view.findViewById<ConstraintLayout>(R.id.chatslist_empty_hint).visibility = View.GONE
        }

        return view
    }
}