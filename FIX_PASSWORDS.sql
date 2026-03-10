-- ============================================================================
-- FIX PASSWORDS FOR OFFICIAL ACCOUNTS
-- ============================================================================
-- Run this in Supabase SQL Editor to set passwords
-- ============================================================================

-- Check current status
SELECT 
  email, 
  confirmed_at IS NOT NULL as is_confirmed,
  encrypted_password IS NOT NULL as has_password
FROM auth.users
WHERE email IN ('jerwenbacani80@gmail.com', 'pandahuntergamer09@gmail.com');

-- Set password for pandahuntergamer09@gmail.com
UPDATE auth.users
SET 
  encrypted_password = crypt('jerwenpogi9', gen_salt('bf')),
  confirmed_at = NOW(),
  email_confirmed_at = NOW()
WHERE email = 'pandahuntergamer09@gmail.com';

-- Set password for jerwenbacani80@gmail.com  
UPDATE auth.users
SET 
  encrypted_password = crypt('jerwenpogi9', gen_salt('bf')),
  confirmed_at = NOW(),
  email_confirmed_at = NOW()
WHERE email = 'jerwenbacani80@gmail.com';

-- Verify it worked
SELECT 
  email, 
  confirmed_at IS NOT NULL as is_confirmed,
  encrypted_password IS NOT NULL as has_password
FROM auth.users
WHERE email IN ('jerwenbacani80@gmail.com', 'pandahuntergamer09@gmail.com');

-- ============================================================================
-- NOW YOU CAN LOGIN WITH:
-- ============================================================================
-- Email: pandahuntergamer09@gmail.com
-- Password: jerwenpogi9
--
-- Email: jerwenbacani80@gmail.com
-- Password: jerwenpogi9
-- ============================================================================
