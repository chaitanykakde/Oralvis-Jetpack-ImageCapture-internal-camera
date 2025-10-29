# OralVis Jetpack Compose - Native Android App

## Overview
This is a complete native Android application built with Kotlin and Jetpack Compose, converted from a Flutter project. The app is designed for dental clinics to capture and manage patient oral health images systematically.

## Features

### 1. **Clinic Management**
- Register clinic with name and ID
- Persistent storage using Room database
- Auto-navigation to Welcome screen on subsequent launches

### 2. **Patient Data Collection**
- Capture patient information (Name, Age, Gender, Phone)
- Auto-incrementing patient ID system
- Excel file generation for each patient
- Data validation and error handling

### 3. **Sequential Image Capture**
- Guided 8-step dental image capture process
- Reference images for each capture step:
  1. Front teeth (closed bite)
  2. Right side front teeth (closed bite)
  3. Left side front teeth (closed bite)
  4. Upper jaw (maxillary occlusal view)
  5. Lower jaw (mandibular occlusal view)
  6. Right cheek (buccal view)
  7. Left cheek (buccal view)
  8. Tongue (visible area)

### 4. **Image Processing**
- Camera integration using CameraX
- Image mirroring/flipping for specific angles
- Preview before saving
- Retake functionality

### 5. **Data Export**
- ZIP file creation with all images and patient Excel data
- Saved to Documents/myapp folder
- Naming convention: `PatientName_PhoneLastTwoDigits.zip`
- Image naming: `ClinicID_PatientID_SerialNumber.png`
- Excel naming: `ClinicID_PatientID.xlsx`

### 6. **Permissions Management**
- Runtime camera permissions
- Storage permissions (legacy and scoped storage)
- MANAGE_EXTERNAL_STORAGE for Android 11+
- User-friendly permission request dialogs

## Technology Stack

### Core Technologies
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36

### Architecture & Libraries

#### Architecture Components
- **MVVM Pattern** with ViewModels
- **Room Database** for local data persistence
- **Navigation Component** for screen navigation
- **StateFlow** for reactive UI updates

#### Key Dependencies
- **Jetpack Compose**: Modern declarative UI
- **Material 3**: Latest Material Design components
- **Room**: 2.6.1 - Local database
- **CameraX**: 1.4.0 - Camera integration
- **Navigation Compose**: 2.8.5 - Navigation
- **Accompanist Permissions**: 0.36.0 - Permission handling
- **Coil**: 2.7.0 - Image loading
- **Apache POI**: 5.2.3 - Excel file creation
- **Apache Commons Compress**: 1.27.1 - ZIP file creation

## Project Structure

```
app/src/main/java/com/chaitany/oralvisjetpack/
├── data/
│   ├── dao/
│   │   ├── ClinicDao.kt
│   │   └── PatientCounterDao.kt
│   ├── database/
│   │   └── OralVisDatabase.kt
│   ├── model/
│   │   ├── Clinic.kt
│   │   └── PatientCounter.kt
│   └── repository/
│       ├── ClinicRepository.kt
│       └── PatientCounterRepository.kt
├── navigation/
│   ├── NavGraph.kt
│   └── Screen.kt
├── ui/
│   ├── screens/
│   │   ├── ClinicEntryScreen.kt
│   │   ├── WelcomeScreen.kt
│   │   ├── PatientEntryScreen.kt
│   │   └── ImageSequenceScreen.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── utils/
│   ├── ExcelUtils.kt
│   ├── ImageUtils.kt
│   ├── PermissionUtils.kt
│   ├── PreferencesManager.kt
│   └── ZipUtils.kt
├── viewmodel/
│   ├── ClinicEntryViewModel.kt
│   ├── PatientEntryViewModel.kt
│   └── ImageSequenceViewModel.kt
├── MainActivity.kt
└── OralVisApplication.kt
```

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 11 or later
- Android SDK 24 or higher

### Installation

1. **Clone or Open Project**
   ```bash
   cd /Users/chaitanyskakde/AndroidStudioProjects/OralVisJetpack
   ```

2. **Sync Gradle**
   - Open project in Android Studio
   - Wait for Gradle sync to complete
   - All dependencies will be downloaded automatically

3. **Build Project**
   ```bash
   ./gradlew build
   ```

4. **Run on Device/Emulator**
   - Connect Android device or start emulator
   - Click "Run" button in Android Studio
   - Or use command: `./gradlew installDebug`

## Permissions Required

The app requests the following permissions:

- **CAMERA**: For capturing dental images
- **READ_EXTERNAL_STORAGE**: For reading images (Android 12 and below)
- **WRITE_EXTERNAL_STORAGE**: For saving files (Android 12 and below)
- **READ_MEDIA_IMAGES**: For reading images (Android 13+)
- **MANAGE_EXTERNAL_STORAGE**: For full storage access (Android 11+)

## Usage Flow

1. **First Launch**
   - Enter Clinic Name and ID
   - Tap "Save & Proceed"

2. **Subsequent Launches**
   - Automatically opens Welcome Screen
   - Shows clinic information

3. **Patient Entry**
   - Fill in patient details
   - Tap "Save and Next"
   - Grant permissions if prompted

4. **Image Capture**
   - Follow on-screen reference images
   - Capture each image using camera
   - Use Mirror/Original button for specific angles
   - Tap "Save & Next" to proceed

5. **Completion**
   - After all 8 images are captured
   - App automatically creates ZIP file
   - Returns to Patient Entry for next patient

## Data Storage

### Local Database
- **Location**: `/data/data/com.chaitany.oralvisjetpack/databases/`
- **Tables**: 
  - `clinic`: Stores clinic information
  - `patient_counter`: Tracks patient ID sequence

### Files
- **Location**: `/storage/emulated/0/Documents/myapp/`
- **Format**: ZIP files containing:
  - 8 PNG images (dental captures)
  - 1 XLSX file (patient data)

## Key Features Implementation

### Room Database
- Type-safe database access
- Coroutines support for async operations
- Migration support for schema updates

### Jetpack Compose UI
- Declarative UI programming
- Material 3 design system
- Reactive state management
- Proper lifecycle handling

### CameraX Integration
- Modern camera API
- FileProvider for secure file sharing
- Image capture and processing

### State Management
- ViewModel for business logic
- StateFlow for UI state
- Repository pattern for data access

## Differences from Flutter Version

### Architecture
- **Flutter**: Widget-based with State management
- **Native**: MVVM with ViewModels and Compose

### Database
- **Flutter**: SQLite with sqflite package
- **Native**: Room with Kotlin Coroutines

### Navigation
- **Flutter**: Navigator with MaterialPageRoute
- **Native**: Navigation Compose with type-safe routes

### Permissions
- **Flutter**: permission_handler plugin
- **Native**: Accompanist Permissions + ActivityResult API

### File Operations
- **Flutter**: path_provider + dart:io
- **Native**: Android Storage Access Framework + FileProvider

## Troubleshooting

### Common Issues

1. **Permission Denied Error**
   - Go to App Settings
   - Enable all required permissions
   - For Android 11+, enable "All files access"

2. **Camera Not Working**
   - Check CAMERA permission
   - Restart app after granting permission

3. **ZIP Creation Failed**
   - Verify storage permissions
   - Check available storage space
   - Ensure Documents/myapp folder is accessible

4. **Build Errors**
   - Clean project: `./gradlew clean`
   - Invalidate caches in Android Studio
   - Re-sync Gradle files

## Performance Considerations

- **Memory**: Images are processed in memory before saving
- **Storage**: ZIP files are created asynchronously
- **Database**: Room provides efficient query caching
- **UI**: Compose recomposes only changed elements

## Security & Privacy

- No network connections
- All data stored locally
- No analytics or tracking
- FileProvider ensures secure file access

## Future Enhancements

Potential improvements:
- Cloud backup integration
- Multi-clinic support
- Image quality analysis
- Patient history viewing
- Data export options
- Dark theme support
- Localization support

## License

This project is converted from a Flutter application for native Android development.

## Contact & Support

For issues or questions, please refer to the project documentation or contact the development team.

---

**Version**: 1.0.0
**Last Updated**: October 2025
**Platform**: Android (Native Kotlin + Jetpack Compose)

