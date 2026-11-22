#
# Angel Client release hardening
#
-injars @IN@
-outjars @OUT@
-libraryjars <java.home>/lib/rt.jar
-libraryjars <java.home>/lib/jce.jar
-libraryjars @LIB@
-libraryjars @MCLIB@
-printmapping @MAP@

-dontshrink
-dontoptimize
-dontnote **
-dontwarn **
-ignorewarnings
-verbose

-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-allowaccessmodification

# Keep annotations/signatures (Mixin + Gson depend on them) but drop all
# debugging information so decompiled output cannot be traced back.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions,Deprecated,MethodParameters
-renamesourcefileattribute Angel

# Entry points and things addressed by name at runtime.
-keep class io.github.angelclient.client.PreMain { *; }
-keep class io.github.angelclient.client.Client { *; }
-keep class io.github.angelclient.client.tweak.** { *; }
-keep class io.github.angelclient.client.mixin.** { *; }
-keep class io.github.angelclient.client.mod.Mod { *; }
-keep class io.github.angelclient.client.mod.hud.HudMod { *; }
-keep class io.github.angelclient.client.event.EventHandler { *; }
-keepclassmembers class * { @io.github.angelclient.client.event.EventHandler <methods>; }
-keepclassmembers class * { @com.google.gson.annotations.Expose <fields>; }
-keepclassmembers class * { @com.google.gson.annotations.SerializedName <fields>; }
-keepclassmembers enum * { *; }
-keep class * implements java.io.Serializable { *; }

# Keep mod names (registered by name) but allow member renaming.
-keepnames class * extends io.github.angelclient.client.mod.Mod

# Mod settings are stored/translated by field name, so field names must stay.
-keepclassmembers class * extends io.github.angelclient.client.mod.Mod { <fields>; }

# Aggressive naming.
-obfuscationdictionary @DICT@
-classobfuscationdictionary @DICT@
-packageobfuscationdictionary @DICT@
