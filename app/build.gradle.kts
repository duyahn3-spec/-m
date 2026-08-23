plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.screenai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.screenai"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "3.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }

        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("org.tensorflow:tensorflow-lite:2.17.0")
}
