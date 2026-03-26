package com.example.communitys.model.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationModel(
    @SerialName("id")             val id: String = "",
    @SerialName("user_id")        val userId: String = "",
    @SerialName("type")           val type: String = "", // report_status, document_status, approval, points_awarded
    @SerialName("title")          val title: String = "",
    @SerialName("message")        val message: String = "",
    @SerialName("reference_id")   val referenceId: String? = null,
    @SerialName("reference_type") val referenceType: String? = null, // report, request, document
    @SerialName("is_read")        val isRead: Boolean = false,
    @SerialName("created_at")     val createdAt: String = ""
)
