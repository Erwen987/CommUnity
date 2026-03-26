package com.example.communitys.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communitys.model.data.NotificationModel
import com.example.communitys.model.repository.NotificationRepository
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {

    private val notificationRepository = NotificationRepository()

    private val _notifications = MutableLiveData<List<NotificationModel>>()
    val notifications: LiveData<List<NotificationModel>> = _notifications

    private val _unreadCount = MutableLiveData<Int>()
    val unreadCount: LiveData<Int> = _unreadCount

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun loadNotifications(userId: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = notificationRepository.getNotifications(userId)
            if (result.isSuccess) {
                _notifications.value = result.getOrNull() ?: emptyList()
            }
            _loading.value = false
        }
    }

    fun loadUnreadCount(userId: String) {
        viewModelScope.launch {
            val result = notificationRepository.getUnreadCount(userId)
            if (result.isSuccess) {
                _unreadCount.value = result.getOrNull() ?: 0
            }
        }
    }

    fun markAsRead(notificationId: String, userId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
            loadNotifications(userId)
            loadUnreadCount(userId)
        }
    }

    fun markAllAsRead(userId: String) {
        viewModelScope.launch {
            notificationRepository.markAllAsRead(userId)
            loadNotifications(userId)
            loadUnreadCount(userId)
        }
    }

    fun deleteNotification(notificationId: String, userId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
            loadNotifications(userId)
            loadUnreadCount(userId)
        }
    }
}
