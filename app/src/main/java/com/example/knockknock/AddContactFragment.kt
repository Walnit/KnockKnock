package com.example.knockknock

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDrawableOrThrow
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.transition.TransitionInflater
import com.example.knockknock.networking.GetKnockRequestStatus
import com.example.knockknock.networking.GetUsernameExists
import com.example.knockknock.networking.SendKnockRequest
import com.example.knockknock.networking.ServerProperties
import com.example.knockknock.networking.structures.AddContactRequest
import com.example.knockknock.networking.structures.GetAddContactResultRequest
import com.example.knockknock.networking.structures.UserExistsRequest
import com.example.knockknock.signal.KnockSignalProtocolStore
import com.example.knockknock.structures.KnockClient
import com.example.knockknock.structures.KnockClientSerializable
import com.example.knockknock.utils.CoroutineHelper
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.ecc.Curve
import retrofit2.Call
import retrofit2.Retrofit
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets

class AddContactFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_add_contact, container, false)
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_in)
        returnTransition = inflater.inflateTransition(R.transition.fade)
        val navController = findNavController()

        with(view) {
            val textInputLayout = findViewById<TextInputLayout>(R.id.add_contact_layout)
            val textInputEditText = findViewById<TextInputEditText>(R.id.add_contact_edittext)
            val knockButton = findViewById<Button>(R.id.add_contact_knock_btn)
            val retrofit = ServerProperties.getRetrofitInstance()

            val masterKey = MasterKey.Builder(requireContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

            var securePreferences = EncryptedSharedPreferences.create(
                requireContext(),
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val states =
                ColorStateList(arrayOf(intArrayOf()), intArrayOf(context.fetchPrimaryColor()))

            var currentUsernameCheckCoroutine: Job? = null

            knockButton.setOnClickListener {


                val target: String = textInputLayout.editText?.text.toString()
                val call: Call<ResponseBody> =
                    retrofit.create(SendKnockRequest::class.java).addContact(
                        AddContactRequest(
                            securePreferences.getString("name", null)!!,
                            target,
                            Base64.encodeToString(
                                Json.encodeToString(
                                    KnockSignalProtocolStore(requireContext())
                                        .getLocalKnockClient().toSerializableClient()
                                ).toByteArray(), Base64.NO_WRAP
                            ),
                            Base64.encodeToString(
                                Curve.calculateSignature(
                                    KnockSignalProtocolStore(requireContext()).identityKeyPair.privateKey,
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
                            updateCardView(
                                securePreferences.getString("name", null)!!,
                                textInputLayout,
                                knockButton,
                                retrofit,
                                view,
                                context
                            )
                        } else if (result.code() == 202) {

                            var secureContacts = EncryptedSharedPreferences.create(
                                requireContext(),
                                "secure_contacts",
                                masterKey,
                                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                            )

                            val rawJson =
                                String(Base64.decode(result.body()?.string(), Base64.NO_WRAP))
                            val targetClient = KnockClient.fromSerialized(
                                Json.decodeFromString<KnockClientSerializable>(rawJson)
                            )

                            val store = KnockSignalProtocolStore(requireContext())

                            val sessionBuilder = SessionBuilder(
                                store,
                                SignalProtocolAddress(targetClient.name, 1)
                            )

                            sessionBuilder.process(targetClient.getPreKeyBundle())

                            secureContacts.edit().putString(targetClient.name, "New Contact!").commit()

                            withContext(Dispatchers.Main) {
                                val bundle = bundleOf("name" to targetClient.name)
                                navController.navigate(
                                    R.id.action_addContactFragment_to_messagesFragment,
                                    bundle
                                )
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
                } catch (e: UnknownHostException) {
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
                    textInputLayout.error = ""


                    textInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
                    textInputLayout.setEndIconTintList(states)
                    textInputLayout.endIconDrawable = context.getProgressBarDrawable()
                    textInputLayout.endIconDrawable!!.setTintList(states)
                    (textInputLayout.endIconDrawable as? Animatable)?.start()

                    // Check if username exists
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
                                        updateCardView(
                                            securePreferences.getString("name", null)!!,
                                            textInputLayout,
                                            knockButton,
                                            retrofit,
                                            view,
                                            context
                                        )
                                    } else {
                                        CoroutineHelper.textInputError(
                                            "No such handle!",
                                            textInputLayout
                                        )
                                    }
                                }
                            } else {
                                CoroutineHelper.textInputError(
                                    "Client outdated, please update this application",
                                    textInputLayout
                                )
                            }
                        } catch (e: ConnectException) {
                            CoroutineHelper.textInputError(
                                "Couldn't connect to server",
                                textInputLayout
                            )
                        } catch (e: IOException) {
                            CoroutineHelper.textInputError("Network Error", textInputLayout)
                        } catch (e: UnknownHostException) {
                            Snackbar.make(textInputLayout, "Network Error", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }

        return view
    }

    private suspend fun updateCardView(
        username: String,
        textInputLayout: TextInputLayout,
        knockButton: Button,
        retrofit: Retrofit,
        view: View,
        context: Context
    ) {

        if (username == textInputLayout.editText?.text.toString()) {
            withContext(Dispatchers.Main) {
                view.findViewById<TextView>(R.id.add_contact_hint).visibility = GONE
                textInputLayout.endIconMode =
                    TextInputLayout.END_ICON_CUSTOM
                textInputLayout.endIconDrawable =
                    AppCompatResources.getDrawable(
                        context,
                        R.drawable.baseline_check_circle_24
                    )
                view.findViewById<TextView>(R.id.add_contact_hint).visibility =
                    GONE
                view.findViewById<ProgressBar>(R.id.progressBar).visibility =
                    GONE
                view.findViewById<ConstraintLayout>(R.id.add_contact_result_layout).visibility =
                    VISIBLE
                view.findViewById<TextView>(R.id.add_contact_username).text =
                    textInputLayout.editText?.text.toString() + " (You)"

                val status = view.findViewById<TextView>(R.id.add_contact_status)
                status.text = "It's you!"
                status.setTextColor(Color.GRAY)
                knockButton.isEnabled = false

            }
        } else {

            // Show the CardView so they wait for more details
            withContext(Dispatchers.Main) {
                view.findViewById<TextView>(R.id.add_contact_hint).visibility = GONE
                textInputLayout.endIconMode =
                    TextInputLayout.END_ICON_CUSTOM
                textInputLayout.endIconDrawable =
                    AppCompatResources.getDrawable(
                        context,
                        R.drawable.baseline_check_circle_24
                    )
                view.findViewById<TextView>(R.id.add_contact_hint).visibility =
                    GONE
                view.findViewById<ProgressBar>(R.id.progressBar).visibility =
                    VISIBLE
                view.findViewById<TextView>(R.id.add_contact_username).text =
                    textInputLayout.editText?.text.toString()
            }

            // Get the more details
            val call: Call<ResponseBody> =
                retrofit.create(GetKnockRequestStatus::class.java).getRequestStatus(
                    GetAddContactResultRequest(
                        username,
                        textInputLayout.editText?.text.toString(),
                        Base64.encodeToString(
                            Curve.calculateSignature(
                                KnockSignalProtocolStore(requireContext()).identityKeyPair.privateKey,
                                textInputLayout.editText?.text.toString()
                                    .toByteArray(StandardCharsets.UTF_8)
                            ), Base64.NO_WRAP
                        )
                    )
                )

            when (call.execute().code()) {
                204 -> {
                    // Not requested
                    withContext(Dispatchers.Main) {
                        view.findViewById<ProgressBar>(R.id.progressBar).visibility =
                            GONE
                        view.findViewById<ConstraintLayout>(R.id.add_contact_result_layout).visibility =
                            VISIBLE
                        val status = view.findViewById<TextView>(R.id.add_contact_status)
                        status.text = "Locked"
                        status.setTextColor(Color.RED)
                        knockButton.isEnabled = true
                    }
                }
                201 -> {
                    // Requested
                    withContext(Dispatchers.Main) {
                        view.findViewById<ProgressBar>(R.id.progressBar).visibility =
                            GONE
                        view.findViewById<ConstraintLayout>(R.id.add_contact_result_layout).visibility =
                            VISIBLE
                        val status =
                            view.findViewById<TextView>(R.id.add_contact_status)
                        status.text = "Waiting..."
                        status.setTextColor(Color.YELLOW)
                        knockButton.isEnabled = false
                    }
                }
                202 -> {
                    // Accepted
                    withContext(Dispatchers.Main) {
                        view.findViewById<ProgressBar>(R.id.progressBar).visibility =
                            GONE
                        view.findViewById<ConstraintLayout>(R.id.add_contact_result_layout).visibility =
                            VISIBLE
                        val status =
                            view.findViewById<TextView>(R.id.add_contact_status)
                        status.text = "Knocking!"
                        status.setTextColor(Color.GREEN)
                        knockButton.isEnabled = true
                    }
                }
                403 -> {
                    // Auth error
                    withContext(Dispatchers.Main) {
                        Snackbar.make(
                            textInputLayout,
                            "Authentication Error",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
                else -> {
                    // Skill issue
                    withContext(Dispatchers.Main) {
                        Snackbar.make(
                            textInputLayout,
                            "Client out of date",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }

            }
        }
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