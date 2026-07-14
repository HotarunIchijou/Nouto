-keepattributes Signature
-keepclassmembers class * extends com.onegravity.rteditor.spans.RTSpan {
    public <init>(int);
}
-keep class com.onegravity.rteditor.spans.** { *; }
-keep class com.onegravity.rteditor.effects.** { *; }

-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy