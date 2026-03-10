-- ============================================================================
-- ADD USER ROLES - Officials vs Residents
-- ============================================================================
-- This adds role management to separate officials from residents
-- Run this in Supabase SQL Editor
-- ============================================================================

-- Step 1: Add role column to users table
ALTER TABLE public.users 
ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'resident';

-- Step 2: Add constraint to ensure valid roles
ALTER TABLE public.users
DROP CONSTRAINT IF EXISTS check_valid_role;

ALTER TABLE public.users
ADD CONSTRAINT check_valid_role 
CHECK (role IN ('resident', 'official', 'admin'));

-- Step 3: Create index for faster role queries
CREATE INDEX IF NOT EXISTS idx_users_role ON public.users(role);

-- Step 4: Update existing users to be residents
UPDATE public.users 
SET role = 'resident' 
WHERE role IS NULL;

-- ============================================================================
-- CREATE OFFICIAL/ADMIN ACCOUNTS
-- ============================================================================
-- You need to create these accounts manually in Supabase Auth
-- Then update their role in the users table

-- Example: After creating official@barangay.gov in Supabase Auth:
-- UPDATE public.users 
-- SET role = 'official' 
-- WHERE email = 'official@barangay.gov';

-- Example: After creating admin@barangay.gov in Supabase Auth:
-- UPDATE public.users 
-- SET role = 'admin' 
-- WHERE email = 'admin@barangay.gov';

-- ============================================================================
-- VERIFICATION
-- ============================================================================

-- Check if role column exists
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'users' AND column_name = 'role';

-- Check all users and their roles
SELECT email, first_name, last_name, role
FROM public.users
ORDER BY role, email;

-- ============================================================================
-- HOW TO CREATE OFFICIAL/ADMIN ACCOUNTS
-- ============================================================================
-- 
-- 1. Go to Supabase Dashboard → Authentication → Users
-- 2. Click "Add User"
-- 3. Enter email: official@barangay.gov (or your choice)
-- 4. Enter password
-- 5. Click "Create User"
-- 6. User will be auto-confirmed (no OTP needed)
-- 7. Then run this SQL to set their role:
--
--    UPDATE public.users 
--    SET role = 'official' 
--    WHERE email = 'official@barangay.gov';
--
-- ============================================================================
