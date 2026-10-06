# Project specific ProGuard rules.

# Keep Room entities/DAO generated implementations.
-keep class com.example.data.model.** { *; }
-keep class com.example.data.local.** { *; }
-dontwarn androidx.room.paging.**

# EncryptedSharedPreferences / Tink rely on reflection over crypto primitives.
-keep class androidx.security.crypto.** { *; }
-dontwarn com.google.crypto.tink.**
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
    <fields>;
}

# OkHttp / Okio ship optional references to platform classes not present on Android.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Jsoup parses untrusted portal HTML.
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# Kotlin coroutines internals.
-dontwarn kotlinx.coroutines.**

# Keep source file and line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile