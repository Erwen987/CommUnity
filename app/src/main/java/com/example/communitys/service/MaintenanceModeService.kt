package com.example.communitys.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.communitys.CommUnityApplication
import com.example.communitys.view.login.LoginActivity
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SystemSetting(
    @SerialName("key") val key: String,
    @SerialName("value") val value: String
)

class MaintenanceModeService : Service() {

    private val supabase = CommUnityApplication.supabase
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isMaintenanceMode = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MaintenanceModeService created")
        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "MaintenanceModeService started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startPolling() {
        Log.d(TAG, "Starting maintenance mode polling")
        serviceScope.launch {
            while (true) {
                try {
                    checkMaintenanceMode()
                    delay(3000) // Check every 3 seconds
                } catch (e: Exception) {
                    Log.e(TAG, "Error in polling loop: ${e.message}")
                    delay(5000) // Wait longer on error
                }
            }
        }
    }

    private suspend fun checkMaintenanceMode() {
        try {
            val result = supabase.from("system_settings")
                .select()
                .decodeSingle<SystemSetting>()

            Log.d(TAG, "Current maintenance mode: ${result.value}")
            
            val maintenanceEnabled = result.value == "true"
            if (maintenanceEnabled && !isMaintenanceMode) {
                handleMaintenanceMode()
            } else if (!maintenanceEnabled) {
                isMaintenanceMode = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking maintenance mode: ${e.message}")
        }
    }

    private fun handleMaintenanceMode() {
        if (isMaintenanceMode) return
        isMaintenanceMode = true
        
        Log.d(TAG, "Maintenance mode activated - logging out user")
        
        serviceScope.launch {
            try {
                // Sign out the user
                supabase.auth.signOut()
                
                // Show maintenance dialog and redirect to login
                val intent = Intent(this@MaintenanceModeService, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("MAINTENANCE_MODE", true)
                }
                startActivity(intent)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling maintenance mode: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "MaintenanceModeService destroyed")
    }

    companion object {
        private const val TAG = "MaintenanceModeService"
    }
}
