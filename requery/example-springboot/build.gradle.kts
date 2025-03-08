plugins {
    java
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.requery.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation(project(":requery"))
    implementation(project(":requery-jackson"))
    annotationProcessor(project(":requery-processor"))
    implementation("junit:junit:4.12")
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(":requery-processor:shadowJar")
}