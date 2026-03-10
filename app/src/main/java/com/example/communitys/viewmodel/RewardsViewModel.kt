package com.example.communitys.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RewardsViewModel : ViewModel() {

    private val _totalPoints = MutableLiveData<Int>()
    val totalPoints: LiveData<Int> = _totalPoints

    private val _rewards = MutableLiveData<List<Reward>>()
    val rewards: LiveData<List<Reward>> = _rewards

    private val _claimState = MutableLiveData<ClaimState>()
    val claimState: LiveData<ClaimState> = _claimState

    init {
        loadRewards()
        loadUserPoints()
    }

    private fun loadUserPoints() {
        // Load from repository
        _totalPoints.value = 1234
    }

    private fun loadRewards() {
        val rewardsList = listOf(
            Reward(
                id = "1",
                name = "Prepaid Load",
                description = "Mobile load credit",
                pointsCost = 100,
                icon = "📱"
            ),
            Reward(
                id = "2",
                name = "School Supplies",
                description = "Basic school supplies pack",
                pointsCost = 300,
                icon = "🎒"
            ),
            Reward(
                id = "3",
                name = "₱500 Groceries",
                description = "₱500 worth of groceries",
                pointsCost = 500,
                icon = "🛒"
            ),
            Reward(
                id = "4",
                name = "₱3,000 Cash",
                description = "Cash reward",
                pointsCost = 1500,
                icon = "💰"
            )
        )
        _rewards.value = rewardsList
    }

    fun claimReward(reward: Reward) {
        val currentPoints = _totalPoints.value ?: 0

        if (currentPoints < reward.pointsCost) {
            _claimState.value = ClaimState.Error("Not enough points. You need ${reward.pointsCost - currentPoints} more points.")
            return
        }

        _claimState.value = ClaimState.Loading

        // Simulate API call
        // In real app, call repository to claim reward
        _totalPoints.value = currentPoints - reward.pointsCost
        _claimState.value = ClaimState.Success("${reward.name} claimed successfully!")
    }

    data class Reward(
        val id: String,
        val name: String,
        val description: String,
        val pointsCost: Int,
        val icon: String
    )

    sealed class ClaimState {
        object Loading : ClaimState()
        data class Success(val message: String) : ClaimState()
        data class Error(val message: String) : ClaimState()
    }
}
