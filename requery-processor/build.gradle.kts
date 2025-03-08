import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow") version "8.3.6"
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(project(":requery"))
    implementation("org.eclipse.persistence:javax.persistence:${libs.versions.jpa.get()}")
    implementation("com.squareup:javapoet:1.11.1")
}

tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        include(dependency("com.squareup:javapoet:.*"))
    }
    relocate("com.squareup", "io.requery.com.squareup")
    archiveFileName.set("${project.name}.jar")
}

tasks.named("jar") {
    finalizedBy("shadowJar")
}