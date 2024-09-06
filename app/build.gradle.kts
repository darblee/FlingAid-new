import java.time.Instant
import java.text.SimpleDateFormat

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.dokka")
}

android {
    namespace = "com.darblee.flingaid"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.darblee.flingaid"
        minSdk = 33
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Generate the Build Time.
            // In the app, you can retrieve build time info.
            // e.g.     Log.i(TAG, "Build time is " + BuildConfig.BUILD_TIME)
            //          Log.i(TAG, "Build time is " + getString(R.string.build_time))
            val instant = Instant.now()
            val sdf = SimpleDateFormat("MM/dd/yyyy, HH:mm:ss")
            val buildTime = sdf.format(instant.epochSecond * 1000L)
            buildConfigField("String", "BUILD_TIME", "\"${buildTime}\"")
            resValue("string", "build_time", "\"${buildTime}\"")
            signingConfig = signingConfigs.getByName("debug")
        }

        debug {

            // Generate the Build Time.
            // In the app, you can retrieve build time info.
            // e.g.     Log.i(TAG, "Build time is " + BuildConfig.BUILD_TIME)
            //          Log.i(TAG, "Build time is " + getString(R.string.build_time))
            val instant = Instant.now()
            val sdf = SimpleDateFormat("MM/dd/yyyy, HH:mm:ss")
            val buildTime = sdf.format(instant.epochSecond * 1000L)
            buildConfigField("String", "BUILD_TIME", "\"${buildTime}\"")
            resValue("string", "build_time", "\"${buildTime}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
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

    // Lifecycle
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)

    // Serialization. This is needed for navigation-compose as well.
    implementation(libs.kotlinx.serialization.json)

    // Preference datastore
    implementation (libs.androidx.datastore.preferences.rxjava2)
    implementation (libs.androidx.datastore.preferences.rxjava3)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Splash API
    implementation (libs.androidx.core.splashscreen)

    // Animation
    implementation(libs.lottie.compose)
}