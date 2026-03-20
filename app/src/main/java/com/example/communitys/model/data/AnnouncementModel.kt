package com.example.communitys.model.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnnouncementModel(
    @SerialName("id")         val id: String = "",
    @SerialName("barangay")   val barangay: String = "",
    @SerialName("title")      val title: String = "",
    @SerialName("body")       val body: String = "",
    @SerialName("image_url")  val imageUrl: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("expires_at") val expiresAt: String = ""
)
