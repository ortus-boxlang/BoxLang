# BoxLang Android Runtime — module R8/ProGuard rules (release builds of this library).
# The consumer-facing rules live in consumer-rules.pro; this file covers the library's own
# release build. Keep them in sync.

-keep class ortus.boxlang.runtime.** { *; }
-keep class ortus.boxlang.compiler.** { *; }
-keep class boxgenerated.** { *; }
-dontwarn ortus.boxlang.**
-dontwarn org.slf4j.**
-dontwarn ch.qos.logback.**
# Parser toolchain is excluded from the APK (AOT/NoOp never parses on device); silence the
# missing-class warnings from the generated ANTLR parsers + JavaBoxpiler that reference them.
-dontwarn org.antlr.**
-dontwarn com.github.javaparser.**
-dontwarn com.google.**
-dontwarn javassist.**

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations
