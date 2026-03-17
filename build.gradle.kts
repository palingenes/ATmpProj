buildscript {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven("https://artifacts.applovin.com/android")
        maven("https://jitpack.io")
        maven("https://maven.aliyun.com/nexus/groups/public/")
        maven("https://maven.aliyun.com/repository/public/")
        maven("https://maven.aliyun.com/repository/jcenter/")
        maven("https://maven.aliyun.com/repository/gradle-plugin/")
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath("io.objectbox:objectbox-gradle-plugin:5.3.0")
    }
}

plugins {
    id("com.android.application") version "8.13.0" apply false
    id("com.android.library") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10" apply false
}
