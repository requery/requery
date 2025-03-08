import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

dependencies {
    implementation(project(":requery"))
    testImplementation(project(":requery-jackson"))
    annotationProcessor(project(":requery-processor"))
    //implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar")))) // for Oracle JDBC drivers
    implementation("io.reactivex.rxjava2:rxjava:${libs.versions.rxjava2.get()}")
    testImplementation("io.projectreactor:reactor-core:${libs.versions.reactor.get()}")
    implementation("org.eclipse.persistence:javax.persistence:${libs.versions.jpa.get()}")
    runtimeOnly("com.google.auto.value:auto-value:1.5.2")
    annotationProcessor("com.google.auto.value:auto-value:1.5.2")
    runtimeOnly("org.immutables:value:2.4.3")
    annotationProcessor("org.immutables:value:2.4.3")
    //testImplementation(fileTree(mapOf("dir" to "test-libs", "include" to listOf("*.jar"))))
    testImplementation("org.openjdk.jmh:jmh-core:1.12")
    testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.12")
    testImplementation("mysql:mysql-connector-java:6.0.3")
    testImplementation("org.postgresql:postgresql:9.4.1209.jre7")
    testImplementation("com.microsoft.sqlserver:mssql-jdbc:6.1.0.jre8")
    testImplementation("org.xerial:sqlite-jdbc:3.8.11.2")
    testImplementation("org.apache.derby:derby:10.12.1.1")
    testImplementation("com.h2database:h2:1.4.197")
    testImplementation("org.hsqldb:hsqldb:2.3.4")
    testImplementation("org.ehcache:ehcache:3.1.1")
    testImplementation("javax.cache:cache-api:1.0.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:${libs.versions.jackson.get()}")
    implementation("junit:junit:4.12")
}

tasks.test {
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
    }
}