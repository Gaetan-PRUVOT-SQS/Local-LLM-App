import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Clés de signature release — secrets résolus dans l'ordre :
//   1) propriété Gradle (-P / gradle.properties)   2) variable d'environnement (CI)
//   3) keystore.properties local (non versionné, dev uniquement)
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) FileInputStream(keystorePropsFile).use { load(it) }
}
fun secret(name: String, fallbackKey: String): String? =
    (project.findProperty(name) as String?)
        ?: System.getenv(name)
        ?: (keystoreProps[fallbackKey] as String?)

val ksStoreFile = secret("RELEASE_STORE_FILE", "storeFile")
val ksStorePassword = secret("RELEASE_STORE_PASSWORD", "storePassword")
val ksKeyAlias = secret("RELEASE_KEY_ALIAS", "keyAlias")
val ksKeyPassword = secret("RELEASE_KEY_PASSWORD", "keyPassword")
val hasReleaseSigning = listOf(ksStoreFile, ksStorePassword, ksKeyAlias, ksKeyPassword).all { it != null }

// URL de téléchargement du modèle, surchargeable sans recompiler le code
// (ex. miroir non-gated) : -PMODEL_URL_OVERRIDE=https://… ou variable d'env.
val modelUrlOverride = (project.findProperty("MODEL_URL_OVERRIDE") as String?)
    ?: System.getenv("MODEL_URL_OVERRIDE") ?: ""

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

android {
    namespace = "com.gaetan.localllmapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gaetan.localllmapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "MODEL_URL_OVERRIDE", "\"$modelUrlOverride\"")
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(ksStoreFile!!)
                storePassword = ksStorePassword
                keyAlias = ksKeyAlias
                keyPassword = ksKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    lint {
        // lintVital plante sur la chaîne d'outils (JDK 26) — désactivé pour la build release.
        checkReleaseBuilds = false
        abortOnError = false
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

    testImplementation("junit:junit:4.13.2")
}