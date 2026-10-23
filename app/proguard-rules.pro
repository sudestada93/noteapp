# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.libreta.app.**$$serializer { *; }
-keepclassmembers class com.libreta.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.libreta.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
