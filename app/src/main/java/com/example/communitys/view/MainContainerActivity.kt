package com.example.communitys.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.communitys.R
import com.example.communitys.databinding.ActivityMainContainerBinding
import com.example.communitys.service.MaintenanceModeService

class MainContainerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainContainerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start maintenance mode monitoring service
        startMaintenanceModeService()

        setupViewPager()
        setupBottomNavigation()
    }

    private fun startMaintenanceModeService() {
        val serviceIntent = Intent(this, MaintenanceModeService::class.java)
        startService(serviceIntent)
    }

    private fun setupViewPager() {
        val adapter = MainPagerAdapter(this)
        binding.viewPager.adapter = adapter

        // Disable swipe gesture between tabs
        binding.viewPager.isUserInputEnabled = false

        // Sync bottom nav when page changes
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> binding.bottomNavigation.selectedItemId = R.id.nav_home
                    1 -> binding.bottomNavigation.selectedItemId = R.id.nav_location
                    2 -> binding.bottomNavigation.selectedItemId = R.id.nav_documents
                    3 -> binding.bottomNavigation.selectedItemId = R.id.nav_rewards
                    4 -> binding.bottomNavigation.selectedItemId = R.id.nav_profile
                }
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_home

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.viewPager.setCurrentItem(0, false) // false = no animation
                    true
                }
                R.id.nav_location -> {
                    binding.viewPager.setCurrentItem(1, false)
                    true
                }
                R.id.nav_documents -> {
                    binding.viewPager.setCurrentItem(2, false)
                    true
                }
                R.id.nav_rewards -> {
                    binding.viewPager.setCurrentItem(3, false)
                    true
                }
                R.id.nav_profile -> {
                    binding.viewPager.setCurrentItem(4, false)
                    true
                }
                else -> false
            }
        }
    }

    fun navigateToTab(position: Int) {
        binding.viewPager.setCurrentItem(position, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the maintenance mode service
        val serviceIntent = Intent(this, MaintenanceModeService::class.java)
        stopService(serviceIntent)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.viewPager.currentItem == 0) {
            @Suppress("DEPRECATION")
            super.onBackPressed()
            moveTaskToBack(true)
        } else {
            binding.viewPager.setCurrentItem(0, false)
        }
    }
}