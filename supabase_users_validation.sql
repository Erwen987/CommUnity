-- ============================================
-- SUPABASE USERS TABLE VALIDATION POLICIES
-- ============================================

-- First, let's add CHECK constraints to the users table
-- These will enforce validation at the database level

-- Add constraint for first_name: only letters, no spaces, min 2 characters
ALTER TABLE public.users
ADD CONSTRAINT check_first_name_valid 
CHECK (
    first_name ~ '^[A-Za-z]{2,}$' AND
    first_name !~ '\s' AND
    LENGTH(TRIM(first_name)) >= 2
);

-- Add constraint for last_name: only letters, no spaces, min 2 characters
ALTER TABLE public.users
ADD CONSTRAINT check_last_name_valid 
CHECK (
    last_name ~ '^[A-Za-z]{2,}$' AND
    last_name !~ '\s' AND
    LENGTH(TRIM(last_name)) >= 2
);

-- Add constraint for email: must be valid format
ALTER TABLE public.users
ADD CONSTRAINT check_email_valid 
CHECK (
    email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$' AND
    email !~ '^\s' AND
    email !~ '\s$'
);

-- ============================================
-- ROW LEVEL SECURITY POLICIES
-- ============================================

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Allow public insert during signup" ON public.users;
DROP POLICY IF EXISTS "Users can read own profile" ON public.users;
DROP POLICY IF EXISTS "Users can update own profile" ON public.users;

-- Allow anyone to insert their own user profile during signup
-- WITH CHECK ensures data meets validation rules
CREATE POLICY "Allow public insert during signup"
ON public.users
FOR INSERT
TO anon, authenticated
WITH CHECK (
    -- First name validation
    first_name ~ '^[A-Za-z]{2,}$' AND
    first_name !~ '\s' AND
    LENGTH(TRIM(first_name)) >= 2 AND
    
    -- Last name validation
    last_name ~ '^[A-Za-z]{2,}$' AND
    last_name !~ '\s' AND
    LENGTH(TRIM(last_name)) >= 2 AND
    
    -- Email validation
    email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$' AND
    email !~ '^\s' AND
    email !~ '\s$' AND
    
    -- Barangay must not be empty
    LENGTH(TRIM(barangay)) > 0
);

-- Allow users to read their own profile
CREATE POLICY "Users can read own profile"
ON public.users
FOR SELECT
TO authenticated
USING (auth_id = auth.uid());

-- Allow users to update their own profile with validation
CREATE POLICY "Users can update own profile"
ON public.users
FOR UPDATE
TO authenticated
USING (auth_id = auth.uid())
WITH CHECK (
    auth_id = auth.uid() AND
    
    -- First name validation
    first_name ~ '^[A-Za-z]{2,}$' AND
    first_name !~ '\s' AND
    LENGTH(TRIM(first_name)) >= 2 AND
    
    -- Last name validation
    last_name ~ '^[A-Za-z]{2,}$' AND
    last_name !~ '\s' AND
    LENGTH(TRIM(last_name)) >= 2 AND
    
    -- Email validation
    email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$' AND
    email !~ '^\s' AND
    email !~ '\s$'
);

-- ============================================
-- VALIDATION FUNCTION (Optional but recommended)
-- ============================================

-- Create a function to validate user data before insert/update
CREATE OR REPLACE FUNCTION validate_user_data()
RETURNS TRIGGER AS $$
BEGIN
    -- Validate first name
    IF NEW.first_name !~ '^[A-Za-z]{2,}$' OR NEW.first_name ~ '\s' THEN
        RAISE EXCEPTION 'First name can only contain letters (A-Z) with no spaces, minimum 2 characters';
    END IF;
    
    -- Validate last name
    IF NEW.last_name !~ '^[A-Za-z]{2,}$' OR NEW.last_name ~ '\s' THEN
        RAISE EXCEPTION 'Last name can only contain letters (A-Z) with no spaces, minimum 2 characters';
    END IF;
    
    -- Validate email format
    IF NEW.email !~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$' THEN
        RAISE EXCEPTION 'Invalid email format. Example: user@gmail.com';
    END IF;
    
    -- Check for leading/trailing spaces in email
    IF NEW.email ~ '^\s' OR NEW.email ~ '\s$' THEN
        RAISE EXCEPTION 'Email cannot have leading or trailing spaces';
    END IF;
    
    -- Validate barangay
    IF LENGTH(TRIM(NEW.barangay)) = 0 THEN
        RAISE EXCEPTION 'Barangay is required';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to run validation function
DROP TRIGGER IF EXISTS validate_user_data_trigger ON public.users;
CREATE TRIGGER validate_user_data_trigger
    BEFORE INSERT OR UPDATE ON public.users
    FOR EACH ROW
    EXECUTE FUNCTION validate_user_data();

-- ============================================
-- PASSWORD VALIDATION (Supabase Auth Level)
-- ============================================

-- Note: Password validation happens at Supabase Auth level, not in the users table
-- You need to configure this in Supabase Dashboard or via SQL:

-- Create a function to validate password during signup
CREATE OR REPLACE FUNCTION validate_password(password TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    -- Password must be at least 8 characters
    IF LENGTH(password) < 8 THEN
        RAISE EXCEPTION 'Password must be at least 8 characters long';
    END IF;
    
    -- Password cannot contain only spaces
    IF password ~ '^\s+$' THEN
        RAISE EXCEPTION 'Password cannot contain only spaces';
    END IF;
    
    -- Password cannot have leading or trailing spaces
    IF password ~ '^\s' OR password ~ '\s$' THEN
        RAISE EXCEPTION 'Password cannot start or end with spaces';
    END IF;
    
    -- Password must contain at least 4-5 letters
    IF (SELECT COUNT(*) FROM regexp_matches(password, '[A-Za-z]', 'g')) < 4 THEN
        RAISE EXCEPTION 'Password must contain at least 4 letters';
    END IF;
    
    -- Password must contain at least one number
    IF password !~ '[0-9]' THEN
        RAISE EXCEPTION 'Password must contain at least one number';
    END IF;
    
    -- Password cannot be only dots and letters (like ".....kkk9")
    -- Must have a good mix of characters
    IF password ~ '^[.]+[A-Za-z]+[0-9]$' THEN
        RAISE EXCEPTION 'Password format is too simple. Use a mix of letters and numbers';
    END IF;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================

COMMENT ON CONSTRAINT check_first_name_valid ON public.users IS 
'First name must contain only letters (A-Z), no spaces, minimum 2 characters';

COMMENT ON CONSTRAINT check_last_name_valid ON public.users IS 
'Last name must contain only letters (A-Z), no spaces, minimum 2 characters';

COMMENT ON CONSTRAINT check_email_valid ON public.users IS 
'Email must be valid format with no leading/trailing spaces';

-- ============================================
-- TESTING QUERIES (Run these to test validation)
-- ============================================

-- These should FAIL:
-- INSERT INTO public.users (auth_id, first_name, last_name, email, barangay) 
-- VALUES (gen_random_uuid(), '  jk', 'Doe', 'test@gmail.com', 'Barangay 1');

-- INSERT INTO public.users (auth_id, first_name, last_name, email, barangay) 
-- VALUES (gen_random_uuid(), 'i.', 'Doe', 'test@gmail.com', 'Barangay 1');

-- INSERT INTO public.users (auth_id, first_name, last_name, email, barangay) 
-- VALUES (gen_random_uuid(), 'John', 'Doe', '@example.com', 'Barangay 1');

-- These should SUCCEED:
-- INSERT INTO public.users (auth_id, first_name, last_name, email, barangay) 
-- VALUES (gen_random_uuid(), 'John', 'Doe', 'john@gmail.com', 'Barangay 1');

-- INSERT INTO public.users (auth_id, first_name, last_name, email, barangay) 
-- VALUES (gen_random_uuid(), 'Maria', 'Santos', 'maria@yahoo.com', 'Barangay 2');
