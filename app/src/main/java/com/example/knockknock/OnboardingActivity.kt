package com.example.knockknock

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.knockknock.onboarding.*
import me.relex.circleindicator.CircleIndicator3

class OnboardingActivity : AppCompatActivity() {
    private val fragmentList = ArrayList<Fragment>()
    private lateinit var viewPager: ViewPager2
    private lateinit var indicator: CircleIndicator3
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        castView()
        fragmentList.add(Page1())
        fragmentList.add(Page2())
        viewPager.adapter = OnboardingViewPager2FragmentAdapter(this, fragmentList)
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        indicator.setViewPager(viewPager)
    }
    private fun castView() {
        viewPager = findViewById(R.id.view_pager2)
        indicator = findViewById(R.id.indicator)
    }
}
