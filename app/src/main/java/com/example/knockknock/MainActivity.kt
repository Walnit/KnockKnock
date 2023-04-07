package com.example.knockknock

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.forEach
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.knockknock.utils.PrefsHelper
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (sharedPreferences.getBoolean("isFirstTime", true)) {
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                var securePreferences = EncryptedSharedPreferences.create(
                    this,
                    "secure_prefs",
                    MasterKey.Builder(this)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                if (securePreferences.contains("name")) {
                    sharedPreferences.edit().putBoolean("isFirstTime", false).commit()
                    startService(Intent(this, MessageSyncService::class.java))
                }
            }.launch(Intent(this, OnboardingActivity::class.java))

        } else {
            startService(Intent(this, MessageSyncService::class.java))
        }
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(navController.graph, findViewById<DrawerLayout>(R.id.drawer_layout))
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        findViewById<NavigationView>(R.id.nav_view)
            .setupWithNavController(navController)
        toolbar.setupWithNavController(navController, appBarConfiguration)
        toolbar.inflateMenu(R.menu.toolbar_menu)

        navController.addOnDestinationChangedListener { _, destination, args ->
            toolbar.menu.forEach {menuItem ->
                menuItem.isVisible = false
            }
            toolbar.invalidateMenu()

            if (destination.id == R.id.messagesFragment) {

                if (args != null) {
                    toolbar.title = args.getString("name")
                    if (args.containsKey("hidden")) {
                        toolbar.menu.findItem(R.id.messages_menu_show).isVisible = true
                        toolbar.menu.findItem(R.id.messages_menu_show).setOnMenuItemClickListener {
                            val hiddenContacts = PrefsHelper(this).openEncryptedPrefs("hidden_contacts")
                            hiddenContacts.all.forEach { (t, any) ->
                                if (any is String && any == args.getString("name")) {
                                    hiddenContacts.edit().remove(t).apply()
                                    PrefsHelper(this).openEncryptedPrefs("secure_contacts").edit().putString(any, "unhidden").apply()
                                    Snackbar.make(toolbar, "Contact unhidden!", Snackbar.LENGTH_SHORT).show()
                                    toolbar.menu.findItem(R.id.messages_menu_show).isVisible = false
                                    toolbar.menu.findItem(R.id.messages_menu_hide).isVisible = true
                                }
                            }
                            true
                        }
                    } else {
                        toolbar.menu.findItem(R.id.messages_menu_hide).isVisible = true
                        toolbar.menu.findItem(R.id.messages_menu_hide).setOnMenuItemClickListener {
                            navController.navigate(R.id.action_messagesFragment_to_hideContactFragment, args)
                            true
                        }
                    }

                } else {
                    toolbar.title = "Messages"
                }
            } else if (destination.id == R.id.addContactFragment) {
                toolbar.title = "Add Contact"
            } else if (destination.id == R.id.knockCodeFragment) {
                toolbar.title = "Hidden Contacts"
            } else if (destination.id == R.id.hideContactFragment) {
                if (args != null) {
                    toolbar.title = "Hide " + args.getString("name")
                } else {
                    toolbar.title = "Hide Contact"
                }
            }
        }

    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v: View? = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }


}