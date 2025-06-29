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

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Room database rules
-keep class com.bookmarklocker.data.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Room DAOs
-keep class * extends androidx.room.Dao { *; }

# Keep Room TypeConverters
-keep class * extends androidx.room.TypeConverter { *; }

# Keep Room schemas
-keep class * extends androidx.room.migration.Migration { *; }

# Keep Gson classes (if using for JSON serialization)
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep ViewBinding classes
-keep class com.bookmarklocker.databinding.** { *; }

# Keep Navigation Component classes
-keepnames class androidx.navigation.fragment.NavHostFragment
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable

# Keep Fragment classes
-keepnames class * extends androidx.fragment.app.Fragment

# Keep Activity classes
-keepnames class * extends androidx.appcompat.app.AppCompatActivity

# Keep ViewModel classes
-keepnames class * extends androidx.lifecycle.ViewModel
-keepnames class * extends androidx.lifecycle.AndroidViewModel

# Keep LiveData classes
-keepnames class * extends androidx.lifecycle.LiveData

# Keep RecyclerView adapters
-keepnames class * extends androidx.recyclerview.widget.RecyclerView$Adapter
-keepnames class * extends androidx.recyclerview.widget.RecyclerView$ViewHolder

# Keep DialogFragment classes
-keepnames class * extends androidx.fragment.app.DialogFragment

# Keep Material Design components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**