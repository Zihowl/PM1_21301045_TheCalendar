plugins {
    alias(libs.plugins.android.application)
    id("realm-android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.zihowl.thecalendar"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zihowl.thecalendar"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Base URL para las peticiones. Puede ser establecido mediante la
        // variable de entorno API_BASE_URL al momento de la compilación.
        val apiUrl = System.getenv("API_BASE_URL") ?: "https://192.168.0.96:5000"
        buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")

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
    buildFeatures {
        viewBinding = false
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    implementation("androidx.core:core:1.12.0")

    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging:23.4.0")

    implementation("io.realm:realm-android-library:10.19.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

    // WorkManager for background sync
    implementation("androidx.work:work-runtime:2.9.0")

    // EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-beta01")
}