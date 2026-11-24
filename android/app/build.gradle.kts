import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Machine-specific, git-ignored. See local.properties.example.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
val backendBaseUrl = localProps.getProperty("backendBaseUrl") ?: "http://localhost:8081"
val teamKeyFile = localProps.getProperty("teamDebug.storeFile")?.let(::file)

android {
    namespace = "ro.banking40.team01"
    compileSdk = 37

    defaultConfig {
        // The event wallet only returns to this exact app ID signed with the team key.
        applicationId = "ro.banking40.team01"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "BACKEND_BASE_URL", "\"${backendBaseUrl.trimEnd('/')}/\"")
    }

    signingConfigs {
        getByName("debug") {
            if (teamKeyFile != null) {
                storeFile = teamKeyFile
                storeType = "PKCS12"
                storePassword = localProps.getProperty("teamDebug.storePassword")
                keyAlias = localProps.getProperty("teamDebug.keyAlias") ?: "androiddebugkey"
                keyPassword = localProps.getProperty("teamDebug.keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

if (teamKeyFile == null) {
    logger.warn("teamDebug.storeFile is not set in local.properties: the debug build uses the default " +
        "debug key and the EUDI Wallet will NOT return to this app.")
} else if (!teamKeyFile.exists()) {
    throw GradleException("teamDebug.storeFile does not exist: $teamKeyFile")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
