# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Firebase model classes
-keep class com.recipeapp.models.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }