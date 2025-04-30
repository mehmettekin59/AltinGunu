# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
#################################
# Retrofit
#################################
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

#################################
# Moshi (JSON parsing)
#################################
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
# Domain model sınıflarını koru
-keep class com.mehmettekin.altingunu.domain.model.** { *; }
# Prevent obfuscating Kotlin metadata
-keepclassmembers class ** {
    @com.squareup.moshi.* <methods>;
}
-keepattributes RuntimeVisibleAnnotations

#################################
# OkHttp
#################################
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

#################################
# Hilt (Dependency Injection)
#################################
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class dagger.** { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory
-keep class androidx.hilt.** { *; }
-keepclassmembers class ** {
    @dagger.** *;
    @javax.inject.** *;
}

#################################
# AndroidX DataStore (Preferences + Proto)
#################################
-keep class androidx.datastore.** { *; }
-keep class kotlinx.coroutines.flow.** { *; }
-dontwarn androidx.datastore.**

#################################
# iText7 (PDF Library)
#################################
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

#################################
# Navigation Component
#################################
-keepnames class androidx.navigation.** { *; }

#################################
# Compose UI
#################################
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

#################################
# Coroutines
#################################
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

#################################
# General keep rules (recommended)
#################################
-keepclassmembers class * {
    public <init>(...);
}
-keepattributes InnerClasses
-keepattributes EnclosingMethod

#################################
# Model sınıfları için özel kurallar
#################################
# ResultState ve UiText sınıflarını koru
-keep class com.mehmettekin.altingunu.utils.ResultState { *; }
-keep class com.mehmettekin.altingunu.utils.UiText { *; }
-keep class com.mehmettekin.altingunu.utils.UiText$* { *; }

#################################
# ViewModels
#################################
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}