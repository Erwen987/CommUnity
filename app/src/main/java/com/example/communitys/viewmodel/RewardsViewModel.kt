package com.example.communitys.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communitys.CommUnityApplication
import com.example.communitys.model.data.RewardItemModel
import com.example.communitys.model.data.UserModel
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class RewardsViewModel : ViewModel() {

    private val supabase = CommUnityApplication.supabase

    private val _totalPoints = MutableLiveData<Int>()
    val totalPoints: LiveData<Int> = _totalPoints

    private val _rewardItems = MutableLiveData<List<RewardItemModel>>()
    val rewardItems: LiveData<List<RewardItemModel>> = _rewardItems

    private val _claimState = MutableLiveData<ClaimState>()
    val claimState: LiveData<ClaimState> = _claimState

    init {
        loadUserPoints()
        loadRewardItems()
        observeRealtimeChanges()
    }

    fun loadUserPoints() {
        viewModelScope.launch {
            try {
                val authId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                val user = supabase.from("users")
                    .select { filter { eq("auth_id", authId) } }
                    .decodeList<UserModel>()
                    .firstOrNull()
                _totalPoints.value = user?.rewardPoints ?: 0
            } catch (e: Exception) {
                Log.e("RewardsViewModel", "loadUserPoints failed: ${e.message}")
                _totalPoints.value = 0
            }
        }
    }

    fun loadRewardItems() {
        viewModelScope.launch {
            try {
                val authId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                val user = supabase.from("users")
                    .select { filter { eq("auth_id", authId) } }
                    .decodeList<UserModel>()
                    .firstOrNull()
                val userBarangay = user?.barangay ?: return@launch
                val items = supabase.from("reward_items")
                    .select { filter {
                        eq("is_active", true)
                        eq("barangay", userBarangay)
                    } }
                    .decodeList<RewardItemModel>()
                _rewardItems.value = items
            } catch (e: Exception) {
                Log.e("RewardsViewModel", "loadRewardItems failed: ${e.message}")
                _rewardItems.value = emptyList()
            }
        }
    }

    fun claimReward(itemId: String, itemName: String, pointsRequired: Int) {
        viewModelScope.launch {
            _claimState.value = ClaimState.Loading
            try {
                val authId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("Not authenticated")

                val user = supabase.from("users")
                    .select { filter { eq("auth_id", authId) } }
                    .decodeList<UserModel>()
                    .firstOrNull() ?: throw Exception("User not found")

                val currentPts = user.rewardPoints ?: 0
                if (currentPts < pointsRequired) {
                    _claimState.value = ClaimState.Error("Not enough points")
                    return@launch
                }

                // Insert pending redemption record
                supabase.from("redemptions").insert(
                    mapOf(
                        "user_id"        to authId,
                        "reward_item_id" to itemId,
                        "points_spent"   to pointsRequired,
                        "status"         to "pending"
                    )
                )

                // Deduct from reward_points
                val newPts = currentPts - pointsRequired
                supabase.from("users")
                    .update({ set("reward_points", newPts) }) {
                        filter { eq("auth_id", authId) }
                    }

                _totalPoints.value = newPts
                _claimState.value = ClaimState.Success(itemName)
            } catch (e: Exception) {
                Log.e("RewardsViewModel", "claimReward failed: ${e.message}")
                _claimState.value = ClaimState.Error(e.message ?: "Failed to request redemption")
            }
        }
    }

    private fun observeRealtimeChanges() {
        viewModelScope.launch {
            try {
                val rewardItemsChannel = supabase.channel("reward_items_changes")
                rewardItemsChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "reward_items"
                }.onEach {
                    loadRewardItems()
                }.launchIn(viewModelScope)
                rewardItemsChannel.subscribe()

                val usersChannel = supabase.channel("users_points_changes")
                usersChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "users"
                }.onEach {
                    loadUserPoints()
                }.launchIn(viewModelScope)
                usersChannel.subscribe()
            } catch (e: Exception) {
                Log.e("RewardsViewModel", "Realtime subscription failed: ${e.message}")
            }
        }
    }

    sealed class ClaimState {
        object Loading : ClaimState()
        data class Success(val itemName: String) : ClaimState()
        data class Error(val message: String) : ClaimState()
    }
}
