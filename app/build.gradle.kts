plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val bundledModelFile = rootProject.layout.projectDirectory.file("models/gemma-4-E2B-it.litertlm").asFile
val bundledChunksDir = rootProject.layout.projectDirectory.dir("models/chunks").asFile
val bundledManifest = rootProject.layout.projectDirectory.file("models/chunks/manifest.json").asFile

android {
    namespace = "com.gaetan.gemmchat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gaetan.gemmchat"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    sourceSets {
        getByName("main") {
            // Chunks seulement — pas le .litertlm entier (> limite Java 2 Go)
            assets.srcDirs("src/main/assets", "${rootProject.projectDir}/models/chunks")
        }
    }

    androidResources {
        noCompress += listOf("bin", "json")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")

    implementation("com.google.ai.edge.litertlm:litertlm-android:0.13.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

tasks.named("preBuild") {
    doFirst {
        require(bundledManifest.exists()) {
            """
            Chunks du modèle Gemma 4 E2B Q4 manquants.
            Le fichier .litertlm (~2,4 Go) dépasse la limite Android (2 Go) — il doit être découpé.

            Lancez:
              ./scripts/build_bundled_apk.sh

            Ou manuellement:
              ./scripts/download_model.sh
              ./scripts/split_model.sh
            """.trimIndent()
        }
        if (!bundledModelFile.exists()) {
            logger.lifecycle("Note: modèle source absent, chunks présents — OK pour la compilation.")
        }
    }
}