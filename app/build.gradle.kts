
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.runanywhere.startup_hackathon20"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.runanywhere.startup_hackathon20"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

}

dependencies {
    // RunAnywhere SDK - Local AARs from GitHub Release v0.1.3-alpha
    // Core SDK (4.01MB)
    implementation(files("libs/RunAnywhereKotlinSDK-release.aar"))
    // LLM Module (2.12MB) - includes llama.cpp with 7 ARM64 CPU variants
    implementation(files("libs/runanywhere-llm-llamacpp-release.aar"))

    // Required SDK dependencies (transitive dependencies from AARs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // Ktor for networking (required by SDK)
    implementation("io.ktor:ktor-client-core:3.0.3")
    implementation("io.ktor:ktor-client-okhttp:3.0.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
    implementation("io.ktor:ktor-client-logging:3.0.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")

    // OkHttp (required by SDK)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Retrofit (required by SDK)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Gson (required by SDK)
    implementation("com.google.code.gson:gson:2.11.0")

    // Okio (required by SDK)
    implementation("com.squareup.okio:okio:3.9.1")

    // AndroidX WorkManager (required by SDK)
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // AndroidX Room (required by SDK)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // AndroidX Security (required by SDK)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // OpenCV for image processing
    implementation("org.opencv:opencv:4.9.0")
    
    // Google ML Kit Text Recognition (OCR)
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    
    // CameraX for camera integration
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    
    // Coil for image loading in Compose
    implementation("io.coil-kt:coil-compose:2.5.0")



    // Standard app dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    //implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended:1.7.5")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // CameraX for camera functionality
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    
    // ML Kit Text Recognition (on-device OCR)
    implementation("com.google.mlkit:text-recognition:16.0.0")
    
    // Accompanist Permissions for runtime permission handling
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}


