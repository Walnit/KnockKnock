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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDrawableOrThrow
import androidx.fragment.app.Fragment
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.knockknock.R
import com.example.knockknock.networking.GetUsernameExists
import com.example.knockknock.networking.SendAddUser
import com.example.knockknock.networking.ServerProperties
import com.example.knockknock.networking.structures.AddUserRequest
import com.example.knockknock.networking.structures.UserExistsRequest
import com.example.knockknock.signal.KnockSignalProtocolStore
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import org.whispersystems.libsignal.util.KeyHelper
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.ConnectException

class Page2 : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.onboarding_pg2_username, container, false)

        with(view) {
            val textInputLayout = findViewById<TextInputLayout>(R.id.ob_pg2_textlayout)
            val textInputEditText = findViewById<TextInputEditText>(R.id.ob_pg2_edittext)
            val continueButton = findViewById<Button>(R.id.ob_pg2_continue_btn)
            val retrofit = ServerProperties.getRetrofitInstance()

            val states =
                ColorStateList(arrayOf(intArrayOf()), intArrayOf(context.fetchPrimaryColor()))

            var currentUsernameCheckCoroutine: Job? = null

            continueButton.setOnClickListener {
                // Signal magic below

                val username: String = textInputEditText.text.toString()

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

                val call: Call<ResponseBody> = retrofit.create(SendAddUser::class.java).addUser(
                    AddUserRequest(
                        textInputLayout.editText?.text.toString(),
                        Base64.encodeToString(
                            identityKeyPair.publicKey.publicKey.serialize(),
                            Base64.NO_WRAP
                        )
                    )
                )
                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = call.execute()
                        if (result.code() == 200) {
                            withContext(Dispatchers.Main) {

                                // Save required information to secure preferences
                                securePreferences.edit()
                                    .putString(
                                        "IKP",
                                        Base64.encodeToString(identityKeyPair.serialize(), Base64.NO_WRAP)
                                    )
                                    .putInt("RID", registrationId)
                                    .putString("name", username)
                                    .commit() // Store IdentityKeyPair and RegistrationID

                                val store = KnockSignalProtocolStore(requireContext())
                                store.setMaxPreKeyID(100)
                                preKeys.forEach { preKey ->
                                    store.storePreKey(preKey.id, preKey)
                                } // Store PreKeys

                                store.storeSignedPreKey(
                                    signedPreKey.id,
                                    signedPreKey
                                )

                                requireActivity().finish()
                            }
                        } else if (result.code() == 403) {
                            withContext(Dispatchers.Main) {
                                Snackbar.make(
                                    textInputLayout,
                                    "Oops, someone was faster than you and choped that name",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Snackbar.make(
                                    textInputLayout,
                                    "Client out of date",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } catch (e: ConnectException) {
                    Snackbar.make(textInputLayout, "Network Error", Snackbar.LENGTH_SHORT).show()
                }
            }

            textInputEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                    // Reset states of indeterminate things
                    currentUsernameCheckCoroutine?.cancel()
                    continueButton.isEnabled = false
                    textInputLayout.error = ""

                    textInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
                    textInputLayout.setEndIconTintList(states)
                    textInputLayout.endIconDrawable = context.getProgressBarDrawable()
                    textInputLayout.endIconDrawable!!.setTintList(states)
                    (textInputLayout.endIconDrawable as? Animatable)?.start()

                    currentUsernameCheckCoroutine = CoroutineScope(Dispatchers.IO).launch {
                        val call: Call<ResponseBody> =
                            retrofit.create(GetUsernameExists::class.java).usernameExists(
                                UserExistsRequest(textInputLayout.editText?.text.toString())
                            )
                        try {
                            val result = call.execute()
                            if (result.code() == 200) {
                                val resultString = result.body()?.string()
                                if (resultString != null) {
                                    if (resultString == "false") {
                                        withContext(Dispatchers.Main) {
                                            textInputLayout.endIconMode =
                                                TextInputLayout.END_ICON_CUSTOM
                                            textInputLayout.endIconDrawable =
                                                AppCompatResources.getDrawable(
                                                    context,
                                                    R.drawable.baseline_check_circle_24
                                                )
                                            continueButton.isEnabled = true
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            textInputLayout.endIconMode =
                                                TextInputLayout.END_ICON_NONE
                                            textInputLayout.error = "Username Taken"
                                        }
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    textInputLayout.endIconMode = TextInputLayout.END_ICON_NONE
                                    textInputLayout.error = "Client out of date"
                                }
                            }
                        } catch (e: ConnectException) {
                            withContext(Dispatchers.Main) {
                                textInputLayout.endIconMode = TextInputLayout.END_ICON_NONE
                                textInputLayout.error = "Network Error"
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
