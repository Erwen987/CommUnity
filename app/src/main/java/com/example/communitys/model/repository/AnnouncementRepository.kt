package com.example.communitys.model.repository

import com.example.communitys.CommUnityApplication
import com.example.communitys.model.data.AnnouncementModel
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AnnouncementRepository {

    private val supabase = CommUnityApplication.supabase

    suspend fun getActiveAnnouncements(barangay: String): Result<List<AnnouncementModel>> {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val now = sdf.format(Date())

            val list = supabase.from("announcements")
                .select {
                    filter {
                        eq("barangay", barangay)
                        gt("expires_at", now)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(10)
                }
                .decodeList<AnnouncementModel>()

            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
