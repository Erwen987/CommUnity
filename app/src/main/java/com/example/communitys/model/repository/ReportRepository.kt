package com.example.communitys.model.repository

import com.example.communitys.CommUnityApplication
import com.example.communitys.model.data.ReportModel
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class ReportRepository {

    private val supabase = CommUnityApplication.supabase

    // ── Create report — used by ReportIssueActivity ───────────────────────────
    // Returns Result<Unit> so ReportIssueActivity compiles without changes

    suspend fun createReport(report: ReportModel): Result<Unit> {
        return try {
            supabase.from("reports").insert(report)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ReportRepository", "createReport failed: ${e.message}")
            Result.failure(Exception(e.message ?: "Failed to save report"))
        }
    }

    // ── Submit report — used by ReportIssueViewModel ──────────────────────────

    suspend fun submitReport(
        category: String,
        description: String,
        location: String,
        imageUrl: String?
    ): Result<Unit> {
        return try {
            // Get current user ID
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: throw Exception("User not authenticated")

            // Create report object
            val report = mapOf(
                "user_id" to userId,
                "category" to category,
                "description" to description,
                "location" to location,
                "image_url" to imageUrl,
                "status" to "pending"
            )

            supabase.from("reports").insert(report)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ReportRepository", "submitReport failed: ${e.message}")
            Result.failure(Exception(e.message ?: "Failed to submit report"))
        }
    }

    // ── Get reports for one user — used by DashboardViewModel ─────────────────

    suspend fun getUserReports(userId: String): Result<List<ReportModel>> {
        return try {
            val reports = supabase.from("reports")
                .select {
                    filter { eq("user_id", userId) }
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<ReportModel>()
            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Get all reports — for officials web dashboard later ───────────────────

    suspend fun getAllReports(): Result<List<ReportModel>> {
        return try {
            val reports = supabase.from("reports")
                .select {
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<ReportModel>()
            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Update status — officials use this on web dashboard ──────────────────

    suspend fun updateReportStatus(reportId: String, status: String): Result<Unit> {
        return try {
            supabase.from("reports")
                .update({ set("status", status) }) {
                    filter { eq("id", reportId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}