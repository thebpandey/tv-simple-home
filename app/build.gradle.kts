import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "tv.simplehome.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "tv.simplehome.launcher"
        minSdk = 26
        targetSdk = 34
        versionCode = 18
        versionName = "2.7"
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 is safe here: no reflection, and the manifest-declared activity is kept automatically.
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { buildConfig = true }

    lint { abortOnError = true }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    // The framework org.json is a stub on the JVM; tests need a real implementation.
    testImplementation("org.json:json:20240303")
}
