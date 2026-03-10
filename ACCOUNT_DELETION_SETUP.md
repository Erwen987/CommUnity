# Account Deletion System Setup

This document explains how to set up the complete account deletion system that allows users to delete their accounts and reuse their email addresses.

## Overview

The account deletion system:
- Logs all deletions to `deleted_accounts` table for officials to review
- Completely removes user profile from `users` table
- Deletes the auth user from `auth.users` (making email available for reuse)
- Prevents orphaned auth users from logging in
- Shows clear messages to users whose accounts were deleted

## Database Setup

### Step 1: Run the SQL Script

Execute the entire `supabase_delete_account_fix.sql` script in your Supabase SQL Editor:

1. Open Supabase Dashboard → Your Project
2. Go to **SQL Editor**
3. Click **New Query**
4. Copy and paste the entire contents of `supabase_delete_account_fix.sql`
5. Click **Run** or press `Ctrl+Enter`

The script creates:
- `deleted_accounts` table with RLS policies
- `delete_user_account(p_reason TEXT)` function
- `is_email_available(p_email TEXT)` function
- `deleted_accounts_view` for officials
- `cleanup_orphaned_auth_users()` function

### Step 2: Clean Up Existing Orphaned Auth Users

If you've been testing and have orphaned auth users (auth records without profiles), clean them up:

```sql
-- Run this in Supabase SQL Editor
DELETE FROM auth.users 
WHERE id NOT IN (SELECT auth_id FROM public.users);
```

This removes any auth users that don't have a corresponding profile in the `users` table.

### Step 3: Verify Setup

Check that everything was created correctly:

1. **Verify Tables**:
   - Go to **Table Editor** → Check `deleted_accounts` table exists

2. **Verify Functions**:
   - Go to **Database** → **Functions** → Look for:
     - `delete_user_account`
     - `is_email_available`
     - `cleanup_orphaned_auth_users`

3. **Verify Policies**:
   ```sql
   SELECT * FROM pg_policies 
   WHERE tablename = 'deleted_accounts';
   ```
   Should show 2 policies:
   - `Users can log their own deletion` (INSERT)
   - `Officials can view deleted accounts` (SELECT)

## Testing the Complete Flow

### Test 1: Delete and Recreate Account

1. **Create a test account**:
   - Open app → Sign Up
   - Email: `test123@gmail.com`
   - Password: `Test1234`
   - First Name: `John`
   - Last Name: `Doe`
   - Barangay: `Test Barangay`
   - Verify OTP
   - Login successfully

2. **Delete the account**:
   - Go to Profile → Delete Account
   - Enter reason: "Testing deletion system"
   - Confirm deletion
   - App should log you out

3. **Try to login with deleted account**:
   - Email: `test123@gmail.com`
   - Password: `Test1234`
   - Should show: "Your account was deleted. Please sign up again."

4. **Register with same email**:
   - Sign Up with same email: `test123@gmail.com`
   - Should work! You can create a new account
   - Verify OTP
   - Login successfully

### Test 2: Verify Officials Can See Deleted Accounts

Run this query in Supabase SQL Editor:

```sql
SELECT * FROM deleted_accounts_view;
```

You should see:
- Email: `test123@gmail.com`
- First Name: `John`
- Last Name: `Doe`
- Barangay: `Test Barangay`
- Reason: "Testing deletion system"
- Deleted timestamp
- Days since deletion

## How It Works

### Account Deletion Flow

1. **User clicks "Delete Account"**:
   - App calls `AuthRepository.deleteAccount(reason)`
   - Repository tries to call SQL function `delete_user_account(p_reason)` via RPC
   - If RPC fails, falls back to manual deletion

2. **SQL Function executes** (preferred method):
   ```sql
   -- Step 1: Log to deleted_accounts
   INSERT INTO deleted_accounts (auth_id, email, first_name, last_name, barangay, reason)
   
   -- Step 2: Delete from users table
   DELETE FROM users WHERE auth_id = current_user_id
   
   -- Step 3: Delete from auth.users (makes email available)
   DELETE FROM auth.users WHERE id = current_user_id
   ```

3. **Manual deletion** (fallback if RPC fails):
   - Log to `deleted_accounts` table
   - Delete from `users` table
   - Auth user remains (will be cleaned up by orphan detection)

4. **App signs out user**:
   - User is logged out locally
   - Email is now available for new registration

### Orphaned Auth User Detection

The app has built-in protection against orphaned auth users:

1. **On Login** (`AuthRepository.signIn()`):
   - After successful auth, checks if profile exists
   - If no profile found → Signs out user immediately
   - Shows message: "Your account was deleted. Please sign up again."

2. **On getCurrentUser()** (when loading profile):
   - Checks if profile exists for current auth user
   - If no profile → Signs out user
   - Shows message: "Your account was deleted. Please sign up again if needed."

This ensures users can't get stuck in a logged-in state without a profile.

### Cleanup Function (Optional)

For periodic cleanup of orphaned auth users:

```sql
-- Run this manually or via cron job
SELECT cleanup_orphaned_auth_users();
```

Returns:
```json
{
  "success": true,
  "deleted_count": 5,
  "message": "Cleaned up 5 orphaned auth records"
}
```

This deletes auth users older than 1 day that don't have a profile.

## For Officials

### View Deleted Accounts

Officials can see all deleted accounts in the web admin panel or via SQL:

```sql
SELECT 
    email,
    first_name,
    last_name,
    barangay,
    reason,
    deleted_at,
    days_since_deletion
FROM deleted_accounts_view
ORDER BY deleted_at DESC;
```

### Check Email Availability

Check if an email is available for registration:

```sql
SELECT is_email_available('test@gmail.com');
-- Returns: true (available) or false (taken)
```

### Export Deleted Accounts Report

```sql
SELECT 
    email,
    first_name || ' ' || last_name as full_name,
    barangay,
    reason,
    TO_CHAR(deleted_at, 'YYYY-MM-DD HH24:MI:SS') as deleted_date
FROM deleted_accounts
WHERE deleted_at >= NOW() - INTERVAL '30 days'
ORDER BY deleted_at DESC;
```

## Troubleshooting

### Issue: Can't register with deleted email

**Symptoms**: 
- User deleted account
- Tries to register with same email
- Gets "Email already registered" error

**Cause**: Auth user still exists in `auth.users`

**Solution**: 
```sql
-- Clean up orphaned auth users
DELETE FROM auth.users 
WHERE id NOT IN (SELECT auth_id FROM public.users);
```

### Issue: User can login but sees empty profile

**Symptoms**:
- User logs in successfully
- Profile shows empty name ("Welcome, ")
- App crashes or shows errors

**Cause**: Orphaned auth user (profile deleted but auth user remains)

**Solution**: 
1. The app will auto-detect and sign them out
2. User will see: "Your account was deleted. Please sign up again."
3. Run cleanup query to prevent future occurrences:
   ```sql
   DELETE FROM auth.users 
   WHERE id NOT IN (SELECT auth_id FROM public.users);
   ```

### Issue: RPC call fails (postgrest error)

**Symptoms**: 
- Logs show "RPC call failed, using manual deletion"
- Account is deleted but email might not be available

**Cause**: Supabase function not properly set up or permissions issue

**Solution**:
1. Verify function exists:
   ```sql
   SELECT * FROM pg_proc WHERE proname = 'delete_user_account';
   ```

2. Verify permissions:
   ```sql
   GRANT EXECUTE ON FUNCTION public.delete_user_account(TEXT) TO authenticated;
   ```

3. If still failing, manual deletion works as fallback. Just run cleanup query periodically.

### Issue: Officials can't see deleted accounts

**Cause**: RLS policy not applied or user not authenticated

**Solution**: 
1. Verify policy exists:
   ```sql
   SELECT * FROM pg_policies 
   WHERE tablename = 'deleted_accounts' 
   AND policyname = 'Officials can view deleted accounts';
   ```

2. Ensure user is authenticated when querying

3. Try using the view instead:
   ```sql
   SELECT * FROM deleted_accounts_view;
   ```

## Security Notes

1. **RLS Enabled**: `deleted_accounts` table has Row Level Security
2. **User can only log their own deletion**: INSERT policy checks `auth_id = auth.uid()`
3. **Officials can view all deletions**: SELECT policy allows all authenticated users
4. **Function is SECURITY DEFINER**: Runs with elevated privileges to delete from auth.users
5. **Cleanup function**: Should only be granted to service_role or admin (not regular users)
6. **Audit trail**: All deletions are permanently logged in `deleted_accounts` table

## Testing Checklist

Complete this checklist to ensure everything works:

- [ ] Run `supabase_delete_account_fix.sql` in Supabase SQL Editor
- [ ] Run cleanup query to remove existing orphaned auth users
- [ ] Verify `deleted_accounts` table exists in Table Editor
- [ ] Verify functions exist in Database → Functions
- [ ] Test: Create account → Delete → Try to login (should show "account deleted" message)
- [ ] Test: Create account → Delete → Register with same email (should work)
- [ ] Test: View deleted accounts via SQL query
- [ ] Verify app auto-detects orphaned auth users and signs them out
- [ ] Check logs show proper deletion flow

## Maintenance

### Daily/Weekly Tasks

1. **Monitor deleted accounts**:
   ```sql
   SELECT COUNT(*) as deletions_this_week
   FROM deleted_accounts
   WHERE deleted_at >= NOW() - INTERVAL '7 days';
   ```

2. **Check for orphaned auth users**:
   ```sql
   SELECT COUNT(*) as orphaned_count
   FROM auth.users
   WHERE id NOT IN (SELECT auth_id FROM public.users);
   ```

3. **Review deletion reasons**:
   ```sql
   SELECT reason, COUNT(*) as count
   FROM deleted_accounts
   WHERE deleted_at >= NOW() - INTERVAL '30 days'
   GROUP BY reason
   ORDER BY count DESC;
   ```

### Optional: Automated Cleanup

Set up a Supabase Edge Function or cron job to run daily:

```sql
SELECT cleanup_orphaned_auth_users();
```

This keeps the auth.users table clean and ensures emails are always available for reuse.

## Next Steps

1. **Test thoroughly**: Follow the testing checklist above
2. **Monitor for a week**: Watch for any issues with deletion/recreation
3. **Set up web admin panel**: Connect CommUnity-main to view deleted accounts
4. **Document for users**: Add deletion policy to Terms of Service
5. **Train officials**: Show them how to view and analyze deleted accounts
