plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.chaitany.oralvisjetpack"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.chaitany.oralvisjetpack"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

// Add testClasses task for compatibility (maps to Android test compilation tasks)
tasks.register("testClasses") {
    dependsOn("compileDebugUnitTestSources")
    description = "Compiles test classes (compatibility task for Android projects)"
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    
    // Permissions
    implementation(libs.accompanist.permissions)
    
    // ExifInterface (for image rotation fix)
    implementation(libs.androidx.exifinterface)
    
    // Compression (for ZIP files)
    implementation(libs.apache.commons.compress)
    
    // AWS SDK (using known stable version)
    implementation("com.amazonaws:aws-android-sdk-core:2.6.31")
    implementation("com.amazonaws:aws-android-sdk-s3:2.6.31")
    implementation("com.amazonaws:aws-android-sdk-ddb:2.6.31")
    implementation("com.amazonaws:aws-android-sdk-ddb-mapper:2.6.31")
    implementation("com.amazonaws:aws-android-sdk-cognito:2.6.31")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}