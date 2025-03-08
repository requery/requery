buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
}

android {
    compileSdk = 35
    buildToolsVersion = libs.versions.android.build.tools.get()

    defaultConfig {
        minSdk = 14
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("proguard-rules.pro")
        multiDexEnabled = true

        lint {
            targetSdk = 28
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    lint {
        abortOnError = false
        disable += "InvalidPackage"
    }

    packaging {
        resources.excludes.add("META-INF/rxjava.properties")
    }

    namespace = "io.requery.android"
}

dependencies {
    implementation(project(":requery"))
    implementation("androidx.sqlite:sqlite:2.0.0")
    implementation("androidx.recyclerview:recyclerview:${libs.versions.androidx.get()}")
    implementation("net.zetetic:android-database-sqlcipher:4.0.1")
    implementation("io.requery:sqlite-android:3.25.3")
    testImplementation("junit:junit:4.12")
    androidTestImplementation(project(":requery-test"))
    androidTestImplementation("androidx.test:runner:1.1.0")
    androidTestImplementation("androidx.test:rules:1.1.0")
    androidTestImplementation("androidx.multidex:multidex:2.0.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${libs.versions.kotlin.get()}")
}

tasks.named("publish") {
    dependsOn("assembleRelease")
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            // leaving out dependencies (components.java) intentionally
            groupId = rootProject.group.toString()
            artifactId = project.name
            version = rootProject.version.toString()
            artifact(getReleaseArtifact())
            pom {
                withXml {
                    asNode().children().add(project.property("pomXml"))
                }
            }
        }
    }
}

fun getReleaseArtifact(): String {
    return "build/outputs/aar/${project.name}-release.aar"
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.srcDirs)
}

tasks.register<Javadoc>("javadoc") {
    source(android.sourceSets.getByName("main").java.srcDirs)
    classpath += project.files(android.bootClasspath.joinToString(File.pathSeparator))
    classpath += configurations.implementation.get()
    isFailOnError = false
}

tasks.register<Jar>("javadocJar") {
    dependsOn("javadoc")
    archiveClassifier.set("javadoc")
    from(tasks.named("javadoc").get())
}

repositories {
    mavenCentral()
}