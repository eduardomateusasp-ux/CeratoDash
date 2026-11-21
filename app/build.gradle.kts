import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "br.com.ceratodash"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.com.ceratodash"
        minSdk = 24
        // targetSdk 30: no Android 12 da central, as permissões Bluetooth antigas
        // continuam valendo, sem pedir autorização em tempo de execução.
        targetSdk = 30
        versionCode = 7
        versionName = "0.7.0"
    }

    // Assinatura de release. A keystore NUNCA fica no repositório:
    //  - no GitHub Actions: lida dos Secrets (KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD);
    //  - no computador: lida de keystore.properties (ignorado pelo git).
    // Sem nenhuma das duas, usa a chave de debug, para o APK instalar por pendrive do mesmo jeito.
    val keystoreProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    fun segredo(env: String, prop: String): String? =
        System.getenv(env)?.takeIf { it.isNotBlank() } ?: keystoreProps.getProperty(prop)
    val ksFile = segredo("KEYSTORE_FILE", "storeFile")

    signingConfigs {
        if (ksFile != null) {
            create("release") {
                storeFile = file(ksFile)
                storePassword = segredo("KEYSTORE_PASSWORD", "storePassword")
                keyAlias = segredo("KEY_ALIAS", "keyAlias")
                keyPassword = segredo("KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (ksFile != null) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
}
