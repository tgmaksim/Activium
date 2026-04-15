import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.android.build.gradle.internal.api.BaseVariantOutputImpl

// Переменные окружения
val propsFile: File = rootProject.file("variables.xml")
val props = Properties()

if (propsFile.exists()) {
    propsFile.inputStream().use {
        props.loadFromXML(it)
    }
} else {
    error("variables.xml not found")
}

val buildNumber = props.getProperty("BUILD_NUMBER").toInt()
val appVersion: String = props.getProperty("APP_VERSION")
val domain: String = props.getProperty("DOMAIN")
val apiKey: String = props.getProperty("API_KEY")
val checkInternetDomain: String = props.getProperty("CHECK_INTERNET_DOMAIN")

// Увеличение номера сборки при каждой компиляции
val newBuildNumber = buildNumber + 1

gradle.taskGraph.whenReady {
    if (hasTask(":app:assembleDebug") || hasTask(":app:assembleRelease")) {
        props["BUILD_NUMBER"] = newBuildNumber.toString()
        propsFile.outputStream().use {
            props.storeToXML(it, null, "UTF-8")
        }
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.devtools.ksp") version "2.3.6"
}

android {
    namespace = "ru.tgmaksim.activium"
    compileSdk {
        version = release(36)
    }

    applicationVariants.all {
        outputs.all {
            val output = this as BaseVariantOutputImpl
            output.outputFileName = "activium_v$appVersion.apk"
        }
    }

    defaultConfig {
        applicationId = "ru.tgmaksim.activium"
        minSdk = 29
        targetSdk = 36
        versionCode = newBuildNumber
        versionName = appVersion

        buildConfigField("String", "DOMAIN", "\"$domain\"")
        buildConfigField("String", "API_KEY", "\"$apiKey\"")
        buildConfigField("String", "CHECK_INTERNET_DOMAIN", "\"${checkInternetDomain}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(libs.okio)
    implementation(libs.okhttp)
    implementation(libs.flexbox)
    implementation(libs.material)
    implementation(libs.konfetti.xml)
    implementation(libs.coil.compose)
    implementation(libs.ktor.client.cio)
    implementation(libs.kotlinx.datetime)
    implementation(libs.ktor.client.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.firebase.analytics)
    implementation(libs.coil.network.okhttp)
    implementation(libs.firebase.crashlytics)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(platform(libs.firebase.bom))

    ksp(libs.androidx.room.compiler)

    api(libs.firebase.messaging)
    api(platform(libs.firebase.bom))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}