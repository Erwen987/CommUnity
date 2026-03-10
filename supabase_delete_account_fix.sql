-- ============================================
-- COMPLETE ACCOUNT DELETION SYSTEM
-- ============================================

-- 1. Create deleted_accounts table (if not exists)
CREATE TABLE IF NOT EXISTS public.deleted_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) DEFAULT '',
    last_name VARCHAR(100) DEFAULT '',
    barangay VARCHAR(255) DEFAULT '',
    reason TEXT NOT NULL,
    deleted_at TIMESTAMP DEFAULT NOW()
);

-- 2. Enable RLS on deleted_accounts
ALTER TABLE public.deleted_accounts ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Users can log their own deletion" ON public.deleted_accounts;
DROP POLICY IF EXISTS "Officials can view deleted accounts" ON public.deleted_accounts;

-- 3. Create policies for deleted_accounts
CREATE POLICY "Users can log their own deletion"
ON public.deleted_accounts FOR INSERT 
TO authenticated
WITH CHECK (auth_id = auth.uid());

CREATE POLICY "Officials can view deleted accounts"
ON public.deleted_accounts FOR SELECT 
TO authenticated
USING (true);

-- ============================================
-- FUNCTION: Complete Account Deletion
-- ============================================

-- This function will:
-- 1. Log the deletion to deleted_accounts
-- 2. Delete from users table
-- 3. Delete from auth.users (making email available for reuse)

CREATE OR REPLACE FUNCTION public.delete_user_account(
    p_reason TEXT DEFAULT 'No reason provided'
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_auth_id UUID;
    v_user_record RECORD;
    v_result JSON;
BEGIN
    -- Get current user's auth ID
    v_auth_id := auth.uid();
    
    IF v_auth_id IS NULL THEN
        RETURN json_build_object(
            'success', false,
            'message', 'Not authenticated'
        );
    END IF;

    -- Get user details before deletion
    SELECT * INTO v_user_record
    FROM public.users
    WHERE auth_id = v_auth_id;

    IF NOT FOUND THEN
        RETURN json_build_object(
            'success', false,
            'message', 'User profile not found'
        );
    END IF;

    -- Step 1: Log the deletion
    INSERT INTO public.deleted_accounts (
        auth_id,
        email,
        first_name,
        last_name,
        barangay,
        reason
    ) VALUES (
        v_auth_id,
        v_user_record.email,
        v_user_record.first_name,
        v_user_record.last_name,
        v_user_record.barangay,
        p_reason
    );

    -- Step 2: Delete from users table
    DELETE FROM public.users WHERE auth_id = v_auth_id;

    -- Step 3: Delete from auth.users (this makes email available for reuse)
    DELETE FROM auth.users WHERE id = v_auth_id;

    RETURN json_build_object(
        'success', true,
        'message', 'Account deleted successfully',
        'email', v_user_record.email
    );

EXCEPTION
    WHEN OTHERS THEN
        RETURN json_build_object(
            'success', false,
            'message', 'Error: ' || SQLERRM
        );
END;
$$;

-- Grant execute permission to authenticated users
GRANT EXECUTE ON FUNCTION public.delete_user_account(TEXT) TO authenticated;

-- ============================================
-- FUNCTION: Check if email is available
-- ============================================

CREATE OR REPLACE FUNCTION public.is_email_available(p_email TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_exists BOOLEAN;
BEGIN
    -- Check if email exists in auth.users
    SELECT EXISTS (
        SELECT 1 FROM auth.users WHERE email = p_email
    ) INTO v_exists;
    
    RETURN NOT v_exists;
END;
$$;

GRANT EXECUTE ON FUNCTION public.is_email_available(TEXT) TO anon, authenticated;

-- ============================================
-- VIEW: For officials to see deleted accounts
-- ============================================

CREATE OR REPLACE VIEW public.deleted_accounts_view AS
SELECT 
    id,
    email,
    first_name,
    last_name,
    barangay,
    reason,
    deleted_at,
    EXTRACT(DAY FROM NOW() - deleted_at) as days_since_deletion
FROM public.deleted_accounts
ORDER BY deleted_at DESC;

-- Grant select on view to authenticated users
GRANT SELECT ON public.deleted_accounts_view TO authenticated;

-- ============================================
-- TRIGGER: Prevent login with deleted account
-- ============================================

-- This trigger prevents users from logging in if their profile was deleted
-- but auth record still exists (shouldn't happen with our function, but safety net)

CREATE OR REPLACE FUNCTION public.check_user_profile_exists()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    -- Check if user profile exists
    IF NOT EXISTS (
        SELECT 1 FROM public.users WHERE auth_id = NEW.id
    ) THEN
        -- If profile doesn't exist, delete the auth user
        DELETE FROM auth.users WHERE id = NEW.id;
        RAISE EXCEPTION 'Account has been deleted. Please sign up again if needed.';
    END IF;
    
    RETURN NEW;
END;
$$;

-- Note: This trigger would run on auth.users which we can't directly modify
-- Instead, we'll handle this in the application code

-- ============================================
-- TESTING QUERIES
-- ============================================

-- View all deleted accounts (for officials)
-- SELECT * FROM public.deleted_accounts_view;

-- Check if email is available
-- SELECT public.is_email_available('test@gmail.com');

-- Test account deletion (run as authenticated user)
-- SELECT public.delete_user_account('Testing account deletion');

-- ============================================
-- CLEANUP OLD ORPHANED AUTH RECORDS
-- ============================================

-- Run this periodically to clean up any auth.users without profiles
-- (This should be run by an admin/cron job)

CREATE OR REPLACE FUNCTION public.cleanup_orphaned_auth_users()
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_deleted_count INTEGER;
BEGIN
    -- Delete auth users that don't have a profile
    WITH deleted AS (
        DELETE FROM auth.users
        WHERE id NOT IN (SELECT auth_id FROM public.users)
        AND created_at < NOW() - INTERVAL '1 day'  -- Only delete if older than 1 day
        RETURNING id
    )
    SELECT COUNT(*) INTO v_deleted_count FROM deleted;

    RETURN json_build_object(
        'success', true,
        'deleted_count', v_deleted_count,
        'message', format('Cleaned up %s orphaned auth records', v_deleted_count)
    );
END;
$$;

-- This should only be executable by service_role or admin
-- GRANT EXECUTE ON FUNCTION public.cleanup_orphaned_auth_users() TO service_role;

-- ============================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================

COMMENT ON TABLE public.deleted_accounts IS 
'Logs all deleted user accounts for audit purposes. Officials can view this to track account deletions.';

COMMENT ON FUNCTION public.delete_user_account(TEXT) IS 
'Completely deletes a user account: logs deletion, removes profile, and deletes auth user. Email becomes available for reuse.';

COMMENT ON FUNCTION public.is_email_available(TEXT) IS 
'Checks if an email address is available for registration (not in auth.users).';

COMMENT ON VIEW public.deleted_accounts_view IS 
'View for officials to see all deleted accounts with calculated days since deletion.';
