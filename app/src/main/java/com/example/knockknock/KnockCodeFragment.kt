package com.example.knockknock

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.text.isDigitsOnly
import androidx.core.widget.addTextChangedListener
import androidx.navigation.findNavController
import com.example.knockknock.knockcode.KnockCode
import com.example.knockknock.knockcode.KnockInput
import com.example.knockknock.utils.PrefsHelper
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.lang.Math.min

class KnockCodeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_knock_code, container, false)

        var sequence: KnockCode? = null
        var endJob: Job? = null

        with (view) {
            val knockerLayout = findViewById<ConstraintLayout>(R.id.knocker_layout)
            val pinLayout = findViewById<ConstraintLayout>(R.id.pin_layout)
            val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.hidden_toggle_group)
            toggleGroup.check(R.id.hidden_knock_toggle)

            toggleGroup.addOnButtonCheckedListener { toggleButton, checkedId, isChecked ->
                if (isChecked) {
                    if (checkedId == R.id.hidden_knock_toggle) {
                        knockerLayout.visibility = VISIBLE
                        pinLayout.visibility = GONE
                    } else if (checkedId == R.id.hidden_pin_toggle) {
                        knockerLayout.visibility = GONE
                        pinLayout.visibility = VISIBLE
                    }
                }
            }

            // Knock Code Magic
            val knockContacts = PrefsHelper(requireContext()).openEncryptedPrefs("hidden_contacts")
            val knocker1 = findViewById<Button>(R.id.hidden_knocker_1)
            val knocker2 = findViewById<Button>(R.id.hidden_knocker_2)
            val knocker3 = findViewById<Button>(R.id.hidden_knocker_3)
            val knocker4 = findViewById<Button>(R.id.hidden_knocker_4)
            val knockProgressBar = findViewById<ProgressBar>(R.id.progressBar2)
            val progBarAnimator = ObjectAnimator.ofInt(knockProgressBar, "progress", 0, 100)
            progBarAnimator.duration = 1000
            progBarAnimator.interpolator = LinearInterpolator()

            val knockerOnClickListener : OnClickListener = OnClickListener {

                // Stop input end timer
                progBarAnimator.cancel()
                knockProgressBar.progress = 0
                endJob?.cancel()

                if (sequence == null) {
                    sequence = KnockCode() // Reset sequence if new input
                }

                // Add button pressed + timing into sequence
                sequence!!.addInput((it as Button).text.toString().toShort(), System.currentTimeMillis())

                // Restart input end timer
                progBarAnimator.start()
                // Executes at end of knock code input
                endJob = CoroutineScope(Dispatchers.IO).launch {
                    delay(1000)
                    sequence!!.finishInput()

                    withContext(Dispatchers.Main) {
                        knocker1.isEnabled = false
                        knocker2.isEnabled = false
                        knocker3.isEnabled = false
                        knocker4.isEnabled = false
                    }


                    Log.i("TAG", "Timing-adjusted sequence: $sequence")

                    var isInContacts = false
                    knockContacts.all.keys.forEach { data ->
                        // data can be from pin or knocker, check if knocker
                        if (!data.isDigitsOnly()) {
                            val tempSequence = KnockCode()
                            tempSequence.fromJson(data)

                            if (tempSequence == sequence) {
                                isInContacts = true
                                withContext(Dispatchers.Main) {
                                    val bundle =
                                        bundleOf("name" to knockContacts.getString(data, null), "hidden" to true)
                                    view.findNavController().navigate(
                                        R.id.action_knockCodeFragment_to_messagesFragment,
                                        args = bundle
                                    )
                                }
                            }
                        }
                    }

                    if (!isInContacts) {
                        withContext(Dispatchers.Main) {
                            Snackbar.make(
                                knockProgressBar,
                                "No associated contact!",
                                Snackbar.LENGTH_LONG
                            ).show()
                            knocker1.isEnabled = true
                            knocker2.isEnabled = true
                            knocker3.isEnabled = true
                            knocker4.isEnabled = true
                        }
                    }

                }

            }

            knocker1.setOnClickListener(knockerOnClickListener)
            knocker2.setOnClickListener(knockerOnClickListener)
            knocker3.setOnClickListener(knockerOnClickListener)
            knocker4.setOnClickListener(knockerOnClickListener)

            // pin sadness
            val pinEditText = findViewById<EditText>(R.id.hidden_pin_edittext)
            val pinSubmitBtn = findViewById<Button>(R.id.hidden_pin_enter_btn)

            pinEditText.addTextChangedListener(object : TextWatcher {
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
                    pinSubmitBtn.isEnabled = !s.isNullOrBlank()
                }
            })

            pinSubmitBtn.setOnClickListener {
                if (!pinEditText.text.isNullOrBlank()) {

                    val data = pinEditText.text.toString()

                    if (knockContacts.contains(data)) {
                        val bundle = bundleOf("name" to knockContacts.getString(data, null), "hidden" to true)
                        view.findNavController().navigate(
                            R.id.action_knockCodeFragment_to_messagesFragment,
                            args = bundle
                        )
                    } else {
                        Snackbar.make(
                            knockProgressBar,
                            "No associated contact!",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }

        }

        return view
    }

}