plugins {
    java
    kotlin("jvm")
}

dependencies {
    implementation(project(":requery"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}")
    runtimeOnly("io.reactivex.rxjava2:rxjava:${libs.versions.rxjava2.get()}")
    runtimeOnly("io.projectreactor:reactor-core:${libs.versions.reactor.get()}")
}

sourceSets {
    main {
        java {
            srcDirs("src/main/kotlin")
        }
    }
}