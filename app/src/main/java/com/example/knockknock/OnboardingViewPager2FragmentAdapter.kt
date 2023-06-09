package com.example.knockknock

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
class OnboardingViewPager2FragmentAdapter (FA: FragmentActivity,
                                 private val fragments:ArrayList<Fragment>): FragmentStateAdapter(FA) {
    override fun getItemCount(): Int = fragments.size
    override fun createFragment(position: Int): Fragment = fragments[position]
}