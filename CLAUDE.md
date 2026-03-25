# CommUnity — Android App (CLAUDE.md)

> This file describes the entire project so Claude can understand it without re-exploration.

---

## Project Overview

**CommUnity** is a resident-facing Android app for Dagupan City, Philippines that connects barangay (local government unit) residents with their officials. Residents can report infrastructure issues, request documents, earn reward points for civic participation, view announcements, and manage their accounts.

**Backend**: Supabase (Auth, Database, Storage, Realtime, Edge Functions)
**Language**: Kotlin, MVVM architecture, ViewBinding, Coroutines + LiveData
**Map**: Google Maps SDK + Google Play Services Location

---

## Architecture

```
view/           → Activities & Fragments (UI layer)
viewmodel/      → ViewModels (LiveData, state management)
model/
  data/         → Data classes (Kotlin @Serializable)
  repository/   → Supabase queries (suspend functions returning Result<T>)
utils/          → ValidationHelper
```

MVVM is strictly followed: Views observe LiveData from ViewModels; ViewModels call Repository methods.

---

## Folder Structure

```
app/src/main/java/com/example/communitys/
├── CommUnityApplication.kt         # Supabase client singleton init
├── SupabaseAuthHelper.kt           # Auth state helpers
├── SupabaseStorageHelper.kt        # Image upload helpers
├── model/
│   ├── data/
│   │   ├── UserModel.kt
│   │   ├── ReportModel.kt
│   │   ├── AnnouncementModel.kt
│   │   ├── RequestModel.kt
│   │   ├── RewardItemModel.kt
│   │   ├── ReportCategoryModel.kt
│   │   └── AppealModel.kt
│   └── repository/
│       ├── AuthRepository.kt
│       ├── ReportRepository.kt
│       ├── AnnouncementRepository.kt
│       └── RequestRepository.kt
├── view/
│   ├── MainContainerActivity.kt    # ViewPager2 + BottomNav host
│   ├── MainPagerAdapter.kt
│   ├── dashboard/
│   │   ├── DashboardFragment.kt
│   │   ├── AnnouncementAdapter.kt
│   │   └── AnnouncementDetailSheet.kt
│   ├── location/
│   │   ├── LocationFragment.kt
│   │   ├── LocationReportsAdapter.kt
│   │   ├── FullMapSheet.kt
│   │   └── AllReportsSheet.kt
│   ├── documents/
│   │   ├── DocumentsFragment.kt
│   │   ├── DocumentsAdapter.kt
│   │   ├── DocumentDetailSheet.kt
│   │   └── ReportDetailSheet.kt
│   ├── rewards/
│   │   ├── RewardsFragment.kt
│   │   └── RewardAdapter.kt
│   ├── profile/
│   │   ├── ProfileFragment.kt
│   │   ├── EditProfileActivity.kt
│   │   └── AvatarPickerSheet.kt
│   ├── login/
│   │   └── LoginActivity.kt
│   ├── signup/
│   │   └── SignUpActivity.kt
│   ├── welcome/
│   │   └── WelcomeActivity.kt
│   ├── verification/
│   │   └── VerifyEmailActivity.kt
│   ├── reportissue/
│   │   ├── ReportIssueActivity.kt
│   │   └── MapPickerActivity.kt
│   └── resetpassword/
│       └── ResetPasswordActivity.kt
├── viewmodel/
│   ├── AuthViewModel.kt
│   ├── DashboardViewModel.kt
│   ├── ProfileViewModel.kt
│   ├── LocationViewModel.kt
│   ├── DocumentsViewModel.kt
│   ├── ReportIssueViewModel.kt
│   └── RewardsViewModel.kt
└── utils/
    └── ValidationHelper.kt
```

---

## Navigation

```
LoginActivity
  → (success) WelcomeActivity
    → MainContainerActivity (ViewPager2, 5 tabs)
        [0] DashboardFragment   (nav_home)
        [1] LocationFragment    (nav_location)
        [2] DocumentsFragment   (nav_documents)
        [3] RewardsFragment     (nav_rewards)
        [4] ProfileFragment     (nav_profile)

Deep link: communitys://reset-password → ResetPasswordActivity
```

Activities launched via Intent (modals):
- `ReportIssueActivity` — from Dashboard "Report Issue"
- `RequestDocumentActivity` — from Dashboard "Request Document"
- `MapPickerActivity` — manual location selection in ReportIssueActivity
- `EditProfileActivity` — from ProfileFragment
- `ResetPasswordActivity` — email deep link

Bottom Sheets:
- `AnnouncementDetailSheet`, `DocumentDetailSheet`, `ReportDetailSheet`
- `AvatarPickerSheet`, `FullMapSheet`, `AllReportsSheet`

---

## User Flows

### Authentication
1. **Sign Up**: `SignUpActivity` → validates fields (name, email @gmail.com, phone, barangay, password, government ID image) → checks if barangay has approved officials → `AuthViewModel.signUp()` → `AuthRepository.signUp()` → creates Supabase auth user → navigates to `VerifyEmailActivity`
2. **OTP Verify**: `VerifyEmailActivity` → reads OTP + uploads ID image (ByteArray) → `AuthRepository.verifyEmail()` → uploads ID to `resident-ids` bucket → updates `id_image_url` in users table → signs out → shows pending approval toast → returns to Login
3. **Login**: `LoginActivity` → `AuthRepository.signIn()` → checks `status` (pending = blocked), `is_banned`, `suspended_until`, maintenance mode → navigates to `MainContainerActivity`
4. **Password Reset**: Forgot password dialog → `AuthRepository.resetPassword()` → email with deep link → `ResetPasswordActivity`

### Report Submission
1. Dashboard → "Report Issue" → `ReportIssueActivity`
2. Select category (loaded from `report_categories`), enter description, upload photo, capture GPS
3. Submit → `ReportRepository.createReport()` → `status='pending'`
4. If `category.points > 0` → auto-award points immediately

### Document Request
1. Dashboard → "Request Document" → `RequestDocumentActivity`
2. Select document type, purpose, payment method (GCash/pay_on_site)
3. If GCash: show QR code, require proof image upload
4. Submit → `RequestRepository.submitRequest()` → `status='pending'`

### Reward System
1. Earn points from reports → points accumulate in `users.points`
2. Profile → "Claim Reward Points" → moves `points` → `reward_points`
3. RewardsFragment → view items filtered by barangay → tap to redeem

---

## Database Tables (Supabase)

| Table | Key Columns |
|-------|-------------|
| **users** | id, auth_id, first_name, last_name, email, barangay, phone, points, reward_points, has_logged_in_before, avatar_url, is_banned, ban_reason, offense_count, suspended_until, **status** (active/pending/banned), **id_image_url** |
| **reports** | id, user_id, problem, description, image_url, location_lat, location_lng, status (pending/in_progress/resolved), created_at, points_awarded, barangay, rejection_reason |
| **report_categories** | id, name, points, sort_order |
| **announcements** | id, barangay, title, body, image_url, is_published, created_at, expires_at |
| **requests** | id, user_id, document_type, purpose, payment_method (gcash/pay_on_site), status (pending/ready_for_pickup/claimed/rejected), reference_number, created_at, proof_url, barangay, rejection_reason |
| **reward_items** | id, name, description, category, points_required, stock, is_active, barangay |
| **redemptions** | id, user_id, reward_item_id, points_spent, status (pending/completed) |
| **rewards** | id, user_id, points, reason |
| **appeals** | id, email, reason, status (pending/rejected/approved), created_at |
| **system_settings** | key, value |
| **deleted_accounts** | user_id, email, reason, deleted_at |
| **officials** | id, barangay, status (approved/pending), ... |

---

## Data Models

```kotlin
UserModel:
  id, authId, firstName, lastName, email, barangay, phone
  points, rewardPoints, hasLoggedInBefore, avatarUrl
  isBanned, banReason, offenseCount, suspendedUntil
  status: String? = "active"    // active | pending | banned
  idImageUrl: String? = null

ReportModel:
  id, userId, problem, description, imageUrl
  locationLat, locationLng
  status: String = "pending"    // pending | in_progress | resolved
  createdAt, pointsAwarded, barangay, rejectionReason

RequestModel:
  id, userId, documentType, purpose
  paymentMethod: String = "pay_on_site"   // gcash | pay_on_site
  status: String = "pending"             // pending | ready_for_pickup | claimed | rejected
  referenceNumber, createdAt, proofUrl, barangay, rejectionReason

AnnouncementModel: id, barangay, title, body, imageUrl, isPublished, createdAt, expiresAt
RewardItemModel: id, name, description, category, pointsRequired, stock, isActive, barangay
ReportCategoryModel: id, name, points, sortOrder
AppealModel: id, email, status (pending/rejected/approved)
```

---

## ViewModels & LiveData

```kotlin
AuthViewModel:
  loginState: LiveData<AuthState>          // Loading | Success | Error
  signupState: LiveData<AuthState>
  resetPasswordState: LiveData<AuthState>
  barangayCheckState: LiveData<Boolean?>   // null=checking, true=has officials, false=none
  fun checkBarangayOfficials(barangay: String)
  fun login(...), signUp(...), resetPassword(...)

DashboardViewModel:
  welcomeMessage, locationDate, reportsSubmitted, inProgress, resolved, pointsEarned
  announcements: LiveData<List<AnnouncementModel>>

ProfileViewModel:
  userProfile, logoutState, changePasswordState, deleteAccountState, avatarUpdateState, claimPointsState

LocationViewModel:
  reports: LiveData<List<ReportModel>>, isLoading, error, barangay

DocumentsViewModel:
  items: LiveData<List<DocumentItem>>, isLoading, error

ReportIssueViewModel:
  reportState: LiveData<ReportState>

RewardsViewModel:
  totalPoints, rewardItems, claimState
```

---

## AuthRepository Key Methods

```kotlin
suspend fun checkBarangayHasOfficials(barangay: String): Boolean
  // queries officials table for status='approved' in barangay; returns true on exception (fail open)

suspend fun signUp(email, password, firstName, lastName, barangay, phone): Result<String>
suspend fun verifyEmail(email, otp, firstName, lastName, barangay, idImageBytes: ByteArray?): Result<Unit>
  // idImageBytes: uploads to resident-ids/{userId}/id.jpg, updates users.id_image_url, then signs out

suspend fun signIn(email, password): Result<Unit>
  // checks: status=="pending" → throw pending message
  // checks: is_banned → throw ban message
  // checks: suspended_until → throw suspension message
  // checks: maintenance mode

suspend fun uploadResidentId(userId: String, imageBytes: ByteArray): Result<String>
  // uploads to resident-ids/{userId}/id.jpg

suspend fun updateResidentIdUrl(idImageUrl: String): Result<Unit>
  // updates users table id_image_url column
```

---

## Validation Rules (ValidationHelper)

- **Email**: must end in @gmail.com
- **Password**: min 8 chars, requires uppercase, lowercase, digit, special char (@#$!%*?&_-)
- **Name**: non-empty
- **Phone**: valid Philippine mobile format
- **Barangay**: must be in the hardcoded list of 33 Dagupan barangays

---

## Barangay List (33 barangays, Dagupan City)

Bacayao Norte, Bacayao Sur, Barangay I (Pob.), Barangay II (Pob.), Barangay III (Pob.), Barangay IV (Pob.), Bolosan, Bonuan Binloc, Bonuan Boquig, Bonuan Gueset, Calmay, Carael, Caranglaan, Herrero, Lasip Chico, Lasip Grande, Lomboy, Lucao, Malued, Mamalingling, Mangin, Mayombo, Pantal, Poblacion Oeste, Pogo Chico, Pogo Grande, Pugaro Suit, Quezon, San Jose, San Lázaro, Salapingao, Taloy, Tebeng

---

## Business Rules

1. **Email**: @gmail.com only
2. **Password**: 8+ chars, mixed case + digit + special char
3. **Active Report Limit**: max 5 unresolved reports per user
4. **Active Document Request Limit**: max 5 unresolved requests per user
5. **Auto-Point Awarding**: if `report_category.points > 0`, award immediately on submission
6. **Claim Reward Points**: moves from `users.points` → `users.reward_points`
7. **Barangay Scoping**: announcements, reward_items, document routing filtered by user's barangay
8. **Account Status**:
   - New sign-ups: `status='pending'` (set by DB trigger `handle_new_user`)
   - After official approval: `status='active'`
   - Banned: `is_banned=true`; suspended: `suspended_until` set to future date
9. **Maintenance Mode**: `system_settings` key `maintenance_mode='true'` blocks all logins
10. **Ban Appeals**: submitted via `appeals` table; admin reviews
11. **Account Deletion**: logged to `deleted_accounts`; email freed for re-registration
12. **Pending Block on Login**: `status=='pending'` throws specific message about awaiting approval
13. **ID Image Upload Timing**: uploaded AFTER OTP verification (user is briefly authenticated), BEFORE sign-out

---

## Supabase Storage Buckets

| Bucket | Purpose | Policies |
|--------|---------|----------|
| `avatars` | User profile photos | Authenticated users can insert/update |
| `report-images` | Photos attached to reports | Authenticated insert, public read |
| `document-proofs` | GCash payment proof images | Authenticated insert, public read |
| `resident-ids` | Government ID photos for sign-up | Authenticated insert (during verify flow), public read |

---

## Supabase Edge Functions Used

| Function | Purpose |
|----------|---------|
| `delete-auth-user` | Deletes a user from Supabase Auth (uses service role); called on account deletion and resident rejection |
| `send-email` | Sends transactional emails via Brevo API |

### `send-email` Types

| type | When |
|------|------|
| `resident_approved` | Official approves a pending resident |
| `resident_rejected` | Official rejects a pending resident |
| `revoked` | Admin revokes an official's account |

---

## Android Permissions Required

```xml
INTERNET, ACCESS_NETWORK_STATE
ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION   (GPS for reports)
READ_MEDIA_IMAGES (API 33+) / READ_EXTERNAL_STORAGE (older)
WRITE_EXTERNAL_STORAGE (legacy < SDK 32)
```

---

## Key Gradle Dependencies

- `io.github.jan-tennert.supabase:*` — gotrue-kt, postgrest-kt, storage-kt, realtime-kt
- `com.google.android.gms:play-services-maps` — Google Maps
- `com.google.android.gms:play-services-location` — GPS
- `io.coil-kt:coil` — image loading (caching disabled for fresh avatars)
- `com.github.yalantis:ucrop` — avatar cropping
- `com.google.android.material` — Material Design components
