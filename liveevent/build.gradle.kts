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

/**
 * publish config, The following properties are generally configured in the ~/.gradle/gradle.properties file
 */
if (hasProperty("signing.keyId")
    && hasProperty("signing.password")
    && hasProperty("signing.secretKeyRingFile")
    && hasProperty("mavenCentralUsername")
    && hasProperty("mavenCentralPassword")
) {
    apply { plugin("com.vanniktech.maven.publish") }

    configure<com.vanniktech.maven.publish.MavenPublishPluginExtension> {
        sonatypeHost = com.vanniktech.maven.publish.SonatypeHost.S01
    }
}