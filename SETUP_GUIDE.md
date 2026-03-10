# CommUnity Setup Guide

## Prerequisites

### Required Software
- **Android Studio**: Arctic Fox or later
- **JDK**: Version 8 or higher
- **Kotlin**: 1.9.22 or later
- **Gradle**: 8.0 or later
- **Android SDK**: API 24 (Android 7.0) minimum, API 34 target

### Supabase Account
1. Create account at [supabase.com](https://supabase.com)
2. Create a new project
3. Note your project URL and anon key

## Project Setup

### 1. Clone Repository

```bash
git clone https://github.com/yourusername/CommUnity.git
cd CommUnity
```

### 2. Configure Supabase

Create or update `local.properties`:
```properties
sdk.dir=/path/to/Android/sdk
supabase.url=YOUR_SUPABASE_PROJECT_URL
supabase.key=YOUR_SUPABASE_ANON_KEY
```

Update `CommUnityApplication.kt`:
```kotlin
val supabase = createSupabaseClient(
    supabaseUrl = "YOUR_SUPABASE_URL",
    supabaseKey = "YOUR_SUPABASE_ANON_KEY"
) {
    install(Auth)
    install(Postgrest)
    install(Storage)
}
```

### 3. Database Setup

Run these SQL commands in Supabase SQL Editor:

#### Create Users Table
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
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Add constraints
ALTER TABLE users
ADD CONSTRAINT check_first_name_valid 
CHECK (
    first_name ~ '^[A-Za-z]{2,}$' AND
    first_name !~ '\s'
);

ALTER TABLE users
ADD CONSTRAINT check_last_name_valid 
CHECK (
    last_name ~ '^[A-Za-z]{2,}$' AND
    last_name !~ '\s'
);
```

#### Create Reports Table
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
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Add index for performance
CREATE INDEX idx_reports_user_id ON reports(user_id);
CREATE INDEX idx_reports_status ON reports(status);
```

#### Enable Row Level Security
```sql
-- Enable RLS
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE reports ENABLE ROW LEVEL SECURITY;

-- Users policies
CREATE POLICY "Allow public insert during signup"
ON users FOR INSERT
TO anon, authenticated
WITH CHECK (true);

CREATE POLICY "Users can read own profile"
ON users FOR SELECT
TO authenticated
USING (auth_id = auth.uid());

CREATE POLICY "Users can update own profile"
ON users FOR UPDATE
TO authenticated
USING (auth_id = auth.uid())
WITH CHECK (auth_id = auth.uid());

-- Reports policies
CREATE POLICY "Users can insert own reports"
ON reports FOR INSERT
TO authenticated
WITH CHECK (user_id IN (
    SELECT id FROM users WHERE auth_id = auth.uid()
));

CREATE POLICY "Users can read own reports"
ON reports FOR SELECT
TO authenticated
USING (user_id IN (
    SELECT id FROM users WHERE auth_id = auth.uid()
));
```

### 4. Storage Setup

Create storage bucket in Supabase:

1. Go to Storage in Supabase dashboard
2. Create new bucket: `report-images`
3. Set as public bucket
4. Configure policies:

```sql
-- Allow authenticated users to upload
CREATE POLICY "Users can upload images"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (bucket_id = 'report-images');

-- Allow public read access
CREATE POLICY "Public can view images"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'report-images');
```

### 5. Email Configuration

Configure email templates in Supabase:

1. Go to Authentication → Email Templates
2. Customize templates:
   - Confirm signup
   - Reset password
   - Magic link

Example confirmation email:
```html
<h2>Welcome to CommUnity!</h2>
<p>Please confirm your email address by entering this code:</p>
<h1>{{ .Token }}</h1>
<p>This code expires in 24 hours.</p>
```

### 6. Build Project

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

### 7. Run on Device/Emulator

1. Connect Android device or start emulator
2. Click "Run" in Android Studio
3. Or use command line:
```bash
./gradlew installDebug
```

## Configuration Files

### build.gradle.kts (Project)
```kotlin
plugins {
    id("com.android.application") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
}
```

### build.gradle.kts (App)
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.communitys"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.communitys"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    
    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:2.0.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:gotrue-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    
    // Ktor
    implementation("io.ktor:ktor-client-android:2.3.7")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
```

## Testing Setup

### Unit Tests
```kotlin
// app/src/test/java/com/example/communitys/
class ValidationHelperTest {
    @Test
    fun `validateEmail with valid email returns success`() {
        val result = ValidationHelper.validateEmail("test@gmail.com")
        assertTrue(result is ValidationHelper.ValidationResult.Success)
    }
}
```

### Instrumented Tests
```kotlin
// app/src/androidTest/java/com/example/communitys/
@RunWith(AndroidJUnit4::class)
class LoginActivityTest {
    @Test
    fun testLoginButton() {
        // Test implementation
    }
}
```

## Troubleshooting

### Common Issues

#### 1. Build Fails - Java Version
```
Error: Unsupported Java version
```
**Solution**: Set Java 11 or higher in Android Studio:
- File → Project Structure → SDK Location → JDK location

#### 2. Supabase Connection Error
```
Error: Unable to connect to Supabase
```
**Solution**: 
- Check internet connection
- Verify Supabase URL and key
- Check Supabase project status

#### 3. ViewBinding Not Found
```
Error: Unresolved reference: databinding
```
**Solution**: 
- Sync Gradle files
- Clean and rebuild project
- Invalidate caches and restart

#### 4. Coroutines Error
```
Error: Suspend function should be called only from a coroutine
```
**Solution**: Use `viewModelScope.launch { }` or `lifecycleScope.launch { }`

#### 5. RLS Policy Blocking Insert
```
Error: new row violates row-level security policy
```
**Solution**: 
- Check RLS policies in Supabase
- Verify user authentication
- Check policy conditions

## Development Workflow

### 1. Feature Development
```bash
# Create feature branch
git checkout -b feature/new-feature

# Make changes
# Test locally

# Commit changes
git add .
git commit -m "Add new feature"

# Push to remote
git push origin feature/new-feature
```

### 2. Code Review
- Create pull request
- Review code changes
- Run tests
- Merge to main

### 3. Release Process
```bash
# Update version in build.gradle.kts
versionCode = 2
versionName = "1.1"

# Build release APK
./gradlew assembleRelease

# Sign APK
# Upload to Play Store
```

## Environment Variables

### Development
```properties
# local.properties
supabase.url=https://dev-project.supabase.co
supabase.key=dev-anon-key
```

### Production
```properties
# local.properties
supabase.url=https://prod-project.supabase.co
supabase.key=prod-anon-key
```

## Security Checklist

- [ ] Supabase keys not committed to Git
- [ ] RLS policies enabled on all tables
- [ ] Input validation on all forms
- [ ] HTTPS only for API calls
- [ ] Secure storage for sensitive data
- [ ] ProGuard enabled for release builds
- [ ] No sensitive data in logs

## Next Steps

1. Complete backend integration
2. Add image upload functionality
3. Implement push notifications
4. Add offline support
5. Create admin panel
6. Write comprehensive tests
7. Optimize performance
8. Prepare for Play Store release
