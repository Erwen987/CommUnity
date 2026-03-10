# CommUnity API Documentation

## Supabase Backend Integration

### Base Configuration

```kotlin
// CommUnityApplication.kt
val supabase = createSupabaseClient(
    supabaseUrl = "YOUR_SUPABASE_URL",
    supabaseKey = "YOUR_SUPABASE_ANON_KEY"
) {
    install(Auth)
    install(Postgrest)
    install(Storage)
}
```

## Authentication API

### 1. Sign Up

**Endpoint**: Supabase Auth - `signUpWith(Email)`

**Request:**
```kotlin
suspend fun signUp(
    email: String,
    password: String,
    firstName: String,
    lastName: String,
    barangay: String
): Result<String>
```

**Validation Rules:**
- **First Name**: 
  - Only letters (A-Z)
  - No spaces or special characters
  - Minimum 2 characters
  
- **Last Name**: 
  - Only letters (A-Z)
  - No spaces or special characters
  - Minimum 2 characters

- **Email**: 
  - Valid email format
  - Must be from: gmail.com, yahoo.com, outlook.com, hotmail.com
  - No leading/trailing spaces

- **Password**: 
  - Minimum 8 characters
  - At least 4 letters
  - At least 1 number
  - No spaces

**Response:**
```kotlin
Result.success("Registration successful! Please check your email to verify your account.")
// OR
Result.failure(Exception("Error message"))
```

**Database Operation:**
After auth user creation, inserts into `users` table:
```sql
INSERT INTO users (auth_id, email, first_name, last_name, barangay)
VALUES (uuid, email, firstName, lastName, barangay)
```

### 2. Sign In

**Endpoint**: Supabase Auth - `signInWith(Email)`

**Request:**
```kotlin
suspend fun signIn(
    email: String,
    password: String
): Result<Unit>
```

**Response:**
```kotlin
Result.success(Unit)
// OR
Result.failure(Exception("Invalid email or password"))
```

### 3. Email Verification

**Endpoint**: Supabase Auth - `verifyEmailOtp()`

**Request:**
```kotlin
suspend fun verifyEmail(
    email: String,
    otp: String
): Result<Unit>
```

**OTP Validation:**
- Must be exactly 6 digits
- Only numbers allowed

**Response:**
```kotlin
Result.success(Unit)
// OR
Result.failure(Exception("Invalid OTP code"))
```

### 4. Resend OTP

**Endpoint**: Supabase Auth - `resendEmail()`

**Request:**
```kotlin
suspend fun resendOTP(email: String): Result<Unit>
```

### 5. Reset Password

**Endpoint**: Supabase Auth - `resetPasswordForEmail()`

**Request:**
```kotlin
suspend fun resetPassword(email: String): Result<Unit>
```

### 6. Sign Out

**Endpoint**: Supabase Auth - `signOut()`

**Request:**
```kotlin
suspend fun logout(): Result<Unit>
```

### 7. Get Current User

**Endpoint**: Supabase Postgrest - `users` table

**Request:**
```kotlin
suspend fun getCurrentUser(): Result<UserModel>
```

**Query:**
```sql
SELECT * FROM users WHERE auth_id = current_user_id
```

**Response:**
```kotlin
data class UserModel(
    val id: String,
    val authId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val barangay: String,
    val createdAt: String
)
```

## Reports API

### 1. Submit Report

**Endpoint**: Supabase Postgrest - `reports` table

**Request:**
```kotlin
suspend fun submitReport(
    category: String,
    description: String,
    location: String,
    imageUrl: String?
): Result<Unit>
```

**Validation:**
- **Category**: Required, not empty
- **Description**: Minimum 10 characters
- **Location**: Required, not empty
- **Image**: Optional

**Database Insert:**
```sql
INSERT INTO reports (category, description, location, image_url, status, user_id)
VALUES (category, description, location, imageUrl, 'pending', current_user_id)
```

### 2. Get User Reports

**Endpoint**: Supabase Postgrest - `reports` table

**Request:**
```kotlin
suspend fun getUserReports(userId: String): Result<List<ReportModel>>
```

**Query:**
```sql
SELECT * FROM reports 
WHERE user_id = userId 
ORDER BY created_at DESC
```

**Response:**
```kotlin
data class ReportModel(
    val id: String,
    val userId: String,
    val category: String,
    val description: String,
    val location: String,
    val imageUrl: String?,
    val status: String, // pending, in_progress, resolved
    val createdAt: String
)
```

### 3. Get All Reports

**Endpoint**: Supabase Postgrest - `reports` table

**Request:**
```kotlin
suspend fun getAllReports(): Result<List<ReportModel>>
```

**Query:**
```sql
SELECT * FROM reports ORDER BY created_at DESC
```

### 4. Update Report Status

**Endpoint**: Supabase Postgrest - `reports` table

**Request:**
```kotlin
suspend fun updateReportStatus(
    reportId: String,
    status: String
): Result<Unit>
```

**Query:**
```sql
UPDATE reports 
SET status = status 
WHERE id = reportId
```

**Valid Status Values:**
- `pending`
- `in_progress`
- `resolved`
- `rejected`

## Storage API

### Upload Image

**Endpoint**: Supabase Storage - `report-images` bucket

**Request:**
```kotlin
suspend fun uploadImage(
    file: ByteArray,
    fileName: String
): Result<String>
```

**Process:**
1. Upload file to storage bucket
2. Get public URL
3. Return URL for database storage

**Example:**
```kotlin
val imageUrl = supabase.storage
    .from("report-images")
    .upload("$userId/$fileName", file)
    .let { 
        supabase.storage
            .from("report-images")
            .publicUrl("$userId/$fileName")
    }
```

## Database Schema

### Users Table

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    auth_id UUID REFERENCES auth.users(id) UNIQUE NOT NULL,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    barangay VARCHAR(255) NOT NULL,
    points INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT check_first_name_valid CHECK (
        first_name ~ '^[A-Za-z]{2,}$' AND
        first_name !~ '\s'
    ),
    CONSTRAINT check_last_name_valid CHECK (
        last_name ~ '^[A-Za-z]{2,}$' AND
        last_name !~ '\s'
    ),
    CONSTRAINT check_email_valid CHECK (
        email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
    )
);
```

### Reports Table

```sql
CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(255) NOT NULL,
    image_url TEXT,
    status VARCHAR(50) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT check_status_valid CHECK (
        status IN ('pending', 'in_progress', 'resolved', 'rejected')
    )
);
```

### Documents Table (Planned)

```sql
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    purpose TEXT,
    status VARCHAR(50) DEFAULT 'processing',
    requested_at TIMESTAMP DEFAULT NOW(),
    released_at TIMESTAMP,
    
    CONSTRAINT check_status_valid CHECK (
        status IN ('processing', 'released', 'rejected')
    )
);
```

### Rewards Table (Planned)

```sql
CREATE TABLE rewards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    points_cost INTEGER NOT NULL,
    available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE reward_claims (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) NOT NULL,
    reward_id UUID REFERENCES rewards(id) NOT NULL,
    points_spent INTEGER NOT NULL,
    claimed_at TIMESTAMP DEFAULT NOW()
);
```

## Row Level Security (RLS) Policies

### Users Table

```sql
-- Allow public insert during signup
CREATE POLICY "Allow public insert during signup"
ON users FOR INSERT
TO anon, authenticated
WITH CHECK (
    first_name ~ '^[A-Za-z]{2,}$' AND
    last_name ~ '^[A-Za-z]{2,}$' AND
    email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
);

-- Users can read own profile
CREATE POLICY "Users can read own profile"
ON users FOR SELECT
TO authenticated
USING (auth_id = auth.uid());

-- Users can update own profile
CREATE POLICY "Users can update own profile"
ON users FOR UPDATE
TO authenticated
USING (auth_id = auth.uid())
WITH CHECK (auth_id = auth.uid());
```

### Reports Table

```sql
-- Users can insert own reports
CREATE POLICY "Users can insert own reports"
ON reports FOR INSERT
TO authenticated
WITH CHECK (user_id IN (
    SELECT id FROM users WHERE auth_id = auth.uid()
));

-- Users can read own reports
CREATE POLICY "Users can read own reports"
ON reports FOR SELECT
TO authenticated
USING (user_id IN (
    SELECT id FROM users WHERE auth_id = auth.uid()
));

-- Officials can read all reports (future)
CREATE POLICY "Officials can read all reports"
ON reports FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM users 
        WHERE auth_id = auth.uid() 
        AND role = 'official'
    )
);
```

## Error Codes

### Authentication Errors
- `INVALID_CREDENTIALS` - Wrong email/password
- `EMAIL_NOT_CONFIRMED` - Email not verified
- `USER_ALREADY_EXISTS` - Email already registered
- `WEAK_PASSWORD` - Password doesn't meet requirements
- `INVALID_EMAIL` - Email format invalid

### Validation Errors
- `VALIDATION_ERROR` - Input validation failed
- `REQUIRED_FIELD` - Required field missing
- `INVALID_FORMAT` - Data format incorrect

### Server Errors
- `NETWORK_ERROR` - Connection failed
- `SERVER_ERROR` - Internal server error
- `UNAUTHORIZED` - Not authenticated
- `FORBIDDEN` - No permission

## Rate Limiting

Supabase implements rate limiting:
- **Auth endpoints**: 30 requests per hour per IP
- **API endpoints**: 100 requests per second per project
- **Storage uploads**: 100 MB per request

## Best Practices

1. **Always validate input** before API calls
2. **Handle errors gracefully** with user-friendly messages
3. **Use coroutines** for async operations
4. **Cache data** when appropriate
5. **Implement retry logic** for network failures
6. **Log errors** for debugging
7. **Secure sensitive data** (never log passwords)
8. **Use RLS policies** for data security
