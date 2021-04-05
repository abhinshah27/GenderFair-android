# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Massimo\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keepclassmembers class com.groops.fairsquare.activities_and_fragments.activities_home.menu.HomeMenuActivity.GFActionsMenuInterface {
   public *;
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# - Twilio - #
-keep class org.webrtc.** { *; }
-keep class com.twilio.video.** { *; }
-keepattributes InnerClasses

# - OkHttp - #
-dontwarn okhttp3.internal.platform.ConscryptPlatform
-dontwarn okhttp3.OkHttpClient$Builder
-dontwarn okio.DeflaterSink
-dontwarn okio.Okio

# - Google mp4parser - #
-dontwarn com.googlecode.mp4parser.authoring.tracks.mjpeg.OneJpegPerIframe

# - Slide Panel - #
-dontwarn com.sothree.slidinguppanel.SlidingUpPanelLayout

# - Crashlytics - #  to get de-obfuscated reports
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable


##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.groops.fairsquare.models.** { *; }

#This is extra - added by me to exclude gson obfuscation
-keep class com.google.gson.** { *; }
##---------------End: proguard configuration for Gson  ----------