# Quick Setup: Role-Based Access Control

## What Changed?

✅ Mobile app now blocks officials/admins from logging in
✅ Web portal now blocks residents from logging in
✅ Added role field to UserModel with helper methods
✅ Updated login flow in both platforms

## Quick Steps

### 1. Run SQL in Supabase (2 minutes)
Open Supabase SQL Editor and run `ADD_USER_ROLES.sql`

### 2. Create Official Account (1 minute)
1. Supabase Dashboard → Authentication → Users
2. Click "Add User"
3. Email: `official@barangay.gov`
4. Password: (your choice)
5. ✅ Check "Auto Confirm User"
6. Click "Create User"

### 3. Set Role (30 seconds)
In Supabase SQL Editor:
```sql
UPDATE public.users 
SET role = 'official' 
WHERE email = 'official@barangay.gov';
```

### 4. Test It!

**Mobile App:**
- Login with resident account → ✅ Works
- Login with official@barangay.gov → ❌ "This account is for officials only. Please use the web portal to login."

**Web Portal:**
- Login with official@barangay.gov → ✅ Works
- Login with resident account → ❌ "This portal is for officials and admins only. Please use the mobile app."

## Files Modified

**Mobile App:**
- `UserModel.kt` - Added role field and helper methods
- `AuthRepository.kt` - Added role checking in login()
- `AuthViewModel.kt` - Updated login() to support requireResident flag
- `LoginActivity.kt` - Passes requireResident = true

**Web Portal:**
- `Login.js` - Checks role after authentication and rejects residents

## That's It!

See `ROLE_BASED_ACCESS_SETUP.md` for detailed documentation.
