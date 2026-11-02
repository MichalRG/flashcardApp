import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val signingPropertiesFile = File(System.getProperty("user.home"), ".vocabboost-signing/keystore.properties")

android {
    namespace = "com.krzyzowski.vocabboost"
    // Current AndroidX releases require compiling against API 37; the app still targets 36.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.krzyzowski.vocabboost"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.4"
    }

    signingConfigs {
        if (signingPropertiesFile.exists()) {
            val signing = Properties().apply { signingPropertiesFile.inputStream().use(::load) }
            create("release") {
                storeFile = File(signing.getProperty("storeFile"))
                storePassword = signing.getProperty("storePassword")
                keyAlias = signing.getProperty("keyAlias")
                keyPassword = signing.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
