# ─── LiteRT-LM (JNI / natif) ───────────────────────────────────────────────
# L'API et les bindings natifs sont appelés via JNI → on garde tout.
-keep class com.google.ai.edge.litertlm.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.ai.edge.litertlm.**

# Méthodes natives (JNI) — ne jamais renommer/supprimer.
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Classes/champs référencés depuis le code natif.
-keepclasseswithmembers class * {
    @com.google.ai.edge.litertlm.* <methods>;
}

# ─── Enums (valueOf via réflexion : ConversationKind, ModelVariant, etc.) ───
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ─── Coil (chargement d'images) ─────────────────────────────────────────────
-dontwarn coil.**

# ─── Kotlin / Coroutines ────────────────────────────────────────────────────
-keepclassmembers class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**

# ─── Compose ────────────────────────────────────────────────────────────────
# (AGP + librairies fournissent leurs propres règles ; rien à ajouter en général)

# Conserver les traces de ligne pour un debug release lisible.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
