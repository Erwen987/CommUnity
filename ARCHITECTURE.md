# CommUnity Architecture Documentation

## MVVM Architecture Pattern

The CommUnity app follows the **Model-View-ViewModel (MVVM)** architectural pattern for clean separation of concerns and maintainability.

### Architecture Layers

```
┌─────────────────────────────────────────┐
│              VIEW LAYER                  │
│  (Activities, Fragments, XML Layouts)   │
└─────────────┬───────────────────────────┘
              │ observes LiveData
              │ calls ViewModel methods
┌─────────────▼───────────────────────────┐
│          VIEWMODEL LAYER                 │
│  (Business Logic, State Management)     │
└─────────────┬───────────────────────────┘
              │ calls repository methods
              │ transforms data
┌─────────────▼───────────────────────────┐
│           MODEL LAYER                    │
│  (Repository, Data Sources, Models)     │
└─────────────┬───────────────────────────┘
              │ API calls
              │ Database operations
┌─────────────▼───────────────────────────┐
│          DATA SOURCES                    │
│  (Supabase, Local Storage)              │
└─────────────────────────────────────────┘
```

## Component Details

### 1. View Layer

**Responsibilities:**
- Display UI to users
- Capture user input
- Observe ViewModel LiveData
- Update UI based on state changes

**Components:**
- **Activities**: Container for fragments, handles navigation
  - `LoginActivity`
  - `SignUpActivity`
  - `WelcomeActivity`
  - `MainContainerActivity`
  - `ReportIssueActivity`
  - `RequestDocumentActivity`
  - `VerifyEmailActivity`

- **Fragments**: Reusable UI components
  - `DashboardFragment`
  - `LocationFragment`
  - `DocumentsFragment`
  - `RewardsFragment`
  - `ProfileFragment`

**Example:**
```kotlin
class LoginActivity : AppCompatActivity() {
    private lateinit var viewModel: AuthViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(AuthViewModel::class.java)
        
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupObservers() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> showLoading()
                is AuthViewModel.AuthState.Success -> navigateToHome()
                is AuthViewModel.AuthState.Error -> showError(state.message)
            }
        }
    }
}
```

### 2. ViewModel Layer

**Responsibilities:**
- Hold and manage UI-related data
- Handle business logic
- Validate user input
- Communicate with repositories
- Expose data via LiveData
- Survive configuration changes

**ViewModels:**
- `AuthViewModel` - Authentication operations
- `DashboardViewModel` - Dashboard state
- `ProfileViewModel` - User profile management
- `ReportIssueViewModel` - Issue reporting
- `DocumentsViewModel` - Document management
- `RewardsViewModel` - Rewards system
- `LocationViewModel` - Location reports

**Example:**
```kotlin
class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    
    private val _loginState = MutableLiveData<AuthState>()
    val loginState: LiveData<AuthState> = _loginState
    
    fun login(email: String, password: String) {
        // Validate input
        val validation = ValidationHelper.validateEmail(email)
        if (validation is ValidationResult.Error) {
            _loginState.value = AuthState.Error(validation.message)
            return
        }
        
        // Call repository
        _loginState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            _loginState.value = when {
                result.isSuccess -> AuthState.Success("Login successful")
                else -> AuthState.Error(result.exceptionOrNull()?.message ?: "Failed")
            }
        }
    }
    
    sealed class AuthState {
        object Loading : AuthState()
        data class Success(val message: String) : AuthState()
        data class Error(val message: String) : AuthState()
    }
}
```

### 3. Model Layer

**Responsibilities:**
- Manage data operations
- Abstract data sources
- Handle API calls
- Cache data
- Provide clean API to ViewModels

**Repositories:**
- `AuthRepository` - User authentication
- `ReportRepository` - Issue reports
- `DocumentRepository` - Document requests (planned)
- `RewardsRepository` - Rewards system (planned)

**Data Models:**
- `UserModel` - User information
- `ReportModel` - Issue report data
- `DocumentModel` - Document request data (planned)

**Example:**
```kotlin
class AuthRepository {
    private val supabase = CommUnityApplication.supabase
    
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## Data Flow

### User Action Flow
```
User Input → View → ViewModel → Repository → Supabase
                ↑                                  ↓
                └──────── LiveData Update ─────────┘
```

### Example: Login Flow
1. User enters email/password and clicks login
2. `LoginActivity` calls `viewModel.login(email, password)`
3. `AuthViewModel` validates input
4. `AuthViewModel` calls `authRepository.signIn()`
5. `AuthRepository` makes API call to Supabase
6. Result flows back through layers
7. `AuthViewModel` updates `loginState` LiveData
8. `LoginActivity` observes change and updates UI

## State Management

### LiveData Pattern
- **Immutable State**: ViewModels expose `LiveData<T>` (read-only)
- **Mutable State**: ViewModels use `MutableLiveData<T>` internally
- **Lifecycle Aware**: Automatically handles lifecycle events

### Sealed Classes for States
```kotlin
sealed class AuthState {
    object Loading : AuthState()
    data class Success(val message: String, val email: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}
```

Benefits:
- Type-safe state representation
- Exhaustive when expressions
- Clear state transitions

## Navigation Architecture

### ViewPager2 + BottomNavigation
```
MainContainerActivity
├── ViewPager2
│   ├── DashboardFragment (Page 0)
│   ├── LocationFragment (Page 1)
│   ├── DocumentsFragment (Page 2)
│   ├── RewardsFragment (Page 3)
│   └── ProfileFragment (Page 4)
└── BottomNavigationView
    ├── Home
    ├── Location
    ├── Documents
    ├── Rewards
    └── Profile
```

**Features:**
- Smooth page transitions
- Swipe navigation
- Fixed bottom navigation
- Synchronized state

## Dependency Injection

Currently using **Manual DI** (constructor injection):
```kotlin
class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()
}
```

**Future**: Consider Hilt/Dagger for:
- Better testability
- Singleton management
- Scoped dependencies

## Error Handling

### Repository Layer
```kotlin
suspend fun signIn(email: String, password: String): Result<Unit> {
    return try {
        // API call
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception("User-friendly message"))
    }
}
```

### ViewModel Layer
```kotlin
result.onSuccess {
    _state.value = State.Success()
}.onFailure { exception ->
    _state.value = State.Error(exception.message ?: "Unknown error")
}
```

### View Layer
```kotlin
when (state) {
    is State.Error -> {
        Toast.makeText(this, state.message, LENGTH_LONG).show()
    }
}
```

## Testing Strategy

### Unit Tests (Planned)
- ViewModel logic
- Repository operations
- Validation helpers

### Integration Tests (Planned)
- Repository + Supabase
- End-to-end flows

### UI Tests (Planned)
- Fragment navigation
- User interactions
- Form validation

## Performance Considerations

1. **Coroutines**: Async operations don't block UI
2. **ViewBinding**: Faster than findViewById
3. **LiveData**: Lifecycle-aware, prevents memory leaks
4. **ViewPager2**: Efficient fragment recycling
5. **Image Loading**: Lazy loading for profile photos

## Security Architecture

### Client-Side
- Input validation before API calls
- Secure storage for tokens
- No sensitive data in logs

### Server-Side (Supabase)
- Row Level Security (RLS) policies
- Database-level validation
- JWT-based authentication
- HTTPS only

## Scalability Considerations

1. **Modular Structure**: Easy to add new features
2. **Repository Pattern**: Easy to swap data sources
3. **MVVM**: Testable and maintainable
4. **Sealed Classes**: Type-safe state management
5. **Coroutines**: Efficient async operations
