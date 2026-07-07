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

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep line numbers so release crash stack traces stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- kotlinx.serialization ----
# R8 must not strip/rename the generated serializers or the @Serializable
# model classes (our Ktor DTOs + UpdateChecker payloads).
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class com.rainbowcockroach.lifelog.data.remote.** {
    *** Companion;
}
-keepclassmembers class com.rainbowcockroach.lifelog.update.** {
    *** Companion;
}
-keepclasseswithmembers class com.rainbowcockroach.lifelog.data.remote.**, com.rainbowcockroach.lifelog.update.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keep, allowobfuscation, allowoptimization class <1> {
    static <1>$Companion Companion;
}

# Ktor, Room, Coil and kotlinx.serialization all ship consumer keep rules in
# their AARs, so no further manual rules are needed for them.