plugins {
    java
    kotlin("jvm")
    kotlin("kapt")
}

sourceSets {
    create("generated") {
        java.srcDir("${layout.buildDirectory.get()}/generated/source/kapt/main/")
    }
}

dependencies {
    implementation(project(":requery"))
    implementation(project(":requery-kotlin"))
    kapt(project(":requery-processor"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}")
    implementation("junit:junit:4.12")
    testImplementation("com.h2database:h2:1.4.191")
    testImplementation("io.reactivex.rxjava2:rxjava:${libs.versions.rxjava2.get()}")
    testImplementation("io.reactivex.rxjava3:rxjava:${libs.versions.rxjava3.get()}")
}

tasks.withType<org.jetbrains.kotlin.gradle.internal.KaptTask>().configureEach {
    dependsOn(":requery-processor:shadowJar")
}