# NEXORA Play Store Release ProGuard / R8 Rules

# Preserve Firestore POJOs & data models serialized via reflection
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers class com.example.data.model.** { *; }
-keep class com.example.data.model.** { *; }
-keep class com.example.ai.** { *; }

# Keep AndroidManifest components (Services, Receivers, Providers, Activities)
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.appwidget.AppWidgetProvider

# Keep Kotlin Coroutines & Serialization metadata
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# Google Play Services / Credential Manager / Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# Moshi / OkHttp / Retrofit
-keepclasseswithmembers class * {
    @com.squareup.moshi.JsonClass <init>(...);
}
-dontwarn okhttp3.**
-dontwarn okio.**
