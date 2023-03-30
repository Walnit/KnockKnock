package com.example.knockknock.onboarding

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDrawableOrThrow
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.knockknock.MainActivity
import com.example.knockknock.MessagesRecyclerAdapter
import com.example.knockknock.R
import com.example.knockknock.database.MessageDatabase
import com.example.knockknock.signal.KnockPreKeyStore
import com.example.knockknock.signal.KnockSignedPreKeyStore
import com.example.knockknock.structures.KnockMessage
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.*
import org.whispersystems.libsignal.util.KeyHelper
import java.nio.charset.StandardCharsets

class Page2 : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.onboarding_pg2_username, container, false)

        with (view) {
            val textInputLayout = findViewById<TextInputLayout>(R.id.ob_name_layout)
            val textInputEditText = findViewById<TextInputEditText>(R.id.ob_name_edittext)
            val continueButton = findViewById<Button>(R.id.ob_2_continue_btn)

            val states = ColorStateList(arrayOf(intArrayOf()), intArrayOf(context.fetchPrimaryColor()))

            var currentUsernameCheckCoroutine: Job? = null

            continueButton.setOnClickListener {
                // Signal magic below

                val username : String = textInputEditText.text.toString()

                val masterKey = MasterKey.Builder(requireContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

                var securePreferences = EncryptedSharedPreferences.create(
                    requireContext(),
                    "secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                // Generate required information

                val identityKeyPair = KeyHelper.generateIdentityKeyPair()
                val registrationId = KeyHelper.generateRegistrationId(false)
                val preKeys = KeyHelper.generatePreKeys(1, 100)
                val signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, 1)

                // Save required information to secure preferences

                securePreferences.edit()
                    .putString("IKP", Base64.encodeToString(identityKeyPair.serialize(), Base64.NO_WRAP))
                    .putInt("RID", registrationId)
                    .putString("name", username)
                    .apply() // Store IdentityKeyPair and RegistrationID

                val preKeyStore = KnockPreKeyStore(requireContext())
                preKeyStore.setMaxPreKeyID(100)
                preKeys.forEach {preKey ->
                    preKeyStore.storePreKey(preKey.id, preKey)
                } // Store PreKeys

                KnockSignedPreKeyStore(requireContext()).storeSignedPreKey(signedPreKey.id, signedPreKey)

                requireActivity().finish()
            }

            textInputEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                    // Reset states of indeterminate things
                    currentUsernameCheckCoroutine?.cancel()
                    continueButton.isEnabled = false

                    textInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
                    textInputLayout.setEndIconTintList(states)
                    textInputLayout.endIconDrawable = context.getProgressBarDrawable()
                    textInputLayout.endIconDrawable!!.setTintList(states)
                    (textInputLayout.endIconDrawable as? Animatable)?.start()

                    currentUsernameCheckCoroutine = CoroutineScope(Dispatchers.IO).launch {
                        delay(1000)
                        if (true) {
                            withContext(Dispatchers.Main) {
                                textInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
                                textInputLayout.endIconDrawable = context.getDrawable(R.drawable.baseline_check_circle_24)
                                continueButton.isEnabled = true
                            }
                        }
                    }
                }
            })
        }

        return view
    }

    // Creds https://antimonit.github.io/2019/08/04/textinputlayout_loading_indicator.html
    fun Context.getProgressBarDrawable(): Drawable {
        val value = TypedValue()
        theme.resolveAttribute(android.R.attr.progressBarStyleSmall, value, false)
        val progressBarStyle = value.data
        val attributes = intArrayOf(android.R.attr.indeterminateDrawable)
        val array = obtainStyledAttributes(progressBarStyle, attributes)
        val drawable = array.getDrawableOrThrow(0)
        array.recycle()
        return drawable
    }

    fun Context.fetchPrimaryColor(): Int {
        val array = obtainStyledAttributes(intArrayOf(android.R.attr.colorPrimary))
        val color = array.getColorOrThrow(0)
        array.recycle()
        return color
    }
}
