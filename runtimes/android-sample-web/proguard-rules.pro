# App-level R8 rules. The BoxLang consumer rules (keep runtime + AOT classes) are pulled
# in automatically from the :runtimes:android library's consumer-rules.pro.

# Keep AOT-compiled BoxLang app classes (handlers, views, layouts, Application).
-keep class boxgenerated.** { *; }
