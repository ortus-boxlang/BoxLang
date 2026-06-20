# BoxLang Android Runtime — consumer R8/ProGuard keep rules.
# These are applied automatically to any app that depends on this .aar.
#
# BoxLang relies on reflection, dynamic invocation, ServiceLoader, and (de)serialization,
# so the relevant types must survive shrinking/obfuscation.

# --- Core runtime: keep reflectively-accessed members ---
-keep class ortus.boxlang.runtime.** { *; }
-keep class ortus.boxlang.compiler.** { *; }
-dontwarn ortus.boxlang.**

# --- AOT-compiled BoxLang classes (handlers, views, Application.bx) ---
# Generated classes live under the boxgenerated package; never strip or rename them.
-keep class boxgenerated.** { *; }

# --- ServiceLoader providers (BIFs, components, boxpilers, services) ---
-keep class * implements ortus.boxlang.runtime.services.IService { *; }
-keep class * implements ortus.boxlang.compiler.IBoxpiler { *; }
-keepclassmembers class * {
    @ortus.boxlang.runtime.bifs.BoxBIF *;
    @ortus.boxlang.runtime.components.BoxComponent *;
}
-keep,allowobfuscation @interface ortus.boxlang.runtime.bifs.BoxBIF
-keep,allowobfuscation @interface ortus.boxlang.runtime.components.BoxComponent

# Keep META-INF/services entries intact for ServiceLoader.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations

# --- SLF4J / Logback (BoxLang logging) ---
-dontwarn org.slf4j.**
-dontwarn ch.qos.logback.**

# --- The JS bridge entry point used by the WebView track ---
-keepclassmembers class ortus.boxlang.runtime.android.BoxWebViewRenderer$Bridge {
    @android.webkit.JavascriptInterface <methods>;
}
