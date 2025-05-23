plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.mehmettekin.altingunu"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mehmettekin.altingunu"
        minSdk = 26
        targetSdk = 35
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.text)

    //Hilt and KSP
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    //Hilt Navigation
    implementation(libs.hilt.navigation)
    //navigation compose
    implementation(libs.androidx.navigation.compose)

    //Data store
    implementation(libs.data.store)

    //Retrofit
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.rxjava3)
    implementation(libs.retrofit.moshi)

    //OkHtpp
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    //moshi-kotlin
    implementation(libs.moshi.kotlin)

    //itext7
    implementation(libs.itext7.core)

    //Extanded Icons
    implementation(libs.extended.icons)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}