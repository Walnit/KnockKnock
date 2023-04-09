package com.example.knockknock

import android.animation.Animator
import android.animation.AnimatorInflater
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import com.example.knockknock.database.KnockMessage
import com.example.knockknock.database.MessageDatabase
import com.example.knockknock.networking.SendMessage
import com.example.knockknock.networking.ServerProperties
import com.example.knockknock.networking.structures.SendMessageRequest
import com.example.knockknock.signal.KnockSignalProtocolStore
import com.example.knockknock.utils.PrefsHelper
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SignalMessage
import java.io.File
import java.net.ConnectException
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*


class MessagesFragment : Fragment() {

    var msgSyncJob: Job? = null
    var startupJob: Job? = null
    lateinit var messageTarget: String
    lateinit var imageUri: Uri
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_messages, container, false)
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_in)
        returnTransition = inflater.inflateTransition(R.transition.fade)

        val navController = findNavController()

        val target = requireArguments().getString("name")

        if (target != null) {

            messageTarget = target
            val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) {success ->
                if (!success) Snackbar.make(view, "Capture Image Failed!", Snackbar.LENGTH_SHORT).show()
                else {
                    Snackbar.make(view, "Sending Image...", Snackbar.LENGTH_SHORT).show()
                    sendImageUsingUri(imageUri, target, view)
                }
            }

            val galleryPhotoLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri == null) Snackbar.make(view, "Attach Image Failed!", Snackbar.LENGTH_SHORT).show()
                else {
                    Snackbar.make(view, "Sending Image...", Snackbar.LENGTH_SHORT).show()
                    sendImageUsingUri(uri, target, view)
                }
            }

            startupJob = CoroutineScope(IO).launch {
                val recyclerView = view.findViewById<RecyclerView>(R.id.messages_recyclerview)
                val progressBar = view.findViewById<ProgressBar>(R.id.messages_loading_progressbar)
                val retrofit = ServerProperties.getRetrofitInstance()
                val prefsHelper = PrefsHelper(requireContext())
                var securePreferences = prefsHelper.openEncryptedPrefs("secure_prefs")
                val secureContacts = prefsHelper.openEncryptedPrefs("secure_contacts")
                val hiddenContacts = prefsHelper.openEncryptedPrefs("hidden_contacts")

                if (!secureContacts.contains(target) && !hiddenContacts.all.values.contains(target)) {
                    withContext(Main) {
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setTitle("Error!")
                        builder.setMessage("You have not requested to contact this user!")
                        builder.setIcon(android.R.drawable.ic_dialog_alert)
                        builder.setPositiveButton("Ok") { _, _ -> }
                        builder.create().show()

                        navController.navigate(R.id.action_messagesFragment_to_ChatsList)
                    }

                } else {

                    if (context != null) {
                        // Set pref so that no notifs and sounds for this guy
                        securePreferences.edit().putString("current", target).apply()

                        val store = KnockSignalProtocolStore(requireContext())

                        val sessionCipher = SessionCipher(
                            store,
                            SignalProtocolAddress(target, 1)
                        )

                        val layoutManager = LinearLayoutManager(requireContext())
                        layoutManager.stackFromEnd = true

                        val adapter =
                            MessagesRecyclerAdapter(target, navController, requireContext())

                        // Set onclicklisteners
                        withContext(Main) {
                            recyclerView.layoutManager = layoutManager
                            recyclerView.adapter = adapter

                            // Send message button
                            view.findViewById<ImageButton>(R.id.messages_send_imgbtn)
                                .setOnClickListener {
                                    val editText =
                                        view.findViewById<TextInputEditText>(R.id.messages_edittext)
                                    if (!editText.text.isNullOrBlank()) {
                                        val editTextContent: String =
                                            editText.text.toString().trim()

                                        CoroutineScope(Default).launch {
                                            // Encrypt the message
                                            val cipherMessage = sessionCipher.encrypt(
                                                editTextContent.toByteArray(StandardCharsets.UTF_8)
                                            )

                                            val messageType: String =
                                                if (cipherMessage is PreKeySignalMessage) {
                                                    "P"
                                                } else if (cipherMessage is SignalMessage) {
                                                    "M"
                                                } else {
                                                    "O"
                                                }

                                            // Serialize message
                                            val serMessage = (messageType + Base64.encodeToString(
                                                cipherMessage.serialize(),
                                                Base64.NO_WRAP
                                            )).toByteArray(StandardCharsets.UTF_8)

                                            // Send message to server
                                            val call = retrofit.create(SendMessage::class.java)
                                                .sendMessage(
                                                    SendMessageRequest(
                                                        securePreferences.getString("name", null)!!,
                                                        target,
                                                        Base64.encodeToString(
                                                            serMessage,
                                                            Base64.NO_WRAP
                                                        ),
                                                        Base64.encodeToString(
                                                            Curve.calculateSignature(
                                                                store.identityKeyPair.privateKey,
                                                                serMessage
                                                            ), Base64.NO_WRAP
                                                        )
                                                    )
                                                )

                                            try {
                                                val result = call.execute()
                                                if (result.code() == 200) {
                                                    MessageDatabase.writeMessages(
                                                        target, arrayOf(
                                                            KnockMessage(
                                                                store.getLocalKnockClient().name,
                                                                System.currentTimeMillis(),
                                                                editTextContent.toByteArray(
                                                                    StandardCharsets.UTF_8
                                                                ),
                                                                KnockMessage.KnockMessageType.TEXT
                                                            )
                                                        ), requireContext()
                                                    )
                                                    secureContacts.edit()
                                                        .putString(target, "You: $editTextContent")
                                                        .apply()

                                                } else if (result.code() == 403) {
                                                    withContext(Main) {
                                                        Snackbar.make(
                                                            recyclerView,
                                                            "Authentication Error",
                                                            Snackbar.LENGTH_LONG
                                                        ).show()
                                                    }
                                                } else {
                                                    withContext(Main) {
                                                        Snackbar.make(
                                                            recyclerView,
                                                            "Client out of date",
                                                            Snackbar.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            } catch (e: ConnectException) {
                                                Snackbar.make(
                                                    recyclerView,
                                                    "Network Error",
                                                    Snackbar.LENGTH_SHORT
                                                )
                                                    .show()
                                            } catch (e: UnknownHostException) {
                                                Snackbar.make(
                                                    recyclerView,
                                                    "Network Error",
                                                    Snackbar.LENGTH_SHORT
                                                )
                                                    .show()
                                            }

                                        }
                                        editText.text!!.clear()
                                    }
                                }

                            // Code for attaching images and stuff

                            val attachBtn = view.findViewById<Button>(R.id.messages_attach_btn)
                            val attachLayout =
                                view.findViewById<LinearLayout>(R.id.messages_attach_layout)
                            val takePhotoBtn = view.findViewById<Button>(R.id.messages_photo_btn)
                            val attachImageBtn =
                                view.findViewById<Button>(R.id.messages_attach_image_btn)

                            attachBtn.setOnClickListener {
                                if (attachLayout.visibility == GONE) {
                                    attachLayout.visibility = VISIBLE
                                    AnimatorInflater.loadAnimator(
                                        requireContext(),
                                        R.animator.messages_attach_in
                                    )
                                        .apply {
                                            setTarget(attachLayout)
                                            interpolator = OvershootInterpolator()
                                            start()
                                        }
                                } else {
                                    AnimatorInflater.loadAnimator(
                                        requireContext(),
                                        R.animator.messages_attach_out
                                    )
                                        .apply {
                                            setTarget(attachLayout)
                                            interpolator = AnticipateInterpolator()
                                            addListener(object : AnimationListener,
                                                Animator.AnimatorListener {
                                                override fun onAnimationStart(animation: Animation?) {
                                                    // Do nothing
                                                }

                                                override fun onAnimationEnd(arg0: Animation) {
                                                    attachLayout.visibility = GONE
                                                }

                                                override fun onAnimationRepeat(animation: Animation?) {
                                                    // Do nothing
                                                }

                                                override fun onAnimationStart(animation: Animator) {
                                                    // Do nothing
                                                }

                                                override fun onAnimationEnd(animation: Animator) {
                                                    attachLayout.visibility = GONE
                                                }

                                                override fun onAnimationCancel(animation: Animator) {
                                                    // Do nothing
                                                }

                                                override fun onAnimationRepeat(animation: Animator) {
                                                    // Do nothing
                                                }
                                            })
                                            start()
                                        }
                                }
                            }
                            takePhotoBtn.setOnClickListener {

                                val tmpImgFile = File.createTempFile(
                                    "IMG_" + SimpleDateFormat(
                                        "yyyyMMdd_HHmmss",
                                        requireContext().resources.configuration.locales.get(0)
                                    )
                                        .format(
                                            Date(System.currentTimeMillis())
                                        ),
                                    ".jpg",
                                    requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                                ).apply {
                                    createNewFile()
                                    deleteOnExit()
                                }
                                imageUri = FileProvider.getUriForFile(
                                    requireContext(),
                                    requireContext().packageName + ".fileprovider",
                                    tmpImgFile
                                )
                                takePhotoLauncher.launch(imageUri)
                                attachLayout.visibility = GONE
                            }
                            attachImageBtn.setOnClickListener {
                                galleryPhotoLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                                attachLayout.visibility = GONE
                            }

                        }

                        // Refresh views to get new messages

                        var lastMessage: Int = recyclerView.adapter!!.itemCount

                        msgSyncJob = CoroutineScope(Default).launch {
                            while (true) {
                                if (context != null) {
                                    val newSize =
                                        MessageDatabase.getMessages(target, requireContext())?.size
                                            ?: 0
                                    if (lastMessage != newSize) {
                                        val newAdapter = MessagesRecyclerAdapter(
                                            target,
                                            navController,
                                            requireContext()
                                        )
                                        withContext(Main) {
                                            recyclerView.adapter = newAdapter
                                            lastMessage = newSize
                                            recyclerView.smoothScrollToPosition(lastMessage - 1)
                                        }
                                    }
                                    delay(500)
                                }
                            }
                        }

                        withContext(Main) { progressBar.visibility = GONE }
                    }
                }
            }
        }

        return view
    }

    override fun onDestroy() {
        PrefsHelper(requireContext()).openEncryptedPrefs("secure_prefs").edit().remove("current").apply()
        startupJob?.cancel()
        msgSyncJob?.cancel()
        super.onDestroy()
    }

    private fun sendImageUsingUri(imageUri: Uri, target: String, view: View) {
        // Send the one single image message

        val retrofit = ServerProperties.getRetrofitInstance()

        val prefsHelper = PrefsHelper(requireContext())
        var securePreferences = prefsHelper.openEncryptedPrefs("secure_prefs")
        val store = KnockSignalProtocolStore(requireContext())

        val sessionCipher = SessionCipher(
            store,
            SignalProtocolAddress(target, 1)
        )

        CoroutineScope(IO).launch {
            val imageBytes = requireContext().contentResolver.openInputStream(imageUri)!!.readBytes()
            val cipherMessage = sessionCipher.encrypt(imageBytes)

            if (cipherMessage is PreKeySignalMessage) {
                // Its very scuffed to do this so imma just tell them to send a text message first
                Snackbar.make(view, "Please send some text messages first!", Snackbar.LENGTH_SHORT).show()
            } else if (cipherMessage is SignalMessage) {
                val serMessage = ("I" + Base64.encodeToString(
                    cipherMessage.serialize(),
                    Base64.NO_WRAP
                )).toByteArray(StandardCharsets.UTF_8)

                val call = retrofit.create(SendMessage::class.java).sendMessage(
                    SendMessageRequest(
                        securePreferences.getString("name", null)!!,
                        target,
                        Base64.encodeToString(serMessage, Base64.NO_WRAP),
                        Base64.encodeToString(
                            Curve.calculateSignature(
                                store.identityKeyPair.privateKey,
                                serMessage
                            ), Base64.NO_WRAP
                        )
                    )
                )

                try {
                    val result = call.execute()
                    if (result.code() == 200) {
                        MessageDatabase.writeMessages(
                            target, arrayOf(
                                KnockMessage(
                                    store.getLocalKnockClient().name,
                                    System.currentTimeMillis(),
                                    imageBytes,
                                    KnockMessage.KnockMessageType.IMAGE
                                )
                            ), requireContext()
                        )

                    } else if (result.code() == 403) {
                        withContext(Main) {
                            Snackbar.make(
                                view,
                                "Authentication Error",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        withContext(Main) {
                            Snackbar.make(
                                view,
                                "Client out of date",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: ConnectException) {
                    Snackbar.make(
                        view,
                        "Network Error",
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
                } catch (e: UnknownHostException) {
                    Snackbar.make(
                        view,
                        "Network Error",
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
    }

}