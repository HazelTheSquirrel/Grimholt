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
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = libs.versions.java.get().toInt()
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass = "dev.grimholt.server.Grimholt"
}

/*
 * Grimholt is distributed as a self-contained executable JAR.
 *
 * Minestom remains an internal implementation dependency, but the released
 * server artifact must not require the operator to install a separate
 * Minestom/Minecraft server JAR. Runtime dependencies are folded into the
 * executable artifact; Grimholt owns the actual server lifecycle and API.
 */
val standaloneJar by tasks.registering(Jar::class) {
    group = "distribution"
    description = "Build the self-contained Grimholt server JAR."

    archiveClassifier.set("standalone")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    from(sourceSets.main.get().output)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })

    exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
}

/*
 * Dependency lockdown is a build-time guard, not merely documentation.
 * Minestom is explicitly allowed; Bukkit/Spigot/Paper/Folia are forbidden.
 */
tasks.register("dependencyAudit") {
    group = "verification"
    description = "Fail if a forbidden server API appears in the runtime dependency graph."

    doLast {
        val forbidden = setOf(
            "org.bukkit:bukkit",
            "org.spigotmc:spigot-api",
            "io.papermc.paper:paper-api",
            "dev.folia:folia-api"
        )

        val resolved = configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts
            .map { "${it.moduleVersion.id.group}:${it.moduleVersion.id.name}" }
            .toSet()

        val violations = resolved.filter { coordinate ->
            forbidden.any { coordinate == it || coordinate.startsWith("$it:") }
        }.sorted()

        check(violations.isEmpty()) {
            "Forbidden Minecraft server API dependency detected: ${violations.joinToString()}"
        }
    }
}

tasks.named("check") {
    dependsOn("dependencyAudit")
}

tasks.jar {
    manifest { attributes["Main-Class"] = application.mainClass.get() }
}
