# Maintenance Mode for Android App

## Overview
The Android app now supports real-time maintenance mode detection. When an admin enables maintenance mode from the web dashboard, all app users will be automatically logged out and shown a maintenance message.

## How It Works

### 1. **Background Service**
- `MaintenanceModeService` runs in the background while the app is active
- Monitors the `system_settings` table in Supabase for maintenance mode changes
- Uses Supabase Realtime to receive instant updates

### 2. **Real-time Detection**
- When maintenance mode is enabled (`value = 'true'`):
  - Service detects the change immediately
  - User is automatically signed out
  - App redirects to LoginActivity
  - Maintenance dialog is shown

### 3. **User Experience**
```
Admin enables maintenance mode
         ↓
App detects change (real-time)
         ↓
User is logged out automatically
         ↓
Redirected to login screen
         ↓
Dialog shows: "System is under maintenance"
```

## Files Created/Modified

### New Files:
1. **`MaintenanceModeService.kt`**
   - Background service that monitors maintenance mode
   - Subscribes to Supabase Realtime changes
   - Handles automatic logout and redirection

### Modified Files:
1. **`MainContainerActivity.kt`**
   - Starts MaintenanceModeService when app launches
   - Service runs while user is logged in

2. **`LoginActivity.kt`**
   - Added maintenance mode dialog
   - Shows warning when user is logged out due to maintenance

3. **`AndroidManifest.xml`**
   - Registered MaintenanceModeService

## Testing

### Test Scenario 1: User Already Logged In
1. User is using the app (any screen)
2. Admin enables maintenance mode from web dashboard
3. **Expected**: User is immediately logged out and sees maintenance dialog

### Test Scenario 2: User Tries to Login During Maintenance
1. Maintenance mode is already enabled
2. User tries to log in
3. **Expected**: Login succeeds but service detects maintenance and logs them out immediately

## Database Requirements

The `system_settings` table must have:
- Realtime enabled (already configured)
- RLS policy allowing public read access to `maintenance_mode` key (already configured)

## Service Lifecycle

- **Started**: When MainContainerActivity is created (user logs in)
- **Runs**: In background while app is active
- **Stopped**: When app is closed or user logs out

## Benefits

✅ Real-time response - no polling needed
✅ Automatic logout - no user action required
✅ Clear messaging - users know why they were logged out
✅ Consistent with web officials portal behavior
✅ Low battery impact - uses Supabase Realtime (WebSocket)

## Future Enhancements

- Add notification before logout (e.g., "Maintenance in 5 minutes")
- Show estimated maintenance completion time
- Allow users to check maintenance status from login screen
