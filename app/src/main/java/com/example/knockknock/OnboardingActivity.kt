package com.example.knockknock

import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback {
            finishAffinity()
        }
        setContentView(R.layout.activity_onboarding)
    }

}
