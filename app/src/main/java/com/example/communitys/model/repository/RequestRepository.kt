package com.example.communitys.model.repository

import com.example.communitys.CommUnityApplication
import com.example.communitys.model.data.RequestModel
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RequestRepository {

    private val supabase = CommUnityApplication.supabase

    // ── Submit a new document request ─────────────────────────────────────────

    suspend fun getUserBarangay(userId: String): String? {
        return try {
            supabase.from("users")
                .select(columns = Columns.list("barangay")) {
                    filter { eq("auth_id", userId) }
                }
                .decodeList<com.example.communitys.model.data.UserModel>()
                .firstOrNull()?.barangay
        } catch (e: Exception) {
            null
        }
    }

    suspend fun submitRequest(
        userId: String,
        documentType: String,
        purpose: String,
        paymentMethod: String,
        proofUrl: String? = null,
        barangay: String? = null
    ): Result<Unit> {
        return try {
            // Check active request limit (max 5 unresolved)
            val activeCount = supabase.from("requests")
                .select(columns = Columns.list("id", "status")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<RequestModel>()
                .count { it.status != "claimed" && it.status != "rejected" }

            if (activeCount >= 5) {
                return Result.failure(Exception(
                    "You have $activeCount active document request${if (activeCount != 1) "s" else ""}. " +
                    "You can only have 5 active requests at a time. " +
                    "Please wait for your requests to be completed before submitting a new one."
                ))
            }

            val userData = supabase.from("users")
                .select(columns = Columns.list("first_name", "last_name")) {
                    filter { eq("auth_id", userId) }
                }
                .decodeList<com.example.communitys.model.data.UserModel>()
                .firstOrNull()
            val residentName = if (userData != null) "${userData.firstName} ${userData.lastName}".trim() else ""

            val data = buildJsonObject {
                put("user_id",       userId)
                put("document_type", documentType)
                put("purpose",       purpose)
                put("payment_method", paymentMethod)
                put("status",        "pending")
                put("resident_name", residentName)
                if (proofUrl != null) put("proof_url", proofUrl)
                if (barangay != null) put("barangay",  barangay)
            }
            supabase.from("requests").insert(data)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("RequestRepository", "submitRequest failed: ${e.message}")
            Result.failure(Exception(e.message ?: "Failed to submit request"))
        }
    }

    // ── Get all requests for a user (active + history) ────────────────────────

    suspend fun getUserRequests(userId: String): Result<List<RequestModel>> {
        return try {
            val result = supabase.from("requests")
                .select {
                    filter { eq("user_id", userId) }
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<RequestModel>()
            Result.success(result)
        } catch (e: Exception) {
            android.util.Log.e("RequestRepository", "getUserRequests failed: ${e.message}")
            Result.failure(e)
        }
    }
}
