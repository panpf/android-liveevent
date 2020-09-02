import com.novoda.gradle.release.PublishExtension
import java.util.*

plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    compileSdkVersion(property("COMPILE_SDK_VERSION").toString().toInt())

    defaultConfig {
        minSdkVersion(property("MIN_SDK_VERSION").toString().toInt())
        targetSdkVersion(property("TARGET_SDK_VERSION").toString().toInt())
        versionCode = property("VERSION_CODE").toString().toInt()
        versionName = property("VERSION_NAME").toString()

        consumerProguardFiles("proguard-rules.pro")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    api("androidx.lifecycle:lifecycle-common:${property("ANDROIDX_LIFECYCLE")}")
    api("androidx.arch.core:core-common:${property("ANDROIDX_ARCH_CORE")}")
    api("androidx.arch.core:core-runtime:${property("ANDROIDX_ARCH_CORE")}")

    androidTestImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${property("KOTLIN_VERSION")}")
    androidTestImplementation("androidx.test:runner:${property("TEST_RUNNER")}")
    androidTestImplementation("androidx.test:rules:${property("TEST_RULES")}")
    androidTestImplementation("androidx.fragment:fragment:${property("ANDROIDX_FRAGMENT")}")
}

Properties().apply { project.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }.takeIf { !it.isEmpty }?.let { localProperties ->
    apply { plugin("com.novoda.bintray-release") }

    configure<PublishExtension> {
        groupId = "com.github.panpf.liveevent"
        artifactId = "liveevent"
        publishVersion = property("VERSION_NAME").toString()
        desc = "Android, Event, Live"
        website = "https://github.com/panpf/android-liveevent"
        userOrg = localProperties.getProperty("bintray.userOrg")
        bintrayUser = localProperties.getProperty("bintray.user")
        bintrayKey = localProperties.getProperty("bintray.apikey")
    }
}
