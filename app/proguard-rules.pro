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

# Firebase Installations Service
-keep class com.google.firebase.installations.** { *; }

# Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }

# Firebase Common
-keep class com.google.firebase.** { *; }
-keepattributes *Annotation*
-keepattributes Signature

# Google Play Services Auth (Required for SMS Retriever/Consent API)
-keep class com.google.android.gms.auth.api.phone.** { *; }
-keep class com.google.android.gms.common.api.** { *; }

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep your data models (Ensures Gson can parse them in Release)
-keep class com.tejashaqua.app.data.model.** { *; }

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Google Maps & Places
-keep class com.google.android.libraries.places.** { *; }
-keep class com.google.android.gms.maps.** { *; }
-dontwarn com.google.android.libraries.places.**

