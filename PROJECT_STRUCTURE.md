# OralVis Jetpack Compose - Project Structure

## Complete File Tree

```
OralVisJetpack/
│
├── app/
│   ├── build.gradle.kts                    # App-level Gradle configuration
│   ├── proguard-rules.pro                  # ProGuard rules for release builds
│   │
│   └── src/
│       ├── androidTest/                    # Instrumented tests
│       │   └── java/com/chaitany/oralvisjetpack/
│       │
│       ├── main/
│       │   ├── AndroidManifest.xml         # App manifest with permissions
│       │   │
│       │   ├── java/com/chaitany/oralvisjetpack/
│       │   │   │
│       │   │   ├── data/                   # Data Layer
│       │   │   │   ├── dao/                # Database Access Objects
│       │   │   │   │   ├── ClinicDao.kt                    # Clinic CRUD operations
│       │   │   │   │   └── PatientCounterDao.kt            # Patient counter operations
│       │   │   │   │
│       │   │   │   ├── database/           # Database Configuration
│       │   │   │   │   └── OralVisDatabase.kt              # Room database setup
│       │   │   │   │
│       │   │   │   ├── model/              # Data Models
│       │   │   │   │   ├── Clinic.kt                       # Clinic entity
│       │   │   │   │   └── PatientCounter.kt               # Patient counter entity
│       │   │   │   │
│       │   │   │   └── repository/         # Repository Pattern
│       │   │   │       ├── ClinicRepository.kt             # Clinic data operations
│       │   │   │       └── PatientCounterRepository.kt     # Counter data operations
│       │   │   │
│       │   │   ├── navigation/             # Navigation System
│       │   │   │   ├── NavGraph.kt                         # Navigation graph definition
│       │   │   │   └── Screen.kt                           # Screen routes sealed class
│       │   │   │
│       │   │   ├── ui/                     # UI Layer
│       │   │   │   ├── screens/            # Compose Screens
│       │   │   │   │   ├── ClinicEntryScreen.kt            # Clinic registration screen
│       │   │   │   │   ├── WelcomeScreen.kt                # Welcome/home screen
│       │   │   │   │   ├── PatientEntryScreen.kt           # Patient data entry screen
│       │   │   │   │   └── ImageSequenceScreen.kt          # Image capture sequence screen
│       │   │   │   │
│       │   │   │   └── theme/              # App Theme
│       │   │   │       ├── Color.kt                        # Color palette
│       │   │   │       ├── Theme.kt                        # Material 3 theme
│       │   │   │       └── Type.kt                         # Typography definitions
│       │   │   │
│       │   │   ├── utils/                  # Utility Classes
│       │   │   │   ├── ExcelUtils.kt                       # Excel file creation
│       │   │   │   ├── ImageUtils.kt                       # Image processing utilities
│       │   │   │   ├── PermissionUtils.kt                  # Permission handling
│       │   │   │   ├── PreferencesManager.kt               # SharedPreferences wrapper
│       │   │   │   └── ZipUtils.kt                         # ZIP file creation
│       │   │   │
│       │   │   ├── viewmodel/              # ViewModels (MVVM)
│       │   │   │   ├── ClinicEntryViewModel.kt             # Clinic entry logic
│       │   │   │   ├── PatientEntryViewModel.kt            # Patient entry logic
│       │   │   │   └── ImageSequenceViewModel.kt           # Image capture logic
│       │   │   │
│       │   │   ├── MainActivity.kt         # Main entry point
│       │   │   └── OralVisApplication.kt   # Application class
│       │   │
│       │   └── res/                        # Resources
│       │       ├── drawable/               # Images & Drawables
│       │       │   ├── dental_ref_1.png                    # Reference: Front teeth
│       │       │   ├── dental_ref_2.png                    # Reference: Right side
│       │       │   ├── dental_ref_3.png                    # Reference: Left side
│       │       │   ├── dental_ref_4.png                    # Reference: Upper jaw
│       │       │   ├── dental_ref_5.png                    # Reference: Lower jaw
│       │       │   ├── dental_ref_6.png                    # Reference: Right cheek
│       │       │   ├── dental_ref_7.png                    # Reference: Left cheek
│       │       │   ├── dental_ref_8.png                    # Reference: Tongue
│       │       │   ├── oralvis_logo.png                    # App logo
│       │       │   ├── ic_launcher_background.xml
│       │       │   └── ic_launcher_foreground.xml
│       │       │
│       │       ├── mipmap-*/               # App Icons (various densities)
│       │       │   ├── ic_launcher.webp
│       │       │   └── ic_launcher_round.webp
│       │       │
│       │       ├── values/                 # Values Resources
│       │       │   ├── colors.xml                          # Color definitions
│       │       │   ├── strings.xml                         # String resources
│       │       │   └── themes.xml                          # Theme definitions
│       │       │
│       │       └── xml/                    # XML Configurations
│       │           ├── backup_rules.xml                    # Backup configuration
│       │           ├── data_extraction_rules.xml           # Data extraction rules
│       │           └── file_paths.xml                      # FileProvider paths
│       │
│       └── test/                           # Unit Tests
│           └── java/com/chaitany/oralvisjetpack/
│
├── gradle/                                 # Gradle Configuration
│   ├── libs.versions.toml                  # Version catalog
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
├── build.gradle.kts                        # Project-level Gradle config
├── gradle.properties                       # Gradle properties
├── gradlew                                 # Gradle wrapper (Unix)
├── gradlew.bat                             # Gradle wrapper (Windows)
├── local.properties                        # Local SDK configuration
├── settings.gradle.kts                     # Project settings
│
├── README.md                               # Project documentation
├── CONVERSION_SUMMARY.md                   # Conversion details
└── PROJECT_STRUCTURE.md                    # This file
```

## File Statistics

### Source Files
- **Total Kotlin Files**: 26
- **Resource Files**: 20+
- **Configuration Files**: 5

### Code Organization by Package

#### `data` package (8 files)
- **Purpose**: Data layer with database, models, and repositories
- **Files**: DAO interfaces, Room database, entity models, repositories
- **Lines**: ~350

#### `navigation` package (2 files)
- **Purpose**: App navigation logic
- **Files**: Navigation graph, screen routes
- **Lines**: ~100

#### `ui` package (7 files)
- **Purpose**: User interface layer
- **Files**: Compose screens, theme definitions
- **Lines**: ~800

#### `utils` package (5 files)
- **Purpose**: Helper utilities
- **Files**: Excel, ZIP, Image, Permission, Preferences utilities
- **Lines**: ~400

#### `viewmodel` package (3 files)
- **Purpose**: Business logic and state management
- **Files**: ViewModels for each screen
- **Lines**: ~350

#### Root package (2 files)
- **Purpose**: App initialization
- **Files**: MainActivity, Application class
- **Lines**: ~70

### Resource Files

#### Drawables (9 PNG files)
- 8 dental reference images (~5.4 MB total)
- 1 logo image
- 2 vector XML drawables

#### Values
- Colors, strings, themes
- Material 3 configurations

#### XML Configs
- FileProvider paths for camera
- Backup and data extraction rules

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                             │
│  (Jetpack Compose Screens + Material 3 Components)          │
│                                                              │
│  ClinicEntryScreen  WelcomeScreen  PatientEntryScreen       │
│                    ImageSequenceScreen                       │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    ViewModel Layer                           │
│              (State Management + Business Logic)            │
│                                                              │
│  ClinicEntryViewModel  PatientEntryViewModel                │
│              ImageSequenceViewModel                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Repository Layer                           │
│               (Data Access Abstraction)                      │
│                                                              │
│      ClinicRepository    PatientCounterRepository           │
└────────────┬────────────────────────────┬───────────────────┘
             │                            │
             ▼                            ▼
┌────────────────────────┐    ┌──────────────────────────────┐
│    Data Sources        │    │     Utilities                │
│                        │    │                              │
│  • Room Database       │    │  • ExcelUtils                │
│  • ClinicDao           │    │  • ZipUtils                  │
│  • PatientCounterDao   │    │  • ImageUtils                │
│  • SharedPreferences   │    │  • PermissionUtils           │
│                        │    │  • PreferencesManager        │
└────────────────────────┘    └──────────────────────────────┘
```

## Data Flow

### 1. Clinic Entry Flow
```
ClinicEntryScreen
    ↓ (user input)
ClinicEntryViewModel
    ↓ (saveClinicInfo)
ClinicRepository
    ↓ (insertClinic)
ClinicDao
    ↓ (Room)
SQLite Database
    ↓ (success callback)
Navigation → WelcomeScreen
```

### 2. Patient Entry Flow
```
PatientEntryScreen
    ↓ (user input)
PatientEntryViewModel
    ↓ (savePatientData)
ExcelUtils (create Excel)
    ↓
PatientCounterRepository (increment counter)
    ↓
SharedPreferences (store Excel bytes)
    ↓
Navigation → ImageSequenceScreen
```

### 3. Image Capture Flow
```
ImageSequenceScreen
    ↓ (camera launch)
CameraX TakePicture
    ↓ (image captured)
ImageSequenceViewModel
    ↓ (process & store)
ImageUtils (transform if needed)
    ↓ (all images captured)
ZipUtils (create ZIP)
    ↓ (save to storage)
Documents/myapp/filename.zip
    ↓ (complete)
Navigation → PatientEntryScreen
```

## Key Dependencies Flow

### Build-Time Dependencies
```
libs.versions.toml
    ↓
app/build.gradle.kts
    ↓
Gradle Sync
    ↓
Dependencies Downloaded
```

### Runtime Dependencies
```
Application Start
    ↓
OralVisApplication.onCreate()
    ↓
MainActivity.onCreate()
    ↓
Room Database Initialization
    ↓
Compose UI Setup
    ↓
Navigation Graph Setup
    ↓
Screen Rendering
```

## Module Organization

### Single Module Structure
```
app/
  ├── :app (application module)
```

*For scalability, can be refactored to:*
```
app/
  ├── :app (UI layer)
  ├── :data (data layer)
  ├── :domain (business logic)
  └── :common (shared utilities)
```

## Resource Naming Conventions

### Drawables
- `dental_ref_[number].png` - Reference images for capture steps
- `ic_*.xml` - Vector icons
- `*.webp` - App launcher icons

### Strings
- `app_name` - Application name
- `screen_title_*` - Screen titles
- `button_*` - Button labels
- `error_*` - Error messages

### Colors
- Material 3 dynamic color system
- Custom colors in Color.kt

## Build Variants

### Debug
- Application ID: `com.chaitany.oralvisjetpack`
- Debuggable: Yes
- Minify: No

### Release
- Application ID: `com.chaitany.oralvisjetpack`
- Debuggable: No
- Minify: Yes (ProGuard)

## File Size Overview

| Category | Size |
|----------|------|
| Kotlin Source | ~50 KB |
| Assets (Images) | ~5.4 MB |
| Resources (XML, etc.) | ~50 KB |
| Dependencies | ~30 MB (after build) |
| **Total APK** | ~35-40 MB (estimated) |

## Code Metrics

### Complexity by Layer

| Layer | Files | Approx. Lines | Complexity |
|-------|-------|---------------|------------|
| UI Layer | 7 | 800 | Medium |
| ViewModel | 3 | 350 | Medium |
| Data Layer | 8 | 350 | Low |
| Utils | 5 | 400 | Medium |
| Navigation | 2 | 100 | Low |
| **Total** | **25** | **~2000** | **Medium** |

## Testing Structure (Recommended)

```
app/src/
  ├── test/                          # Unit Tests
  │   └── java/com/chaitany/oralvisjetpack/
  │       ├── viewmodel/
  │       │   ├── ClinicEntryViewModelTest.kt
  │       │   ├── PatientEntryViewModelTest.kt
  │       │   └── ImageSequenceViewModelTest.kt
  │       ├── repository/
  │       │   └── RepositoryTests.kt
  │       └── utils/
  │           └── UtilsTests.kt
  │
  └── androidTest/                   # Instrumented Tests
      └── java/com/chaitany/oralvisjetpack/
          ├── database/
          │   └── DatabaseTests.kt
          └── ui/
              └── ScreenTests.kt
```

## Version Control Recommendations

### .gitignore Structure
```
# Android
*.iml
.gradle/
/local.properties
/.idea/
.DS_Store
/build/
/captures/
.externalNativeBuild/
.cxx/

# Gradle
.gradle/

# Generated files
bin/
gen/
out/

# Keystore files
*.jks
*.keystore

# Local configuration
local.properties
```

## Performance Optimization Points

### 1. Image Loading
- Use Coil for async image loading
- Implement image caching
- Compress images before saving

### 2. Database
- Use Flow for reactive queries
- Implement proper indexing
- Use transactions for batch operations

### 3. UI
- Minimize recompositions
- Use remember for stable references
- Implement proper state hoisting

### 4. Build
- Enable R8 shrinking in release
- Use ProGuard rules
- Optimize APK size

## Maintenance Checklist

- [ ] Update dependencies regularly
- [ ] Monitor app performance
- [ ] Check for memory leaks
- [ ] Review database migrations
- [ ] Test on different Android versions
- [ ] Verify storage permissions
- [ ] Test camera on different devices
- [ ] Validate Excel/ZIP creation
- [ ] Check crash reports
- [ ] Monitor user feedback

## Scalability Considerations

### Future Enhancements
1. **Multi-clinic support**: Add clinic selection screen
2. **Cloud sync**: Implement Firebase/backend integration
3. **Analytics**: Add Firebase Analytics
4. **Crash reporting**: Integrate Crashlytics
5. **Image analysis**: ML Kit for image quality
6. **Export formats**: PDF, CSV options
7. **User authentication**: Add login system
8. **Offline mode**: Enhance offline capabilities

### Performance Targets
- App startup: < 2 seconds
- Database queries: < 50ms
- Screen navigation: < 100ms
- Image capture: < 500ms
- ZIP creation: < 2 seconds (for 8 images)

---

**Document Version**: 1.0  
**Last Updated**: October 2025  
**Maintained By**: Development Team

