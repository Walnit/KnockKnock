package com.example.knockknock

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.knockknock.onboarding.*
import me.relex.circleindicator.CircleIndicator3

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback {
            finishAffinity()
        }
        setContentView(R.layout.activity_onboarding)
    }

}
