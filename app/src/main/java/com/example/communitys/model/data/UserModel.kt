package com.example.communitys.model.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserModel(
    @SerialName("id")
    val id: String = "",

    @SerialName("auth_id")
    val authId: String = "",

    @SerialName("first_name")
    val firstName: String = "",

    @SerialName("last_name")
    val lastName: String = "",

    @SerialName("email")
    val email: String = "",

    @SerialName("barangay")
    val barangay: String = "",

    @SerialName("phone")
    val phone: String? = null,

    @SerialName("points")
    val points: Int? = 0,

    @SerialName("has_logged_in_before")
    val hasLoggedInBefore: Boolean? = false,

    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    @SerialName("is_banned")
    val isBanned: Boolean? = false,

    @SerialName("ban_reason")
    val banReason: String? = null,

    @SerialName("offense_count")
    val offenseCount: Int? = 0,

    @SerialName("suspended_until")
    val suspendedUntil: String? = null
) {
    // Full name helper used by ProfileFragment
    val name: String get() = "$firstName $lastName".trim()
}