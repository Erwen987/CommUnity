package com.example.communitys.model.repository

import com.example.communitys.CommUnityApplication
import com.example.communitys.model.data.ReportCategoryModel
import com.example.communitys.model.data.ReportModel
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ReportRepository {

    private val supabase = CommUnityApplication.supabase

    // ── Create report — used by ReportIssueActivity ───────────────────────────
    // Returns Result<String> with the new report's UUID

    private suspend fun countActiveReports(userId: String): Int {
        return try {
            supabase.from("reports")
                .select(columns = Columns.list("id", "status")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<ReportModel>()
                .count { it.status != "resolved" }
        } catch (e: Exception) { 0 }
    }

    suspend fun createReport(report: ReportModel): Result<String> {
        return try {
            val activeCount = countActiveReports(report.userId)
            if (activeCount >= 5) {
                return Result.failure(Exception(
                    "You have $activeCount unresolved report${if (activeCount != 1) "s" else ""}. " +
                    "You can only have 5 active reports at a time. " +
                    "Please wait for your reports to be resolved before submitting a new one."
                ))
            }

            val userData = supabase.from("users")
                .select(columns = Columns.list("first_name", "last_name")) {
                    filter { eq("auth_id", report.userId) }
                }
                .decodeList<com.example.communitys.model.data.UserModel>()
                .firstOrNull()
            val residentName = if (userData != null) "${userData.firstName} ${userData.lastName}".trim() else ""

            val data = buildJsonObject {
                put("user_id",       report.userId)
                put("problem",       report.problem)
                put("description",   report.description)
                put("status",        report.status)
                put("resident_name", residentName)
                if (report.imageUrl    != null) put("image_url",    report.imageUrl)
                if (report.locationLat != null) put("location_lat", report.locationLat)
                if (report.locationLng != null) put("location_lng", report.locationLng)
                if (report.barangay   != null) put("barangay",     report.barangay)
            }
            val inserted = supabase.from("reports").insert(data) {
                select()
            }.decodeSingle<ReportModel>()
            Result.success(inserted.id)
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
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: throw Exception("User not authenticated")

            val activeCount = countActiveReports(userId)
            if (activeCount >= 5) {
                return Result.failure(Exception(
                    "You have $activeCount unresolved report${if (activeCount != 1) "s" else ""}. " +
                    "You can only have 5 active reports at a time. " +
                    "Please wait for your reports to be resolved before submitting a new one."
                ))
            }

            val userData = supabase.from("users")
                .select(columns = Columns.list("first_name", "last_name")) {
                    filter { eq("auth_id", userId) }
                }
                .decodeList<com.example.communitys.model.data.UserModel>()
                .firstOrNull()
            val residentName = if (userData != null) "${userData.firstName} ${userData.lastName}".trim() else ""

            val report = mapOf(
                "user_id"       to userId,
                "category"      to category,
                "description"   to description,
                "location"      to location,
                "image_url"     to imageUrl,
                "status"        to "pending",
                "resident_name" to residentName
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

    // ── Get user's barangay from users table ──────────────────────────────────

    suspend fun getUserBarangay(userId: String): String? {
        return try {
            supabase.from("users")
                .select { filter { eq("auth_id", userId) } }
                .decodeList<com.example.communitys.model.data.UserModel>()
                .firstOrNull()?.barangay
        } catch (e: Exception) {
            null
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

    // ── Award points for a report ─────────────────────────────────────────────
    // Called after submission for auto-awarded problems (e.g. Broken Road / Pothole).
    // Inserts a rewards row, updates users.points, and stamps points_awarded on the report.

    suspend fun awardPoints(userId: String, reportId: String, points: Int, reason: String): Result<Unit> {
        return try {
            // Insert rewards record (non-fatal — just a log entry)
            try {
                supabase.from("rewards").insert(
                    mapOf(
                        "user_id" to userId,
                        "points"  to points,
                        "reason"  to reason
                    )
                )
            } catch (e: Exception) {
                android.util.Log.w("ReportRepository", "rewards log insert failed (non-fatal): ${e.message}")
            }

            // Stamp points on the report
            supabase.from("reports")
                .update({ set("points_awarded", points) }) {
                    filter { eq("id", reportId) }
                }

            // Fetch current points then increment
            val currentPoints = supabase.from("users")
                .select { filter { eq("auth_id", userId) } }
                .decodeList<com.example.communitys.model.data.UserModel>()
                .firstOrNull()?.points ?: 0

            supabase.from("users")
                .update({ set("points", currentPoints + points) }) {
                    filter { eq("auth_id", userId) }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ReportRepository", "awardPoints failed: ${e.message}")
            Result.failure(Exception(e.message ?: "Failed to award points"))
        }
    }

    // ── Get report categories from DB ────────────────────────────────────────

    suspend fun getCategories(): Result<List<ReportCategoryModel>> {
        return try {
            val categories = supabase.from("report_categories")
                .select { order(column = "sort_order", order = Order.ASCENDING) }
                .decodeList<ReportCategoryModel>()
            Result.success(categories)
        } catch (e: Exception) {
            android.util.Log.e("ReportRepository", "getCategories failed: ${e.message}")
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