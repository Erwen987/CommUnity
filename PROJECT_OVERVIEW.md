# CommUnity - Barangay Management System

## Project Overview

CommUnity is an Android mobile application designed to streamline communication and services between barangay residents and local government officials in the Philippines.

## Key Features

### 1. User Authentication
- Email-based registration with OTP verification
- Secure login with enhanced validation
- Password reset functionality
- Session management with Supabase Auth

### 2. Dashboard
- Welcome messages for users
- Quick access to report issues
- Quick access to request documents
- Real-time date and location display
- Notification center

### 3. Issue Reporting
- Report barangay issues (broken streetlights, potholes, etc.)
- Category selection
- Location tagging
- Photo upload capability
- Status tracking (Pending, In Progress, Resolved)

### 4. Document Requests
- Request barangay clearance
- Request certificates of residency
- Request business permits
- Track document status (Processing, Released)

### 5. Location/Reports View
- View all reported issues in the barangay
- Filter by status
- See issue details and locations

### 6. Rewards System
- Earn points for community participation
- Redeem rewards:
  - Prepaid load (100 points)
  - School supplies (300 points)
  - ₱500 groceries (500 points)
  - ₱3,000 cash (1500 points)

### 7. User Profile
- View personal information
- Track earned points
- View submission history
- Edit profile
- Logout functionality

## Technical Stack

### Frontend
- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI Components**: 
  - Material Design 3
  - ViewPager2 for smooth navigation
  - ViewBinding for type-safe view access
  - BottomNavigationView

### Backend
- **BaaS**: Supabase
  - Authentication (GoTrue)
  - Database (PostgreSQL)
  - Storage (for images)
  - Row Level Security (RLS)

### Libraries & Dependencies
- AndroidX Core KTX
- Lifecycle & LiveData
- Coroutines for async operations
- Ktor Client for networking
- Kotlinx Serialization
- CircleImageView for profile photos
- GridLayout for rewards display

## Project Structure

```
app/
├── src/main/
│   ├── java/com/example/communitys/
│   │   ├── model/
│   │   │   ├── data/          # Data models
│   │   │   └── repository/    # Data repositories
│   │   ├── view/              # Activities & Fragments
│   │   │   ├── dashboard/
│   │   │   ├── documents/
│   │   │   ├── location/
│   │   │   ├── login/
│   │   │   ├── profile/
│   │   │   ├── reportissue/
│   │   │   ├── rewards/
│   │   │   ├── signup/
│   │   │   ├── verification/
│   │   │   └── welcome/
│   │   ├── viewmodel/         # ViewModels
│   │   ├── utils/             # Utility classes
│   │   ├── CommUnityApplication.kt
│   │   ├── SupabaseAuthHelper.kt
│   │   └── SupabaseStorageHelper.kt
│   └── res/
│       ├── drawable/          # Images & icons
│       ├── layout/            # XML layouts
│       └── menu/              # Navigation menus
```

## Target Users

1. **Barangay Residents**
   - Report community issues
   - Request official documents
   - Track submissions
   - Earn and redeem rewards

2. **Barangay Officials** (Future)
   - View and manage reports
   - Process document requests
   - Update issue statuses
   - Manage rewards system

## Barangay Information

**Location**: Barangay Naguilayan, Binmaley, Pangasinan, Philippines

## Development Status

- ✅ Authentication system with validation
- ✅ MVVM architecture implementation
- ✅ Dashboard with quick actions
- ✅ Issue reporting system
- ✅ Document request system
- ✅ Rewards system UI
- ✅ Profile management
- ✅ Smooth navigation with ViewPager2
- 🔄 Backend integration (In Progress)
- 🔄 Image upload functionality
- 🔄 Real-time notifications
- 📋 Admin panel (Planned)

## Security Features

- Email validation (Gmail, Yahoo, Outlook, Hotmail only)
- Strong password requirements (8+ chars, 4+ letters, 1+ number)
- Name validation (letters only, no spaces)
- Database-level validation with RLS policies
- Secure session management
- Input sanitization

## Future Enhancements

1. Push notifications for status updates
2. In-app messaging with officials
3. Community announcements
4. Event calendar
5. Emergency alerts
6. Multi-language support (English, Filipino)
7. Offline mode with sync
8. Analytics dashboard for officials
9. QR code for document verification
10. Integration with other government services
