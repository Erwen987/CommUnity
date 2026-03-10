# CommUnity Validation Rules

## Overview

This document outlines all validation rules implemented in the CommUnity application for data integrity and security.

## User Registration Validation

### First Name & Last Name

**Rules:**
- Only letters (A-Z, a-z) allowed
- No spaces, numbers, or special characters
- Minimum 2 characters
- Maximum 100 characters (database constraint)
- Leading and trailing spaces are trimmed

**Valid Examples:**
- ✅ "John"
- ✅ "Maria"
- ✅ "Jose"

**Invalid Examples:**
- ❌ "John123" (contains numbers)
- ❌ "John Doe" (contains space)
- ❌ "J." (contains special character)
- ❌ "  John" (leading spaces)
- ❌ "J" (too short)
- ❌ "i." (too short and has special character)

**Error Messages:**
- Empty: "{Field} is required"
- Too short: "{Field} must be at least 2 letters"
- Contains numbers: "{Field} cannot contain numbers"
- Contains spaces: "{Field} cannot contain spaces"
- Special characters: "{Field} can only contain letters (A-Z)"
- Only dots/spaces: "{Field} must contain actual letters"

**Implementation:**
```kotlin
fun validateName(name: String, fieldName: String): ValidationResult {
    val trimmedName = name.trim()
    
    if (trimmedName.isEmpty()) {
        return ValidationResult.Error("$fieldName is required")
    }
    
    if (trimmedName.all { it == '.' || it.isWhitespace() }) {
        return ValidationResult.Error("$fieldName must contain actual letters")
    }
    
    if (trimmedName.length < 2) {
        return ValidationResult.Error("$fieldName must be at least 2 letters")
    }
    
    if (trimmedName.any { it.isDigit() }) {
        return ValidationResult.Error("$fieldName cannot contain numbers")
    }
    
    if (trimmedName.contains(" ")) {
        return ValidationResult.Error("$fieldName cannot contain spaces")
    }
    
    if (!trimmedName.all { it.isLetter() }) {
        return ValidationResult.Error("$fieldName can only contain letters (A-Z)")
    }
    
    return ValidationResult.Success
}
```

### Email Address

**Rules:**
- Must be valid email format (RFC 5322)
- Must have username before @
- Must have valid domain after @
- Only specific domains allowed:
  - gmail.com
  - yahoo.com
  - outlook.com
  - hotmail.com
- No leading or trailing spaces
- Case insensitive

**Valid Examples:**
- ✅ "user@gmail.com"
- ✅ "john.doe@yahoo.com"
- ✅ "maria_santos@outlook.com"
- ✅ "test123@hotmail.com"

**Invalid Examples:**
- ❌ "@gmail.com" (no username)
- ❌ "user@" (no domain)
- ❌ "user@example.com" (invalid domain)
- ❌ "  user@gmail.com" (leading spaces)
- ❌ "user gmail.com" (missing @)
- ❌ "user@gmailcom" (missing dot)

**Error Messages:**
- Empty: "Email is required"
- No username: "Email must have a username before @"
- Invalid format: "Invalid email format. Example: user@gmail.com"
- No @ symbol: "Email must contain @ symbol"
- No domain: "Email must have a domain after @. Example: @gmail.com"
- Invalid domain: "Please use a valid email domain (gmail.com, yahoo.com, outlook.com, hotmail.com)"

**Implementation:**
```kotlin
fun validateEmail(email: String): ValidationResult {
    val trimmedEmail = email.trim()
    
    if (trimmedEmail.isEmpty()) {
        return ValidationResult.Error("Email is required")
    }
    
    if (trimmedEmail.startsWith("@")) {
        return ValidationResult.Error("Email must have a username before @")
    }
    
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
        return ValidationResult.Error("Invalid email format. Example: user@gmail.com")
    }
    
    if (!trimmedEmail.contains("@")) {
        return ValidationResult.Error("Email must contain @ symbol")
    }
    
    val domain = trimmedEmail.substringAfter("@", "")
    if (domain.isEmpty()) {
        return ValidationResult.Error("Email must have a domain after @. Example: @gmail.com")
    }
    
    val validDomains = listOf("gmail.com", "yahoo.com", "outlook.com", "hotmail.com")
    if (!validDomains.any { trimmedEmail.endsWith("@$it", ignoreCase = true) }) {
        return ValidationResult.Error("Please use a valid email domain (gmail.com, yahoo.com, outlook.com, hotmail.com)")
    }
    
    return ValidationResult.Success
}
```

### Password

**Rules:**
- Minimum 8 characters
- Must contain at least 4 letters
- Must contain at least 1 number
- No spaces allowed (leading, trailing, or anywhere)
- Cannot be only special characters
- Cannot start with many special characters (max 3)
- Cannot have more dots than letters

**Valid Examples:**
- ✅ "password123"
- ✅ "MyPass2024"
- ✅ "secure1234"
- ✅ "Test@123"

**Invalid Examples:**
- ❌ "pass123" (too short)
- ❌ "password" (no numbers)
- ❌ "12345678" (no letters)
- ❌ "pass 123" (contains space)
- ❌ "  password123" (leading spaces)
- ❌ ".....kkk9" (too many dots, not enough letters)
- ❌ "abc1" (less than 4 letters)

**Error Messages:**
- Empty: "Password is required"
- Leading/trailing spaces: "Password cannot start or end with spaces"
- Contains spaces: "Password cannot contain spaces"
- Only special chars: "Password must contain letters and numbers"
- Too short: "Password must be at least 8 characters long"
- Not enough letters: "Password must contain at least 4 letters"
- No numbers: "Password must contain at least one number"
- Too many dots: "Password format is too simple. Use more letters than symbols"
- Starts with special chars: "Password cannot start with many special characters"

**Implementation:**
```kotlin
fun validatePassword(password: String): ValidationResult {
    if (password.isEmpty()) {
        return ValidationResult.Error("Password is required")
    }
    
    if (password != password.trim()) {
        return ValidationResult.Error("Password cannot start or end with spaces")
    }
    
    if (password.contains(" ") || password.any { it.isWhitespace() }) {
        return ValidationResult.Error("Password cannot contain spaces")
    }
    
    if (password.all { !it.isLetterOrDigit() }) {
        return ValidationResult.Error("Password must contain letters and numbers")
    }
    
    if (password.length < 8) {
        return ValidationResult.Error("Password must be at least 8 characters long")
    }
    
    val letterCount = password.count { it.isLetter() }
    if (letterCount < 4) {
        return ValidationResult.Error("Password must contain at least 4 letters")
    }
    
    if (!password.any { it.isDigit() }) {
        return ValidationResult.Error("Password must contain at least one number")
    }
    
    val dotCount = password.count { it == '.' }
    if (dotCount > letterCount) {
        return ValidationResult.Error("Password format is too simple. Use more letters than symbols")
    }
    
    val startsWithSpecialChars = password.takeWhile { !it.isLetterOrDigit() }
    if (startsWithSpecialChars.length > 3) {
        return ValidationResult.Error("Password cannot start with many special characters")
    }
    
    return ValidationResult.Success
}
```

### Confirm Password

**Rules:**
- Must not be empty
- Must match the password field exactly

**Error Messages:**
- Empty: "Please confirm your password"
- Mismatch: "Passwords do not match"

### Barangay Selection

**Rules:**
- Must not be empty
- Must be selected from dropdown

**Error Messages:**
- Empty: "Please select a barangay"

**Available Options:**
- Barangay 1
- Barangay 2
- Barangay 3
- Barangay 4
- Barangay 5
- San Isidro
- Santa Rosa
- San Miguel
- Santo Domingo

## Login Validation

### Email
Same rules as registration email validation

### Password
Same rules as registration password validation

## OTP Verification

**Rules:**
- Must be exactly 6 digits
- Only numbers allowed
- No spaces

**Valid Examples:**
- ✅ "123456"
- ✅ "000000"
- ✅ "999999"

**Invalid Examples:**
- ❌ "12345" (too short)
- ❌ "1234567" (too long)
- ❌ "12 34 56" (contains spaces)
- ❌ "abcdef" (contains letters)

**Error Messages:**
- Empty: "OTP is required"
- Wrong length: "OTP must be 6 digits"
- Non-numeric: "OTP must contain only numbers"

**Implementation:**
```kotlin
fun validateOTP(otp: String): ValidationResult {
    if (otp.isEmpty()) {
        return ValidationResult.Error("OTP is required")
    }
    
    if (otp.length != 6) {
        return ValidationResult.Error("OTP must be 6 digits")
    }
    
    if (!otp.all { it.isDigit() }) {
        return ValidationResult.Error("OTP must contain only numbers")
    }
    
    return ValidationResult.Success
}
```

## Report Issue Validation

### Category

**Rules:**
- Must not be empty
- Must be selected from predefined list

**Error Messages:**
- Empty: "Please select a category"

**Available Categories:**
- Infrastructure
- Sanitation
- Safety
- Environment
- Others

### Description

**Rules:**
- Minimum 10 characters
- Maximum 1000 characters
- Cannot be only spaces

**Error Messages:**
- Empty: "Description is required"
- Too short: "Description must be at least 10 characters"

### Location

**Rules:**
- Must not be empty
- Cannot be only spaces

**Error Messages:**
- Empty: "Location is required"

## Database-Level Validation

### SQL Constraints

**Users Table:**
```sql
-- First name validation
CONSTRAINT check_first_name_valid CHECK (
    first_name ~ '^[A-Za-z]{2,}$' AND
    first_name !~ '\s' AND
    LENGTH(TRIM(first_name)) >= 2
)

-- Last name validation
CONSTRAINT check_last_name_valid CHECK (
    last_name ~ '^[A-Za-z]{2,}$' AND
    last_name !~ '\s' AND
    LENGTH(TRIM(last_name)) >= 2
)

-- Email validation
CONSTRAINT check_email_valid CHECK (
    email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$' AND
    email !~ '^\s' AND
    email !~ '\s$'
)
```

**Reports Table:**
```sql
-- Status validation
CONSTRAINT check_status_valid CHECK (
    status IN ('pending', 'in_progress', 'resolved', 'rejected')
)
```

## Validation Flow

```
User Input
    ↓
Client-Side Validation (Kotlin)
    ↓ (if valid)
API Call to Supabase
    ↓
Database-Level Validation (SQL)
    ↓ (if valid)
Success Response
```

## Error Handling

### Client-Side
```kotlin
when (val result = ValidationHelper.validateEmail(email)) {
    is ValidationResult.Error -> {
        tilEmail.error = result.message
        etEmail.requestFocus()
    }
    is ValidationResult.Success -> {
        // Proceed with API call
    }
}
```

### Server-Side
```kotlin
result.onFailure { exception ->
    val errorMessage = when {
        exception.message?.contains("check_first_name_valid") == true ->
            "First name can only contain letters"
        exception.message?.contains("check_email_valid") == true ->
            "Invalid email format"
        else -> exception.message ?: "Validation failed"
    }
    showError(errorMessage)
}
```

## Testing Validation

### Unit Tests
```kotlin
@Test
fun `validateName with valid name returns success`() {
    val result = ValidationHelper.validateName("John", "First name")
    assertTrue(result is ValidationHelper.ValidationResult.Success)
}

@Test
fun `validateName with numbers returns error`() {
    val result = ValidationHelper.validateName("John123", "First name")
    assertTrue(result is ValidationHelper.ValidationResult.Error)
    assertEquals("First name cannot contain numbers", (result as ValidationHelper.ValidationResult.Error).message)
}
```

## Best Practices

1. **Validate early**: Check input before API calls
2. **Clear errors**: Remove error messages when user starts typing
3. **Focus on error**: Move focus to first field with error
4. **User-friendly messages**: Explain what's wrong and how to fix it
5. **Consistent validation**: Same rules on client and server
6. **Trim input**: Remove leading/trailing spaces
7. **Case insensitive**: For emails and usernames
8. **Secure passwords**: Enforce strong password requirements
