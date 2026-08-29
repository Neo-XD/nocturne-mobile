# Nocturne Mobile ProGuard Rules

# Keep Media3 classes for reflection-based player features
-keep class androidx.media3.** { *; }

# Keep Kotlin Serialization models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor engine
-keep class io.ktor.** { *; }
