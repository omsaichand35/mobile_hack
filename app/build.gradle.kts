plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.friend"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.friend"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ✅ Required for native MediaPipe text classification (.so libs)
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
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

    // ✅ This enables prefab support for native libs
    buildFeatures {
        prefab = true
    }

    // ✅ Needed to pack native .so files
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.mediapipe:tasks-text:latest.release")
    implementation("com.google.mediapipe:tasks-text:0.4.0") {
        attributes {
            attribute(Attribute.of("org.gradle.jvm.environment", String::class.java), "android")
        }
    }

    // Jsoup for HTML parsing
    implementation("org.jsoup:jsoup:1.17.2")
}