plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")

}


android {
    namespace = "com.example.whitecard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.whitecard"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation ("androidx.compose.material:material:1.5.0")



    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.05.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.6.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.1.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation ("com.google.mlkit:barcode-scanning:17.3.0")
    implementation ("com.google.firebase:firebase-messaging-ktx:23.4.0")
    implementation (platform("com.google.firebase:firebase-bom:32.5.0"))
    implementation ("com.google.firebase:firebase-auth-ktx")


    // Google Play Services Auth
    implementation ("com.google.android.gms:play-services-auth:20.7.0")

    // Firebase Auth (if you're not already using it)


    // Firebase Firestore (if you're not already using it)
    implementation ("com.google.firebase:firebase-firestore-ktx:24.10.0")
    implementation ("com.google.android.gms:play-services-auth:20.7.0")

    // Kotlin Coroutines support for Firebase
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")


    // QR code scanning
    implementation("com.google.mlkit:barcode-scanning:17.1.0")
    implementation("androidx.camera:camera-camera2:1.2.3")
    implementation("androidx.camera:camera-lifecycle:1.2.3")
    implementation("androidx.camera:camera-view:1.2.3")

    // QR code generation

    implementation("com.google.zxing:core:3.4.1")


    implementation ("androidx.work:work-runtime-ktx:2.8.1")



    //coil
    implementation("io.coil-kt:coil-compose:2.5.0")


    implementation("com.google.mlkit:face-detection:16.1.5")
    implementation("com.google.android.gms:play-services-mlkit-face-detection:17.1.0")


    implementation ("com.google.mlkit:face-detection:16.1.5")

    // Optional: ML Kit Face Mesh detection for more detailed contours
    implementation ("com.google.mlkit:face-mesh-detection:16.0.0-beta1")










    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.05.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")


        implementation("androidx.core:core-splashscreen:1.0.1")


    implementation ("com.google.mlkit:text-recognition:16.0.0")

// Compose
    implementation("androidx.compose.ui:ui:1.6.5")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material:1.6.5")
    implementation("androidx.activity:activity-compose:1.9.0")

// CameraX
    implementation("androidx.camera:camera-core:1.3.3")
    implementation("androidx.camera:camera-camera2:1.3.3")
    implementation("androidx.camera:camera-lifecycle:1.3.3")
    implementation("androidx.camera:camera-view:1.3.3")

// ML Kit
    implementation("com.google.mlkit:face-detection:16.1.6")
    implementation("com.google.mlkit:text-recognition:16.0.0")

// Lifecycle, Navigation, Coroutines
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

// Optional: Coil for images
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ML Kit Face detection
    implementation("com.google.mlkit:face-detection:16.1.6")
// ML Kit Text recognition (includes Latin)
    implementation("com.google.mlkit:text-recognition:16.0.0")


    implementation("com.google.android.gms:play-services-auth:21.0.0") // Example for auth
    implementation("com.google.android.gms:play-services-location:21.2.0") // Example for location
    // ... other play services dependencies
    implementation("com.google.mlkit:object-detection:17.0.0")

    implementation ("androidx.exifinterface:exifinterface:1.3.6")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")


    implementation ("com.google.mlkit:text-recognition:16.0.0")

// Firebase Storage
    implementation ("com.google.firebase:firebase-storage-ktx")

// Coroutines support for Firebase
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")







}

