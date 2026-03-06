# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /path/to/android-sdk/tools/proguard/proguard-android.txt

# Keep Room entities
-keep class com.bordrotakip.data.local.entity.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep ML Kit component registrars (release OCR init)
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
-keep class com.google.mlkit.common.internal.CommonComponentRegistrar { <init>(); *; }
-keep class com.google.mlkit.vision.common.internal.VisionCommonRegistrar { <init>(); *; }
-keep class com.google.mlkit.vision.text.internal.TextRegistrar { <init>(); *; }
