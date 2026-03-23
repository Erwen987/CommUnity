package com.example.communitys.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communitys.CommUnityApplication
import com.example.communitys.model.repository.AuthRepository
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = _userProfile

    private val _logoutState = MutableLiveData<LogoutState>()
    val logoutState: LiveData<LogoutState> = _logoutState

    private val _changePasswordState = MutableLiveData<ActionState>()
    val changePasswordState: LiveData<ActionState> = _changePasswordState

    private val _deleteAccountState = MutableLiveData<ActionState>()
    val deleteAccountState: LiveData<ActionState> = _deleteAccountState

    private val _avatarUpdateState = MutableLiveData<ActionState>()
    val avatarUpdateState: LiveData<ActionState> = _avatarUpdateState

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            val result = authRepository.getCurrentUser()
            result.onSuccess { user ->
                _userProfile.value = UserProfile(
                    name      = "${user.firstName} ${user.lastName}".trim(),
                    firstName = user.firstName,
                    lastName  = user.lastName,
                    email     = user.email,
                    phone     = user.phone ?: "",
                    barangay  = formatBarangay(user.barangay),
                    points    = user.points ?: 0,
                    avatarUrl = user.avatarUrl
                )
            }
        }
    }

    fun updateProfile(name: String, email: String, barangay: String) {
        _userProfile.value = _userProfile.value?.copy(
            name     = name,
            email    = email,
            barangay = barangay
        )
    }

    // ── Change Password ───────────────────────────────────────────────────────

    fun changePassword(currentPassword: String, newPassword: String) {
        _changePasswordState.value = ActionState.Loading
        viewModelScope.launch {
            val result = authRepository.changePassword(currentPassword, newPassword)
            result.onSuccess {
                _changePasswordState.value = ActionState.Success
            }.onFailure { e ->
                _changePasswordState.value = ActionState.Error(e.message ?: "Failed to change password")
            }
        }
    }

    // ── Delete Account ────────────────────────────────────────────────────────

    fun deleteAccount(currentPassword: String, reason: String) {
        _deleteAccountState.value = ActionState.Loading
        viewModelScope.launch {
            val result = authRepository.deleteAccount(currentPassword, reason)
            result.onSuccess {
                _deleteAccountState.value = ActionState.Success
            }.onFailure { e ->
                _deleteAccountState.value = ActionState.Error(e.message ?: "Failed to delete account")
            }
        }
    }

    // ── Update Avatar ─────────────────────────────────────────────────────────

    fun updateAvatar(avatarUrl: String) {
        viewModelScope.launch {
            val result = authRepository.updateAvatar(avatarUrl)
            result.onSuccess {
                _userProfile.value = _userProfile.value?.copy(avatarUrl = avatarUrl)
                _avatarUpdateState.value = ActionState.Success
            }.onFailure { e ->
                _avatarUpdateState.value = ActionState.Error(e.message ?: "Failed to update avatar")
            }
        }
    }

    fun uploadAndSaveAvatar(imageBytes: ByteArray) {
        _avatarUpdateState.value = ActionState.Loading
        viewModelScope.launch {
            val userId = CommUnityApplication.supabase.auth.currentUserOrNull()?.id ?: return@launch
            val uploadResult = authRepository.uploadAvatarPhoto(userId, imageBytes)
            uploadResult.onSuccess { url ->
                val saveResult = authRepository.updateAvatar(url)
                saveResult.onSuccess {
                    _userProfile.value = _userProfile.value?.copy(avatarUrl = url)
                    _avatarUpdateState.value = ActionState.Success
                }.onFailure { e ->
                    _avatarUpdateState.value = ActionState.Error(e.message ?: "Failed to save avatar")
                }
            }.onFailure { e ->
                _avatarUpdateState.value = ActionState.Error(e.message ?: "Failed to upload photo")
            }
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    fun logout() {
        _logoutState.value = LogoutState.Loading
        viewModelScope.launch {
            val result = authRepository.logout()
            result.onSuccess {
                _logoutState.value = LogoutState.Success
            }.onFailure { e ->
                _logoutState.value = LogoutState.Error(e.message ?: "Logout failed")
            }
        }
    }

    private fun formatBarangay(barangay: String): String {
        if (barangay.isBlank()) return "Dagupan City, Pangasinan"
        val trimmed = barangay.trim()
        val prefix = if (trimmed.startsWith("Barangay", ignoreCase = true)) "" else "Barangay "
        return "${prefix}${trimmed}, Dagupan City, Pangasinan"
    }

    // ── Data classes ──────────────────────────────────────────────────────────

    data class UserProfile(
        val name      : String  = "",
        val firstName : String  = "",
        val lastName  : String  = "",
        val email     : String  = "",
        val phone     : String  = "",
        val barangay  : String  = "",
        val points    : Int     = 0,
        val avatarUrl : String? = null
    )

    sealed class LogoutState {
        object Loading : LogoutState()
        object Success : LogoutState()
        data class Error(val message: String) : LogoutState()
    }

    sealed class ActionState {
        object Loading : ActionState()
        object Success : ActionState()
        data class Error(val message: String) : ActionState()
    }

}