# ProGuard rules for ZionChat

# =====================
# General Android
# =====================

# Keep application classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.view.View

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# =====================
# Kotlin
# =====================

# Keep Kotlin Metadata for reflection
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Kotlin serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.zionchat.app.data.**$$serializer { *; }
-keepclassmembers class com.zionchat.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.zionchat.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# =====================
# Data Models (Gson/Kotlinx Serialization)
# =====================

# Keep all data classes in the data package for serialization
-keep class com.zionchat.app.data.** { *; }
-keepclassmembers class com.zionchat.app.data.** {
    <fields>;
    <init>(...);
}

# Keep data class companion objects
-keepclassmembers class com.zionchat.app.data.**$Companion { *; }

# =====================
# OkHttp / Okio
# =====================

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Keep OkHttp connection pool
-keep class okhttp3.ConnectionPool { *; }
-keep class okhttp3.OkHttpClient { *; }
-keep class okhttp3.Request { *; }
-keep class okhttp3.Response { *; }

# =====================
# Ktor Client
# =====================

-dontwarn io.ktor.**
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** {
    <methods>;
}

# =====================
# Coil (Image Loading)
# =====================

-keep class coil3.** { *; }
-keep interface coil3.** { *; }
-dontwarn coil3.**

# =====================
# Jetpack Compose
# =====================

# Keep Compose UI classes
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Compose compiler generated classes
-keep class * extends androidx.compose.runtime.Composer { *; }
-keep class * implements androidx.compose.runtime.Composer { *; }

# Keep Composable functions
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# =====================
# Material 3
# =====================

-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# =====================
# Navigation
# =====================

-keep class androidx.navigation.** { *; }
-keepclassmembers class androidx.navigation.** {
    <methods>;
}

# =====================
# DataStore
# =====================

-keep class androidx.datastore.** { *; }
-keepclassmembers class androidx.datastore.** {
    <methods>;
}

# =====================
# Lifecycle / ViewModel
# =====================

-keep class androidx.lifecycle.** { *; }
-keepclassmembers class androidx.lifecycle.** {
    <methods>;
}

# =====================
# MCP Kotlin SDK
# =====================

-keep class io.modelcontextprotocol.** { *; }
-keep interface io.modelcontextprotocol.** { *; }
-dontwarn io.modelcontextprotocol.**

# =====================
# CommonMark (Markdown)
# =====================

-keep class org.commonmark.** { *; }
-keep interface org.commonmark.** { *; }

# =====================
# Gson
# =====================

# Keep Gson serialized classes
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

# Keep generic signature for Gson
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.* <fields>;
}

# =====================
# Logging Removal (Release builds)
# =====================

# Remove logging calls in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(...);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Remove println calls
-assumenosideeffects class kotlin.io.ConsoleKt {
    public static void println(...);
}

# =====================
# Optimization Settings
# =====================

# Enable aggressive optimization
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Optimization settings
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# Allow access modification for better optimization
-allowaccessmodification

# Keep source file names and line numbers for stack traces
-keepattributes SourceFile,LineNumberTable

# Keep annotations
-keepattributes *Annotation*

# Keep generic signatures for reflection
-keepattributes Signature

# Keep exception info for better crash reports
-keepattributes Exceptions
