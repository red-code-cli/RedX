# Add project specific ProGuard rules here.

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.redx.codex.**$$serializer { *; }
-keepclassmembers class com.redx.codex.** {
    *** Companion;
}
-keepclasseswithmembers class com.redx.codex.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Coil
-dontwarn coil.**

# Glance
-keep class androidx.glance.** { *; }

# ZXing
-keep class com.google.zxing.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Our data models - never obfuscate
-keep class com.redx.codex.domain.model.** { *; }
-keep class com.redx.codex.data.network.dto.** { *; }
