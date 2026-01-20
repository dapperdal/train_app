# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /sdk/tools/proguard/proguard-android.txt

# Keep Retrofit
-keepattributes Signature
-keepattributes Exceptions

# Gson
-keepattributes *Annotation*
-keep class com.darren.trains.data.model.** { *; }
