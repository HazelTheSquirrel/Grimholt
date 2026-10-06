plugins {
    java
    application
}

group = "dev.grimholt"
version = "0.1.0-SNAPSHOT"

repositories { mavenCentral() }

java {
    toolchain { languageVersion = JavaLanguageVersion.of(libs.versions.java.get()) }
    withSourcesJar()
}

dependencies {
    implementation(libs.minestom)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.slf4j.simple)
    testImplementation(libs.junit)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = libs.versions.java.get().toInt()
}

tasks.test { useJUnitPlatform() }

application { mainClass = "dev.grimholt.server.Grimholt" }

tasks.jar {
    manifest { attributes["Main-Class"] = application.mainClass.get() }
}
