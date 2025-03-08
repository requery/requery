plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    compileOnly("jakarta.transaction:jakarta.transaction-api:2.0.0")
    compileOnly("javax.cache:cache-api:1.0.0")
    compileOnly("io.reactivex.rxjava2:rxjava:${libs.versions.rxjava2.get()}")
    compileOnly("io.projectreactor:reactor-core:${libs.versions.reactor.get()}")
    compileOnly("com.google.code.findbugs:jsr305:3.0.1")
}

tasks.named<Javadoc>("javadoc") {
    classpath += configurations.compileOnly.get()
}