package com.example.communitys.model.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReportCategoryModel(
    @SerialName("id")          val id: String = "",
    @SerialName("name")        val name: String = "",
    @SerialName("description") val description: String? = null,
    @SerialName("points")      val points: Int = 0,
    @SerialName("sort_order")  val sortOrder: Int = 0
)
