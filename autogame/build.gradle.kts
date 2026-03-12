import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-kapt")
}

fun releaseTime(): String {
    val current = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return current.format(formatter)
}

android {
    namespace = "com.cymf.autogame"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cymf.autogame"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "1.0.9"
        multiDexEnabled = true
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "PATH_M", "\"auto\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        setProperty("archivesBaseName", "AutoGame-v${versionName}-${releaseTime()}")
    }
    signingConfigs {
        create("config") {
            storeFile = File("../jks/tmp.jks")
            keyAlias = "key0"
            keyPassword = "tmp112233"
            storePassword = "tmp112233"

            enableV1Signing = true
            enableV2Signing = true
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("config")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("config")
            isDebuggable = true
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    lint {
        baseline = file("lint-baseline.xml")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-Xinline-classes",
            "-Xallow-result-return-type",
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property"
        )
    }
}

tasks.withType(type = KaptGenerateStubsTask::class) {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.core:core:1.16.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.lifecycle:lifecycle-common:2.8.7")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("com.kunminx.arch:unpeek-livedata:7.8.0")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    implementation("com.google.guava:guava:33.5.0-jre")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.2.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.concurrent:concurrent-futures-ktx:1.2.0")

    implementation("com.squareup.okhttp3:okhttp:5.3.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.0")
    implementation("com.squareup.okio:okio:3.16.2")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.moshi:moshi:1.15.2")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    implementation("com.squareup.retrofit2:converter-moshi:3.0.0")

    implementation("com.github.getActivity:Toaster:13.6")

    // 为 shell 提供 API 的核心模块
    implementation("com.github.topjohnwu.libsu:core:6.0.0")
}