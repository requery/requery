import java.util.Properties

plugins {
    java
    `maven-publish`
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath(group = "com.android.tools.build", name = "gradle", version = libs.versions.android.gradle.plugin.get())
        classpath(kotlin("gradle-plugin", version = libs.versions.kotlin.get()))
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

group = "io.requery"
version = "1.6.1"
description = "A light but powerful object mapper and SQL generator for Java/Android"

val properties = Properties()
val localProperties = project.rootProject.file("local.properties")
if (localProperties.exists()) {
    properties.load(localProperties.inputStream())
}

configure(listOf(
        project(":requery"),
        project(":requery-processor"),
        project(":requery-kotlin"),
        project(":requery-jackson")
)) {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).links("http://docs.oracle.com/javase/8/docs/api/")
    }

    val sourcesJar by tasks.registering(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
        dependsOn(tasks.classes)
    }

    val javadocJar by tasks.registering(Jar::class) {
        archiveClassifier.set("javadoc")
        from(tasks.javadoc)
        dependsOn(tasks.javadoc)
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                groupId = rootProject.group.toString()
                artifactId = project.name
                version = rootProject.version.toString()

                artifact(sourcesJar)
                artifact(javadocJar)

                pom {
                    name.set(project.name)
                    description.set(project.description ?: rootProject.description)
                    url.set("https://github.com/requery/requery")
                    scm {
                        url.set("https://github.com/requery/requery.git")
                        connection.set("scm:git:git://github.com/requery/requery.git")
                        developerConnection.set("scm:git:git@github.com/requery/requery.git")
                    }
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("http://www.apache.org/license/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }
                    developers {
                        developer {
                            id.set("npurushe")
                            name.set("Nikhil Purushe")
                        }
                    }
                }
            }
        }
    }
}