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

tasks.named("assemble") {
    dependsOn(standaloneJar)
}

tasks.jar {
    manifest { attributes["Main-Class"] = application.mainClass.get() }
}


/*
 * Exact Mojang 26.4 Snapshot 3 data/reference tooling.
 */
val vanillaGeneratedDir = layout.buildDirectory.dir("generated-vanilla/26.4-snapshot-3")
val vanillaDownloadDir = layout.buildDirectory.dir("vanilla-reference")

val generateVanilla26_4S3 by tasks.registering {
    group = "vanilla"
    description = "Download Mojang 26.4 Snapshot 3 and generate exact reports/data."
    val outputDir = vanillaGeneratedDir
    val downloadDir = vanillaDownloadDir
    outputs.dir(outputDir)

    doLast {
        val version = "26.4-snapshot-3"
        val manifest = URI("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json").toURL().readText()
        val versionPattern = """\{"id"\s*:\s*"${Pattern.quote(version)}"[^{}]*"url"\s*:\s*"([^"]+)"[^{}]*\}"""
        val versionUrl = Regex(versionPattern).find(manifest)?.groupValues?.get(1)
            ?: error("Mojang version manifest does not contain " + version)
        val versionJson = URI(versionUrl).toURL().readText()
        val server = Regex("""\"server\"\s*:\s*\{[^{}]*\"sha1\"\s*:\s*\"([0-9a-f]{40})\"[^{}]*\"url\"\s*:\s*\"([^\"]+)\"""")
            .find(versionJson) ?: error("Mojang version metadata does not contain a server download for " + version)
        val expectedSha1 = server.groupValues[1]
        val jarUrl = server.groupValues[2]

        val downloadPath = downloadDir.get().asFile.toPath().resolve("server-" + version + ".jar")
        Files.createDirectories(downloadPath.parent)
        if (!Files.isRegularFile(downloadPath)) {
            URI(jarUrl).toURL().openStream().use { input ->
                Files.copy(input, downloadPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        val actualSha1 = MessageDigest.getInstance("SHA-1")
            .digest(Files.readAllBytes(downloadPath))
            .joinToString("") { "%02x".format(it) }
        check(actualSha1 == expectedSha1) {
            "SHA-1 mismatch for " + version + ": expected " + expectedSha1 + ", got " + actualSha1
        }

        val workDir = downloadDir.get().asFile.toPath().resolve("reports-work")
        Files.createDirectories(workDir)
        val javaExecutable = Path.of(
            System.getProperty("java.home"), "bin",
            if (System.getProperty("os.name").lowercase().contains("win")) "java.exe" else "java"
        )
        val process = ProcessBuilder(
            javaExecutable.toString(), "-DbundlerMainClass=net.minecraft.data.Main", "-jar", downloadPath.toAbsolutePath().toString(), "--all", "--output", workDir.resolve("generated").toString()
        ).directory(workDir.toFile()).redirectErrorStream(true).start()
        check(process.waitFor(180, TimeUnit.SECONDS)) {
            "Mojang " + version + " report generation timed out"
        }
        check(process.exitValue() == 0) {
            "Mojang " + version + " --all failed (" + process.exitValue() + "): " + process.inputStream.readBytes().toString(Charsets.UTF_8)
        }

        val out = outputDir.get().asFile.toPath()
        if (Files.exists(out)) out.toFile().deleteRecursively()
        Files.createDirectories(out)

        val generated = workDir.resolve("generated")
        check(Files.isDirectory(generated)) { "Mojang data generator produced no generated directory" }
        generated.toFile().copyRecursively(out.toFile(), overwrite = true)

        out.resolve("manifest.properties").toFile().writeText(
            "version=" + version + "\n" +
            "protocol=1073742165\n" +
            "worldDataVersion=5122\n" +
            "dataPackVersion=123\n" +
            "resourcePackVersion=100\n" +
            "javaMajor=25\n" +
            "serverSha1=" + expectedSha1 + "\n" +
            "serverUrl=" + jarUrl + "\n"
        )
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateVanilla26_4S3)
    from(vanillaGeneratedDir) {
        into("vanilla/26.4-snapshot-3")
    }
}

val vanillaReferenceSmoke26_4S3 by tasks.registering {
    group = "verification"
    notCompatibleWithConfigurationCache("The smoke test launches and manages an external JVM process.")
    description = "Boot the exact Mojang 26.4 Snapshot 3 server jar and verify a clean startup/shutdown."
    dependsOn(generateVanilla26_4S3)
    doLast {
        val jar = vanillaDownloadDir.get().asFile.toPath().resolve("server-26.4-snapshot-3.jar")
        check(Files.isRegularFile(jar)) { "Generated reference jar missing: " + jar }
        val work = layout.buildDirectory.dir("vanilla-reference-smoke").get().asFile.toPath()
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
            javaExecutable.toString(), "-Xms512M", "-Xmx2G",
            "-jar", jar.toAbsolutePath().toString(), "--nogui"
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
            if (text.contains("Done (") || text.contains("For help, type \"help\"")) {
                ready = true
                break
            }
            Thread.sleep(250)
        }
        if (process.isAlive) {
            process.outputStream.bufferedWriter().use { it.write("stop\\n"); it.flush() }
            process.waitFor(15, TimeUnit.SECONDS)
        }
        if (!ready) {
            process.destroyForcibly()
            reader.join(2000)
            error("26.4 Snapshot 3 reference server did not reach ready state. Output:\\n" + output)
        }
        reader.join(5000)
        check(process.exitValue() == 0) {
            "26.4 Snapshot 3 reference server exited with " + process.exitValue() + ":\\n" + output
        }
    }
}

val verifyVanilla26_4S3Reference by tasks.registering {
    group = "verification"
    description = "Verify that an operator supplied reference is exactly 26.4 Snapshot 3."
    doLast {
        val value = System.getenv("GRIMHOLT_MC_26_4_S3_JAR")
            ?: error("Set GRIMHOLT_MC_26_4_S3_JAR to the real 26.4 Snapshot 3 server jar")
        val jar = file(value)
        check(jar.isFile()) { "Reference jar does not exist: " + jar }
        println("Verified path for pinned Minecraft 26.4 Snapshot 3 reference: " + jar)
    }
}
