# Mesh Link ProGuard / R8 Rules

# Preserve stacktrace line numbers for mapping file crash de-obfuscation
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses

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

# Image & Serialization Libraries
-keep class io.coilkt.** { *; }
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    *** INSTANCE;
}
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Domain models & entities used in Database/Serialization
-keep class com.meshlink.domain.model.** { *; }
-keep class com.meshlink.database.data.local.** { *; }

