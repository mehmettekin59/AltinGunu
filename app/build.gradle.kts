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
        versionCode = 4
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true  // Kullanılmayan kaynakları kaldır
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


    implementation(libs.data.store)
    implementation(libs.data.store.core)
    implementation(libs.data.store.preferences.core)

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

    implementation(libs.androidx.lifecycle.process)

    //splashscreen
    implementation(libs.androidx.core.splashscreen)

    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.slf4j:slf4j-simple:1.7.36")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}