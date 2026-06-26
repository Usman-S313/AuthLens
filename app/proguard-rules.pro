# Add project-specific ProGuard rules here.

# OpenCV
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }

# Kotlin Coroutines
-keepclassmembernames class kotlinx.** { volatile <fields>; }
