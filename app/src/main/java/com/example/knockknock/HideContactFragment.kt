package com.example.knockknock

import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.text.isDigitsOnly
import androidx.navigation.findNavController
import com.example.knockknock.knockcode.KnockCode
import com.example.knockknock.utils.PrefsHelper
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*

class HideContactFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_hide_contact, container, false)

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
                        knockerLayout.visibility = View.VISIBLE
                        pinLayout.visibility = View.GONE
                    } else if (checkedId == R.id.hidden_pin_toggle) {
                        knockerLayout.visibility = View.GONE
                        pinLayout.visibility = View.VISIBLE
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


            val knockerOnClickListener : View.OnClickListener = View.OnClickListener {

                // Stop input end timer
                progBarAnimator.cancel()
                knockProgressBar.progress = 0
                endJob?.cancel()

                if (sequence == null) {
                    sequence = KnockCode() // Reset sequence if new input
                }

                // Add button pressed + timing into sequence
                sequence!!.addInput(
                    (it as Button).text.toString().toShort(),
                    System.currentTimeMillis()
                )

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


                    var isInContacts = false
                    knockContacts.all.keys.forEach { data ->
                        // data can be from pin or knocker, check if knocker
                        if (!data.isDigitsOnly()) {
                            val tempSequence = KnockCode()
                            tempSequence.fromJson(data)

                            if (tempSequence == sequence) {
                                isInContacts = true
                                withContext(Dispatchers.Main) {
                                    Snackbar.make(
                                        knockProgressBar,
                                        "Similar/Same Knock Code already used!",
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

                    if (!isInContacts) {
                        PrefsHelper(requireContext()).openEncryptedPrefs("secure_contacts").edit().remove(requireArguments().getString("name")).apply()
                        knockContacts.edit().putString(sequence!!.toJson(), requireArguments().getString("name")).apply()
                        withContext(Dispatchers.Main) {
                            Snackbar.make(
                                view,
                                "Contact hidden!",
                                Snackbar.LENGTH_LONG
                            ).show()
                            view.findNavController().navigate(R.id.action_hideContactFragment_to_ChatsList)
                        }
                    }

                }

            }

            knocker1.setOnClickListener(knockerOnClickListener)
            knocker2.setOnClickListener(knockerOnClickListener)
            knocker3.setOnClickListener(knockerOnClickListener)
            knocker4.setOnClickListener(knockerOnClickListener)

            // what if im stupid? knock code edition
            val knockCodeFAQ = findViewById<Button>(R.id.hidden_what_btn)
            knockCodeFAQ.setOnClickListener {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("What's a Knock Code?")
                builder.setMessage(getString(R.string.knock_faq))
                builder.setIcon(R.drawable.baseline_info_24)
                builder.setPositiveButton("Ok") {_,_->}
                builder.create().show()
            }

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

                    if (!knockContacts.contains(data)) {
                        PrefsHelper(requireContext()).openEncryptedPrefs("secure_contacts").edit().remove(requireArguments().getString("name")).apply()
                        knockContacts.edit().putString(data, requireArguments().getString("name")).apply()
                        Snackbar.make(
                            view,
                            "Contact hidden!",
                            Snackbar.LENGTH_LONG
                        ).show()
                        view.findNavController().navigate(R.id.action_hideContactFragment_to_ChatsList)
                    } else {
                        Snackbar.make(
                            knockProgressBar,
                            "PIN already used!",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }


        }

        return view
    }

}