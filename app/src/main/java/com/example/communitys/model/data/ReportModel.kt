package com.example.communitys.model.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReportModel(
    @SerialName("id")           val id: String = "",
    @SerialName("user_id")      val userId: String = "",
    @SerialName("problem")      val problem: String = "",
    @SerialName("description")  val description: String = "",
    @SerialName("image_url")    val imageUrl: String? = null,
    @SerialName("location_lat") val locationLat: Double? = null,
    @SerialName("location_lng") val locationLng: Double? = null,
    @SerialName("status")         val status: String = "pending",
    @SerialName("created_at")     val createdAt: String = "",
    @SerialName("points_awarded") val pointsAwarded: Int? = null,
    @SerialName("barangay")       val barangay: String? = null
)