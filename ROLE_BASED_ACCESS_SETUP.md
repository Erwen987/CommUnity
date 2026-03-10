# Role-Based Access Control Setup Guide

## Overview
This system separates users into three roles:
- **Residents**: Can ONLY login on mobile app
- **Officials**: Can ONLY login on web portal
- **Admins**: Can ONLY login on web portal

## Step 1: Run the SQL Script in Supabase

1. Go to your Supabase Dashboard
2. Navigate to **SQL Editor**
3. Copy and paste the contents of `ADD_USER_ROLES.sql`
4. Click **Run** to execute

This will:
- Add a `role` column to the users table
- Set all existing users to 'resident' by default
- Add validation constraints
- Create an index for performance

## Step 2: Create Official/Admin Accounts

### Method 1: Using Supabase Dashboard (Recommended)

1. Go to **Supabase Dashboard → Authentication → Users**
2. Click **"Add User"** button
3. Fill in the form:
   - Email: `official@barangay.gov` (or your choice)
   - Password: Create a strong password
   - Auto Confirm User: ✅ Check this box (no OTP needed)
4. Click **"Create User"**
5. The user will be created and auto-confirmed

### Method 2: Using SQL (After creating in Auth)

After creating the user in Supabase Auth, update their role:

```sql
-- For officials
UPDATE public.users 
SET role = 'official' 
WHERE email = 'official@barangay.gov';

-- For admins
UPDATE public.users 
SET role = 'admin' 
WHERE email = 'admin@barangay.gov';
```

## Step 3: Test the Access Control

### Test Mobile App (Residents Only)

1. Try logging in with a resident account → ✅ Should work
2. Try logging in with an official account → ❌ Should show error:
   - "This account is for officials only. Please use the web portal to login."

### Test Web Portal (Officials/Admins Only)

1. Try logging in with an official account → ✅ Should work
2. Try logging in with an admin account → ✅ Should work
3. Try logging in with a resident account → ❌ Should show error:
   - "This portal is for officials and admins only. Please use the mobile app."

## How It Works

### Mobile App (LoginActivity.kt)
- Calls `viewModel.login(email, password, requireResident = true)`
- AuthRepository checks the user's role after authentication
- If role is 'official' or 'admin', signs them out and shows error
- Only 'resident' role can proceed

### Web Portal (Login.js)
- After successful authentication, fetches user profile from database
- Checks if role is 'official' or 'admin'
- If role is 'resident', signs them out and shows error
- Only 'official' and 'admin' roles can proceed

## Creating Multiple Officials/Admins

You can create as many official/admin accounts as needed:

```sql
-- Example: Create multiple officials
UPDATE public.users SET role = 'official' WHERE email = 'official1@barangay.gov';
UPDATE public.users SET role = 'official' WHERE email = 'official2@barangay.gov';
UPDATE public.users SET role = 'official' WHERE email = 'official3@barangay.gov';

-- Example: Create multiple admins
UPDATE public.users SET role = 'admin' WHERE email = 'admin1@barangay.gov';
UPDATE public.users SET role = 'admin' WHERE email = 'admin2@barangay.gov';
```

## Verification Queries

Check all users and their roles:
```sql
SELECT email, first_name, last_name, role, created_at
FROM public.users
ORDER BY role, email;
```

Count users by role:
```sql
SELECT role, COUNT(*) as count
FROM public.users
GROUP BY role;
```

## Important Notes

1. **New Signups**: All new users who sign up through the mobile app will automatically be 'resident'
2. **Role Changes**: You can change a user's role at any time using SQL
3. **No Self-Registration**: Officials and admins must be created manually in Supabase Dashboard
4. **Security**: The role check happens on both client and server side for security

## Troubleshooting

### Issue: Official can still login to mobile app
- Check the user's role in database: `SELECT role FROM users WHERE email = 'official@barangay.gov'`
- Make sure it's set to 'official' or 'admin'
- Rebuild the mobile app

### Issue: Resident can still login to web portal
- Clear browser cache and cookies
- Make sure the web app is using the latest code
- Check browser console for errors

### Issue: User has no role
- Run: `UPDATE users SET role = 'resident' WHERE role IS NULL;`
- This sets default role for any users without one
