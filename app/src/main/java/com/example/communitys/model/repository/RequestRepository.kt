package com.example.communitys.model.repository

import com.example.communitys.CommUnityApplication
import com.example.communitys.model.data.RequestModel
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class RequestRepository {

    private val supabase = CommUnityApplication.supabase

    // ── Submit a new document request ─────────────────────────────────────────

    suspend fun submitRequest(
        userId: String,
        documentType: String,
        purpose: String,
        paymentMethod: String
    ): Result<Unit> {
        return try {
            supabase.from("requests").insert(
                mapOf(
                    "user_id" to userId,
                    "document_type" to documentType,
                    "purpose" to purpose,
                    "payment_method" to paymentMethod
                    // reference_number and status use DB defaults
                )
            )
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
