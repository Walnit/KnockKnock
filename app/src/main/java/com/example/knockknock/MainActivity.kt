package com.example.knockknock

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.knockknock.databinding.ActivityMainBinding
import com.example.knockknock.signal.KnockPreKeyStore
import com.example.knockknock.signal.KnockSignedPreKeyStore
import com.google.android.material.navigation.NavigationView
import org.whispersystems.libsignal.util.KeyHelper

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (sharedPreferences.getBoolean("isFirstTime", true)) {
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                sharedPreferences.edit().putBoolean("isFirstTime", false).apply()


            }.launch(Intent(this, OnboardingActivity::class.java))

        }
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(navController.graph, findViewById<DrawerLayout>(R.id.drawer_layout))
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        findViewById<NavigationView>(R.id.nav_view)
            .setupWithNavController(navController)
        toolbar.setupWithNavController(navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { _, destination, args ->
            if (destination.id == R.id.messagesFragment) {
                if (args != null) {
                    toolbar.title = args.getString("name")
                } else {
                    toolbar.title = "Messages"
                }
            }
        }

    }

}