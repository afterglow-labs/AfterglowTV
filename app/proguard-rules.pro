# AfterglowTV ProGuard Rules
# ============================================================

# ── Kotlin ──────────────────────────────────────────────────
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }

# ── Hilt / Dagger ───────────────────────────────────────────
-keepclasseswithmembers class * {
    @dagger.* <methods>;
}
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ── Retrofit / OkHttp ───────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
# SEC-H02: Only keep what Retrofit actually requires — interface methods & response types.
-keep,allowobfuscation interface retrofit2.Call
-keep,allowobfuscation interface retrofit2.Callback
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keepclassmembers,allowobfuscation class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepnames class okhttp3.internal.platform.**
-keepnames class okhttp3.internal.tls.**

# ── GSON ────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes EnclosingMethod
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# Keep fields that GSON accesses via reflection
-keepclassmembers class com.afterglowtv.data.remote.** { <fields>; }

# ── Room ────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ── Domain models (serialized by GSON / passed across modules) ──
-keep class com.afterglowtv.domain.model.** { *; }
-keep class com.afterglowtv.data.local.entity.** { *; }
-keep class com.afterglowtv.data.remote.xtream.model.** { *; }

# ── Security / TLS ──────────────────────────────────────────
-keepnames class com.afterglowtv.data.security.CredentialCrypto
-keepclassmembers class com.afterglowtv.data.security.CredentialCrypto {
    <fields>;
    <methods>;
}

# ── SMBJ optional desktop/JVM integrations ─────────────────
# SMBJ references optional Kerberos/GSS and expression-language classes that are
# not available on Android. We use password-based SMB paths, so these optional
# code paths are not required for Fire TV builds.
-dontwarn javax.el.**
-dontwarn org.ietf.jgss.**

# ── Media3 / ExoPlayer ─────────────────────────────────────
# SEC-H01: Media3 ships its own consumer-proguard-rules.pro inside the AAR — no need
# for a broad keep here. Only explicitly keep classes loaded via reflection that the
# AAR rules don't cover (e.g., extension renderers resolved at runtime).
-keep class androidx.media3.exoplayer.DefaultRenderersFactory { *; }
-dontwarn androidx.media3.**

# ── Amazon Appstore SDK / Fire IAP + DRM ───────────────────
-keep class com.amazon.device.iap.** { *; }
-keep class com.amazon.device.drm.** { *; }
-keep class com.afterglowtv.app.store.amazon.AmazonAppstoreBridge { *; }
-dontwarn com.amazon.**

# ── Coroutines ──────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ── Compose ─────────────────────────────────────────────────
-dontwarn androidx.compose.**

# ── Coil (image loading) ───────────────────────────────────
-dontwarn coil3.**

# ── DataStore ───────────────────────────────────────────────
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ── General ─────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable       # Better crash reports
-renamesourcefileattribute SourceFile
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
