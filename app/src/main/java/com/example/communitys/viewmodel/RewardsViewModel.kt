package com.example.communitys.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communitys.CommUnityApplication
import com.example.communitys.model.data.UserModel
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class RewardsViewModel : ViewModel() {

    private val supabase = CommUnityApplication.supabase

    private val _totalPoints = MutableLiveData<Int>()
    val totalPoints: LiveData<Int> = _totalPoints

    private val _claimState = MutableLiveData<ClaimState>()
    val claimState: LiveData<ClaimState> = _claimState

    init {
        loadUserPoints()
    }

    fun loadUserPoints() {
        viewModelScope.launch {
            try {
                val authId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                val users = supabase.from("users")
                    .select { filter { eq("auth_id", authId) } }
                    .decodeList<UserModel>()
                _totalPoints.value = users.firstOrNull()?.points ?: 0
            } catch (e: Exception) {
                android.util.Log.e("RewardsViewModel", "loadUserPoints failed: ${e.message}")
                _totalPoints.value = 0
            }
        }
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
