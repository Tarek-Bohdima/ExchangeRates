# Keep generic signatures so Retrofit can introspect Call<T> return types.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Moshi reflection-free codegen produces *JsonAdapter classes — keep them.
-keep,allowobfuscation,allowshrinking class * implements com.squareup.moshi.JsonAdapter
-keep,allowobfuscation,allowshrinking class **JsonAdapter { *; }

# Retrofit needs to call our suspend interface methods.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Keep DTO classes — Moshi codegen relies on the public constructor + properties.
-keep class com.tarek.exchangerates.data.remote.dto.** { *; }
