package com.example.communitys.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.communitys.view.dashboard.DashboardFragment
import com.example.communitys.view.documents.DocumentsFragment
import com.example.communitys.view.location.LocationFragment
import com.example.communitys.view.profile.ProfileFragment
import com.example.communitys.view.rewards.RewardsFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DashboardFragment()      // Home
            1 -> LocationFragment()       // Location
            2 -> DocumentsFragment()      // Documents
            3 -> RewardsFragment()        // Rewards
            4 -> ProfileFragment()        // Profile
            else -> DashboardFragment()
        }
    }
}