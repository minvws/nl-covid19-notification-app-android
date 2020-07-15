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
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

-keepnames class androidx.fragment.app.FragmentContainerView
-keepnames class androidx.navigation.fragment.NavHostFragment

# Bouncy Castle
-keep class * extends java.security.Provider
-keep class org.bouncycastle.jce.provider.** {*;}
-keep class org.bouncycastle.jcajce.provider.** {*;}
-keep class org.bouncycastle.pqc.jcajce.provider.** {*;}

# Tink is used by androidx.security and its shaded protobufs needs to be kept
-keep class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite { *; }