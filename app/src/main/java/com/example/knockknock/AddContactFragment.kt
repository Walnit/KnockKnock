package com.example.knockknock

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDrawableOrThrow
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.knockknock.networking.GetUsernameExists
import com.example.knockknock.networking.SendKnockRequest
import com.example.knockknock.networking.ServerProperties
import com.example.knockknock.networking.structures.AddContactRequest
import com.example.knockknock.networking.structures.UserExistsRequest
import com.example.knockknock.signal.KnockIdentityKeyStore
import com.example.knockknock.structures.KnockClient
import com.example.knockknock.structures.KnockClientSerializable
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import org.whispersystems.libsignal.ecc.Curve
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.ConnectException
import java.nio.charset.StandardCharsets

class AddContactFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_add_contact, container, false)

        val navController = findNavController()

        with(view) {
            val textInputLayout = findViewById<TextInputLayout>(R.id.add_contact_layout)
            val textInputEditText = findViewById<TextInputEditText>(R.id.add_contact_edittext)
            val knockButton = findViewById<Button>(R.id.add_contact_knock_btn)
            val retrofit = ServerProperties.getRetrofitInstance()

            val states =
                ColorStateList(arrayOf(intArrayOf()), intArrayOf(context.fetchPrimaryColor()))

            var currentUsernameCheckCoroutine: Job? = null

            knockButton.setOnClickListener {

                val masterKey = MasterKey.Builder(requireContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

                var securePreferences = EncryptedSharedPreferences.create(
                    requireContext(),
                    "secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )

                val target: String = textInputLayout.editText?.text.toString()
                val call: Call<ResponseBody> =
                    retrofit.create(SendKnockRequest::class.java).addContact(
                        AddContactRequest(
                            securePreferences.getString("name", null)!!,
                            target,
                            Base64.encodeToString(
                                Json.encodeToString(
                                    KnockIdentityKeyStore(requireContext())
                                        .getLocalKnockClient().toSerializableClient()
                                ).toByteArray(), Base64.NO_WRAP
                            ),
                            Base64.encodeToString(
                                Curve.calculateSignature(
                                    KnockIdentityKeyStore(requireContext()).identityKeyPair.privateKey,
                                    target.toByteArray(StandardCharsets.UTF_8)
                                ), Base64.NO_WRAP
                            )
                        )
                    )
                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = call.execute()
                        if (result.code() == 201) {
                            withContext(Dispatchers.Main) {
                                Snackbar.make(
                                    textInputLayout,
                                    "Request Sent!",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        } else if (result.code() == 202) {

                            val masterKey = MasterKey.Builder(requireContext())
                                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

                            var secureContacts = EncryptedSharedPreferences.create(
                                requireContext(),
                                "secure_contacts",
                                masterKey,
                                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                            )

                            val rawJson = String(Base64.decode(result.body()?.string(), Base64.NO_WRAP))
                            val targetClient = KnockClient.fromSerialized(
                                Json.decodeFromString<KnockClientSerializable>(rawJson)
                            )

                            secureContacts.edit().putString(targetClient.name, rawJson).commit()

                            withContext(Dispatchers.Main) {
                                val bundle = bundleOf("name" to targetClient.name)
                                navController.navigate(R.id.action_addContactFragment_to_messagesFragment, bundle)
                            }

                        } else if (result.code() == 403) {
                            withContext(Dispatchers.Main) {
                                Snackbar.make(
                                    textInputLayout,
                                    "Authentication Error",
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
                    knockButton.isEnabled = false
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
                                    if (resultString == "true") {
                                        withContext(Dispatchers.Main) {
                                            textInputLayout.endIconMode =
                                                TextInputLayout.END_ICON_CUSTOM
                                            textInputLayout.endIconDrawable =
                                                AppCompatResources.getDrawable(
                                                    context,
                                                    R.drawable.baseline_check_circle_24
                                                )
                                            knockButton.isEnabled = true
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            textInputLayout.endIconMode =
                                                TextInputLayout.END_ICON_NONE
                                            textInputLayout.error = "Username doesn't exist"
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
                        } catch (e: IOException) {
                        withContext(Dispatchers.Main) {
                            e.printStackTrace()
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