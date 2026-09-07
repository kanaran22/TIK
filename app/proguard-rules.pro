# Add project specific ProGuard rules here.
# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# --- Jetpack Glance (home screen widget) ---
# Glance serialises its layout/session state through protobuf and resolves the
# widget + action classes reflectively. Without these, R8 strips enough that the
# widget never renders past its initialLayout placeholder — silently, no crash.
-keep class androidx.glance.appwidget.protobuf.** { *; }
-keep class androidx.glance.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * implements androidx.glance.appwidget.action.ActionCallback { *; }

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
