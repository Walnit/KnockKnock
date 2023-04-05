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

        val masterKey = MasterKey.Builder(requireContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

        var secureContacts = EncryptedSharedPreferences.create(
            requireContext(),
            "secure_contacts",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val adapter = ChatsListRecyclerAdapter(secureContacts.all.keys.toTypedArray())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        if (secureContacts.all.size > 4) {
            view.findViewById<ConstraintLayout>(R.id.chatslist_empty_hint).visibility = View.GONE
        }

        return view
    }
}