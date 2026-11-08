# Proguard and R8 optimization rules for Resto app

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep class com.braveboy.resto.data.local.db.entity.** { *; }
-keep class com.braveboy.resto.data.local.db.Converters { *; }
-dontwarn androidx.room.paging.**

# Moshi Serialization & Data Models
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class com.braveboy.resto.data.model.** { *; }
-keep class com.braveboy.resto.data.remote.dto.** { *; }

# Retrofit
-keep,allowobfuscation,allowshrinking interface * {
    @retrofit2.http.* <methods>;
}

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Suppress harmless warnings
-dontnote **
-dontwarn **
