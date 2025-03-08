buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

android {
    compileSdk = 35
    buildToolsVersion = libs.versions.android.build.tools.get()

    defaultConfig {
        applicationId = "io.requery.android.example.kotlinapp"
        minSdk = 15
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
        }
    }

    lint {
        abortOnError = false
    }

    sourceSets {
        getByName("main") {
            java {
                srcDirs("src/main/kotlin")
            }
        }
    }

    namespace = "io.requery.android.example.kotlinapp"
}

dependencies {
    implementation("androidx.appcompat:appcompat:${libs.versions.androidx.get()}")
    implementation("androidx.recyclerview:recyclerview:${libs.versions.androidx.get()}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}")
    implementation("io.reactivex.rxjava2:rxjava:${libs.versions.rxjava2.get()}")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.0")
    //implementation(project(":requery")) // replace with 'io.requery:requery:<version>'
    implementation(project(":requery-android")) // replace with 'io.requery:requery-android:<version>'
    implementation(project(":requery-kotlin")) // replace with 'io.requery:requery-kotlin:<version>'
    kapt(project(":requery-processor")) // replace with 'io.requery:requery-processor:<version>'
}