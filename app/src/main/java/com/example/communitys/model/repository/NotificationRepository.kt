package com.example.communitys.model.repository

import com.example.communitys.CommUnityApplication
import com.example.communitys.model.data.NotificationModel
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class NotificationRepository {

    private val supabase = CommUnityApplication.supabase

    // Get all notifications for a user
    suspend fun getNotifications(userId: String): Result<List<NotificationModel>> {
        return try {
            val notifications = supabase.from("notifications")
                .select {
                    filter { eq("user_id", userId) }
                    order(column = "created_at", order = Order.DESCENDING)
                    limit(50) // Limit to last 50 notifications
                }
                .decodeList<NotificationModel>()
            Result.success(notifications)
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "getNotifications failed: ${e.message}")
            Result.failure(e)
        }
    }

    // Get unread notification count
    suspend fun getUnreadCount(userId: String): Result<Int> {
        return try {
            val notifications = supabase.from("notifications")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("is_read", false)
                    }
                }
                .decodeList<NotificationModel>()
            Result.success(notifications.size)
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "getUnreadCount failed: ${e.message}")
            Result.failure(e)
        }
    }

    // Mark notification as read
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            supabase.from("notifications")
                .update({ set("is_read", true) }) {
                    filter { eq("id", notificationId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "markAsRead failed: ${e.message}")
            Result.failure(e)
        }
    }

    // Mark all notifications as read for a user
    suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            supabase.from("notifications")
                .update({ set("is_read", true) }) {
                    filter { eq("user_id", userId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "markAllAsRead failed: ${e.message}")
            Result.failure(e)
        }
    }

    // Delete a notification
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            supabase.from("notifications")
                .delete {
                    filter { eq("id", notificationId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "deleteNotification failed: ${e.message}")
            Result.failure(e)
        }
    }
}
