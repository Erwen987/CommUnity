# Testing Maintenance Mode in Android App

## Prerequisites
- Android app installed and running
- User logged in
- Admin access to web dashboard

## Testing Steps

### Step 1: Check Service is Running
1. Open Android Studio
2. Go to **Logcat** (View → Tool Windows → Logcat)
3. Filter by "MaintenanceModeService"
4. You should see:
   ```
   MaintenanceModeService created
   MaintenanceModeService started
   Current maintenance mode: false
   Successfully subscribed to maintenance mode changes
   ```

### Step 2: Enable Maintenance Mode
1. Open web admin dashboard
2. Go to Settings
3. Toggle "Maintenance Mode" to ON
4. Watch the Android Logcat

### Step 3: Expected Behavior
Within 5-10 seconds, you should see in Logcat:
```
Received update: ...
Maintenance mode changed to: true
Maintenance mode activated - logging out user
```

Then the app should:
1. Automatically log out the user
2. Redirect to login screen
3. Show maintenance dialog with message:
   "⚠️ System Maintenance - The system is currently under maintenance..."

## Troubleshooting

### Issue: Service not starting
**Check:**
- Is MainContainerActivity being launched after login?
- Check Logcat for any errors

**Fix:**
- Make sure you're logged in (service only starts after login)
- Rebuild the app

### Issue: No realtime updates
**Check Logcat for:**
- "Error subscribing to maintenance mode"
- If you see this, the service will fall back to polling (checks every 5 seconds)

**Verify Database:**
```sql
-- Check if realtime is enabled
SELECT tablename FROM pg_publication_tables 
WHERE schemaname = 'public' AND tablename = 'system_settings';
```

### Issue: Dialog not showing
**Check:**
- Is MAINTENANCE_MODE extra being passed to LoginActivity?
- Check Logcat for "Maintenance mode activated"

**Debug:**
Add breakpoint in `LoginActivity.showMaintenanceModeDialog()`

## Manual Testing

### Test 1: User Already Logged In
1. User is using the app
2. Admin enables maintenance mode
3. **Expected**: User logged out within 5-10 seconds, sees dialog

### Test 2: Maintenance Already Enabled
1. Enable maintenance mode first
2. Try to log in to app
3. **Expected**: Login succeeds, then immediately logged out with dialog

### Test 3: Disable Maintenance Mode
1. Maintenance mode is ON
2. Admin disables it
3. **Expected**: Users can log in normally

## Logcat Filters

Use these filters in Android Studio Logcat:

```
MaintenanceModeService
```

Or for more detail:
```
tag:MaintenanceModeService|tag:LoginActivity
```

## Database Queries

### Check current status:
```sql
SELECT * FROM system_settings WHERE key = 'maintenance_mode';
```

### Enable maintenance mode:
```sql
UPDATE system_settings 
SET value = 'true', updated_at = NOW() 
WHERE key = 'maintenance_mode';
```

### Disable maintenance mode:
```sql
UPDATE system_settings 
SET value = 'false', updated_at = NOW() 
WHERE key = 'maintenance_mode';
```

## Common Issues

1. **Service stops when app is minimized**
   - This is expected behavior
   - Service only runs while app is active

2. **Delay in detection**
   - Realtime: Should be instant (< 1 second)
   - Polling fallback: Up to 5 seconds delay

3. **Multiple logout dialogs**
   - Check if service is being started multiple times
   - Should only start once in MainContainerActivity.onCreate()
