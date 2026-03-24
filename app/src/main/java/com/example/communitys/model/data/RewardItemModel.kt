package com.example.communitys.model.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RewardItemModel(
    @SerialName("id")              val id: String = "",
    @SerialName("name")            val name: String = "",
    @SerialName("description")     val description: String? = null,
    @SerialName("category")        val category: String = "",
    @SerialName("points_required") val pointsRequired: Int = 0,
    @SerialName("stock")           val stock: Int = 0,
    @SerialName("is_active")       val isActive: Boolean = true
)
