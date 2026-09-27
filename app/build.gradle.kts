plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.androidmixtape"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.puzzleduck.mixtape"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    flavorDimensions += "platform"
    productFlavors {
        create("modern") {
            dimension = "platform"
            minSdk = 24
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    fun modernImplementation(dependencyNotation: Any) {
        add("modernImplementation", dependencyNotation)
    }

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    modernImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    modernImplementation("androidx.activity:activity-compose:1.9.3")
    modernImplementation("androidx.compose.material3:material3")
    modernImplementation("androidx.compose.ui:ui")
    modernImplementation("androidx.compose.ui:ui-tooling-preview")
    modernImplementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    modernImplementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    modernImplementation("androidx.media3:media3-exoplayer:1.5.1")
    modernImplementation("androidx.media3:media3-session:1.5.1")
    modernImplementation("androidx.car.app:app:1.8.0-beta01")
    modernImplementation("androidx.media:media:1.6.0")

    modernImplementation("androidx.compose.ui:ui-tooling")
    modernImplementation("androidx.compose.ui:ui-test-manifest")

    testCompileOnly("androidx.compose.runtime:runtime:1.7.6")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    add("androidTestUtil", "androidx.test:orchestrator:1.5.1")

    add("androidTestModernImplementation", platform("androidx.compose:compose-bom:2024.12.01"))
    add("androidTestModernImplementation", "androidx.compose.ui:ui-test-junit4")
}
