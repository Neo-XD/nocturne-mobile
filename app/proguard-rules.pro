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

# NewPipe Extractor JavaScript engine & classes
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
-keep class org.schabi.newpipe.extractor.** { *; }
