package com.example.communitys.model.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppealModel(
    @SerialName("id")      val id: String = "",
    @SerialName("email")   val email: String = "",
    @SerialName("status")  val status: String = "pending"
)
