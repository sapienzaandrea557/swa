# Add project-specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in d:\Programmi\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class com.smart.giga.eater.** { *; }