package com.example.communitys.model.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RequestModel(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("document_type") val documentType: String = "",
    val purpose: String = "",
    @SerialName("payment_method") val paymentMethod: String = "pay_on_site",
    val status: String = "reviewing",
    @SerialName("reference_number") val referenceNumber: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("barangay") val barangay: String? = null
)
