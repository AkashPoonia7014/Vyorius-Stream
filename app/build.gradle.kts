plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.sky.VyoriusStream"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sky.VyoriusStream"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

//        ndk {
//            abiFilters += "armeabi-v7a"
//            abiFilters += "arm64-v8a"
//        }
    }
    splits {
        abi {
            isEnable = true            // Note the 'isEnable' assignment for Kotlin DSL
            reset()
            include("arm64-v8a")
            isUniversalApk = false     // 'isUniversalApk' to disable universal APK creation
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true           // Enables code minification via R8/ProGuard.
            isShrinkResources = true         // Removes unused resources to further slim down the APK.
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // VLC for RTSP
    implementation ("org.videolan.android:libvlc-all:3.6.0")

    // scalable Size Unit (support for different screen sizes)
    implementation ("com.intuit.sdp:sdp-android:1.1.0")
    implementation ("com.intuit.ssp:ssp-android:1.1.0")

    // Screen Recording
    implementation ("com.github.HBiSoft:HBRecorder:3.0.9")






}