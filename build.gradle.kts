import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

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
    maxHeapSize = "2g"
}

tasks.register<JavaExec>("benchmark") {
    group = "verification"
    description = "Run the Grimholt region/world-model microbenchmark."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("dev.grimholt.server.vanilla.VanillaBenchmarkMain")
    args(
        providers.gradleProperty("players").orElse("1000").get(),
        providers.gradleProperty("regions").orElse("50").get(),
        providers.gradleProperty("iterations").orElse("20").get()
    )
}

application {
    mainClass = "dev.grimholt.server.Grimholt"
}

/*
 * Grimholt is distributed as a self-contained executable JAR.
 *
 * Grimholt owns the runtime. The released server artifact must not require
 * Minestom or another Minecraft server implementation at runtime.
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
 * Minestom/Bukkit/Spigot/Paper/Folia are forbidden runtime dependencies.
 */
tasks.register("dependencyAudit") {
    group = "verification"
    description = "Fail if a forbidden server API appears in the runtime dependency graph."

    doLast {
        val forbidden = setOf(
            "net.minestom:minestom",
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

tasks.named("assemble") {
    dependsOn(standaloneJar)
}

tasks.jar {
    manifest { attributes["Main-Class"] = application.mainClass.get() }
}


/*
 * Exact Mojang 26.4-snapshot-3 reference tooling.
 *
 * The checked-in reference/minecraft/26.4/server.jar is the sole vanilla
 * reference input. Grimholt never ships it inside the runtime artifact.
 * Updating Minecraft is intentionally manual: replace the reference JAR,
 * update the pinned checksum/constants, regenerate the reports, then run CI.
 */
val vanillaGeneratedDir = layout.buildDirectory.dir("generated-vanilla/26.4")
val vanillaReferenceJar = layout.projectDirectory.file("reference/minecraft/26.4/server.jar")
val vanillaReferenceUrl = URI("https://piston-data.mojang.com/v1/objects/2d89c95c030e635387448f332961074ce1adbb4b/server.jar")

val generateVanilla26_2 by tasks.registering {
    group = "vanilla"
    description = "Generate exact Minecraft 26.4-snapshot-3 reports from the checked-in reference server.jar."
    val outputDir = vanillaGeneratedDir
    inputs.file(vanillaReferenceJar)
    outputs.dir(outputDir)
    notCompatibleWithConfigurationCache("The data generator launches the checked-in Mojang reference JVM.")

    doLast {
        val version = "26.4-snapshot-3"
        val expectedSha1 = "2d89c95c030e635387448f332961074ce1adbb4b"
        val jar = vanillaReferenceJar.asFile.toPath()
        check(Files.isRegularFile(jar)) { "Missing Minecraft 26.4-snapshot-3 reference JAR: $jar" }
        val actualSha1 = MessageDigest.getInstance("SHA-1")
            .digest(Files.readAllBytes(jar))
            .joinToString("") { "%02x".format(it) }
        check(actualSha1 == expectedSha1) {
            "SHA-1 mismatch for Minecraft 26.4-snapshot-3 reference: expected $expectedSha1, got $actualSha1"
        }

        val workDir = layout.buildDirectory.dir("vanilla-reference/26.4-snapshot-3").get().asFile.toPath()
        if (Files.exists(workDir)) workDir.toFile().deleteRecursively()
        Files.createDirectories(workDir)
        val javaExecutable = Path.of(
            System.getProperty("java.home"), "bin",
            if (System.getProperty("os.name").lowercase().contains("win")) "java.exe" else "java"
        )
        val generatedDir = workDir.resolve("generated")
        val process = ProcessBuilder(
            javaExecutable.toString(),
            "-DbundlerMainClass=net.minecraft.data.Main",
            "-jar", jar.toAbsolutePath().toString(),
            "--all", "--output", generatedDir.toAbsolutePath().toString()
        ).directory(workDir.toFile()).redirectErrorStream(true).start()
        check(process.waitFor(180, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            "Mojang 26.4-snapshot-3 report generation timed out"
        }
        val output = process.inputStream.readBytes().toString(Charsets.UTF_8)
        check(process.exitValue() == 0) {
            "Mojang 26.4-snapshot-3 --all failed (${process.exitValue()}): $output"
        }
        check(Files.isDirectory(generatedDir)) { "Mojang 26.4-snapshot-3 data generator produced no generated directory" }

        val out = outputDir.get().asFile.toPath()
        if (Files.exists(out)) out.toFile().deleteRecursively()
        Files.createDirectories(out)
        generatedDir.toFile().copyRecursively(out.toFile(), overwrite = true)
        out.resolve("manifest.properties").toFile().writeText(
            "version=$version\n" +
            "protocol=1073742165\n" +
            "worldDataVersion=5122\n" +
            "dataPackVersion=123.0\n" +
            "resourcePackVersion=100\n" +
            "javaMajor=25\n" +
            "serverSha1=$expectedSha1\n" +
            "serverPath=reference/minecraft/26.4/server.jar\n"
        )
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateVanilla26_2)
    from(vanillaGeneratedDir) { into("vanilla/26.4") }
}

val verifyVanilla26_2Reference by tasks.registering {
    group = "verification"
    description = "Verify the checked-in Minecraft 26.4-snapshot-3 reference server.jar checksum."
    doLast {
        val jar = vanillaReferenceJar.asFile.toPath()
        check(Files.isRegularFile(jar)) { "Reference jar does not exist: $jar" }
        val actual = MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(jar))
            .joinToString("") { "%02x".format(it) }
        check(actual == "2d89c95c030e635387448f332961074ce1adbb4b") {
            "Minecraft 26.4-snapshot-3 reference SHA-1 mismatch: $actual"
        }
        println("Verified Minecraft 26.4-snapshot-3 reference: $jar")
    }
}

val vanillaReferenceSmoke26_2 by tasks.registering {
    group = "verification"
    notCompatibleWithConfigurationCache("The smoke test launches and manages an external JVM process.")
    description = "Boot the exact checked-in Mojang 26.4-snapshot-3 server.jar and verify clean startup/shutdown."
    dependsOn(verifyVanilla26_2Reference)
    doLast {
        val jar = vanillaReferenceJar.asFile.toPath()
        val work = layout.buildDirectory.dir("vanilla-reference-smoke/26.4-snapshot-3").get().asFile.toPath()
        if (Files.exists(work)) work.toFile().deleteRecursively()
        Files.createDirectories(work)
        Files.writeString(work.resolve("eula.txt"), "eula=true\n")
        Files.writeString(work.resolve("server.properties"),
            "online-mode=false\n" +
            "server-port=0\n" +
            "server-ip=127.0.0.1\n" +
            "enable-query=false\n" +
            "enable-rcon=false\n" +
            "spawn-protection=0\n")
        val javaExecutable = Path.of(
            System.getProperty("java.home"), "bin",
            if (System.getProperty("os.name").lowercase().contains("win")) "java.exe" else "java"
        )
        val process = ProcessBuilder(
            javaExecutable.toString(), "-Xms512M", "-Xmx2G", "-jar", jar.toAbsolutePath().toString(), "--nogui"
        ).directory(work.toFile()).redirectErrorStream(true).start()
        val output = StringBuilder()
        val reader = Thread {
            process.inputStream.bufferedReader().useLines { lines -> lines.forEach { output.append(it).append('\n') } }
        }
        reader.start()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(90)
        var ready = false
        while (System.nanoTime() < deadline && process.isAlive) {
            val text = output.toString()
            if (text.contains("Done (")) { ready = true; break }
            Thread.sleep(250)
        }
        if (!ready) {
            process.destroyForcibly()
            process.waitFor(10, TimeUnit.SECONDS)
            reader.join(2000)
            error("Minecraft 26.4-snapshot-3 reference server did not reach ready state. Output:\n$output")
        }
        process.destroy()
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            process.waitFor(10, TimeUnit.SECONDS)
        }
        reader.join(5000)
        check(!process.isAlive) { "Minecraft 26.4-snapshot-3 reference server did not terminate. Output:\n$output" }
    }
}
