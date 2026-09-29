# AnunciaAI — regras ProGuard/R8
# Kotlinx Serialization: manter serializers gerados
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class br.com.anunciaai.**$$serializer { *; }
-keepclassmembers class br.com.anunciaai.** { *** Companion; }
-keepclasseswithmembers class br.com.anunciaai.** { kotlinx.serialization.KSerializer serializer(...); }

# Ofuscação básica das chaves embutidas (BuildConfig) — uso pessoal
-repackageclasses 'anunciaai'

# OkHttp/Retrofit
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepclassmembers,allowshrinking,allowobfuscation interface * { @retrofit2.http.* <methods>; }

# Tink (security-crypto) referencia anotações errorprone ausentes no classpath — só dontwarn
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
