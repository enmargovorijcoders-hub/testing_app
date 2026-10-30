plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.devicetest"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.devicetest"
        minSdk = 14          // Android 4.0
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // 32-bit and 64-bit ARM. The app has no native code, so it also runs on any other CPU.
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a") }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Signed with the auto-generated debug key so the APK installs without extra setup.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}
