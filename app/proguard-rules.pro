# Mesh Link ProGuard / R8 Rules

# Hilt / Dagger
-keep class dagger.** { *; }
-keep class dagger.hilt.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep interface dagger.** { *; }
-keep interface dagger.hilt.** { *; }
-keep @dagger.Module class *
-keep @dagger.hilt.InstallIn class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *

# Room & SQLCipher
-keep class net.zetetic.database.sqlcipher.** { *; }
-keep class net.zetetic.database.** { *; }
-keep class androidx.room.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# AndroidX Navigation
-keepnames class androidx.navigation.NavType { *; }

# Firebase Crashlytics
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Ensure domain models used with Reflection or Serialization are kept
-keep class com.meshlink.domain.model.** { *; }
-keep class com.meshlink.database.data.local.** { *; }
