plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

dependencies {
    compileOnly("javax.transaction:javax.transaction-api:1.2")
    compileOnly("javax.cache:cache-api:1.0.0")
    compileOnly("io.reactivex.rxjava2:rxjava:${libs.versions.rxjava2.get()}")
    compileOnly("io.projectreactor:reactor-core:${libs.versions.reactor.get()}")
    compileOnly("com.google.code.findbugs:jsr305:3.0.1")
}

tasks.named<Javadoc>("javadoc") {
    classpath += configurations.compileOnly.get()
}