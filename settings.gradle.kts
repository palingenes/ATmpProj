@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io/") }
        maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
        maven { url = uri("https://s01.oss.sonatype.org/content/groups/public") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://artifacts.applovin.com/android") }
    }
}

rootProject.name = "ATmpProj"

include(":app") //  啥用没有
include(":autogame")    // 类似AutoApp，之前发布游戏任务手动调用使用，现疑似废弃
include(":webview") //  一个webview测试js的小工具，没啥用
