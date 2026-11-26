plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }

android { namespace = "com.alzikr.app"; compileSdk = 35
    defaultConfig { applicationId = "com.alzikr.app"; minSdk = 26; targetSdk = 35; versionCode = 68; versionName = "1.5.1"
        manifestPlaceholders["appLabel"] = "ALzikr"
        if (project.hasProperty("fresh")) { applicationId = "com.alzikr.app.fresh"; manifestPlaceholders["appLabel"] = "ALzikr جديد" } }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

// Light on purpose: Compose + Material 3, plus Lottie for the companion pets.
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    // The companion pets: professional Lottie animations (LottieFiles, Lottie Simple License).
    implementation("com.airbnb.android:lottie-compose:6.6.0")
    testImplementation("junit:junit:4.13.2")
    // The real org.json for JVM tests (Android's copy is a stub there).
    testImplementation("org.json:json:20240303")
}
