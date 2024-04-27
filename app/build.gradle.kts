import java.time.Instant
import java.text.SimpleDateFormat

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
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
            isMinifyEnabled = false
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
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:${rootProject.extra["lifecycle_version"]}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${rootProject.extra["lifecycle_version"]}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${rootProject.extra["lifecycle_version"]}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:${rootProject.extra["lifecycle_version"]}")

    // Preference datastore
    implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation ("androidx.datastore:datastore-preferences-rxjava2:1.1.0")
    implementation ("androidx.datastore:datastore-preferences-rxjava3:1.1.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Splash API
    implementation ("androidx.core:core-splashscreen:1.0.1")

    // Animation
    implementation("com.airbnb.android:lottie-compose:6.3.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}