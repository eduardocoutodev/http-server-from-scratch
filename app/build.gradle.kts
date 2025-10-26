plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    application
}

group = "com.eduardocoutodev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.25")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}


kotlin {
    jvmToolchain(23)
}

application {
    mainClass.set("AppKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "AppKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}