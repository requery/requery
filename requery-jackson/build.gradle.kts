plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":requery"))
    implementation("com.fasterxml.jackson.core:jackson-core:${libs.versions.jackson.get()}")
    implementation("com.fasterxml.jackson.core:jackson-annotations:${libs.versions.jackson.get()}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${libs.versions.jackson.get()}")
}