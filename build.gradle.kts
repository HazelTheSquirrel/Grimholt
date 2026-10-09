import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import java.util.zip.ZipFile

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
        val forbiddenGroups = listOf(
            "net.minestom", "org.bukkit", "org.spigotmc", "io.papermc.paper",
            "dev.folia", "org.purpurmc", "com.velocitypowered"
        )

        val resolved = configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts
            .map { "${it.moduleVersion.id.group}:${it.moduleVersion.id.name}" }
            .toSet()

        val violations = resolved.filter { coordinate ->
            val group = coordinate.substringBefore(':')
            forbiddenGroups.any { group == it || group.startsWith(it + ".") }
        }.sorted()

        check(violations.isEmpty()) {
            "Forbidden Minecraft server implementation/API dependency detected: " + violations.joinToString()
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

val downloadVanilla26_4Reference by tasks.registering {
    group = "vanilla"
    description = "Download the exact Mojang Minecraft 26.4-snapshot-3 reference JAR when it is not checked in."
    outputs.file(vanillaReferenceJar)
    doLast {
        val jar = vanillaReferenceJar.asFile.toPath()
        if (!Files.isRegularFile(jar)) {
            Files.createDirectories(jar.parent)
            vanillaReferenceUrl.toURL().openStream().use { input ->
                Files.copy(input, jar, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        val expected = "2d89c95c030e635387448f332961074ce1adbb4b"
        val actual = MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(jar))
            .joinToString("") { "%02x".format(it) }
        check(actual == expected) { "Minecraft 26.4-snapshot-3 reference SHA-1 mismatch: expected $expected, got $actual" }
    }
}

val generateVanilla26_4 by tasks.registering {
    group = "vanilla"
    dependsOn(downloadVanilla26_4Reference)
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
            "version=26.4-snapshot-3\n" +
            "protocol=1073742165\n" +
            "worldDataVersion=5122\n" +
            "dataPackVersion=123.0\n" +
            "resourcePackVersion=100.0\n" +
            "javaMajor=25\n" +
            "serverSha1=$expectedSha1\n" +
            "serverPath=reference/minecraft/26.4/server.jar\n"
        )

        // Persist a deterministic index of vanilla tag files generated from
        // the reference JAR. Runtime code consumes this index instead of guessing
        // which tags exist in the target snapshot.
        val tagsRoot = out.resolve("data/minecraft/tags")
        val tagFiles = if (Files.isDirectory(tagsRoot)) {
            Files.walk(tagsRoot).use { stream ->
                stream.filter(Files::isRegularFile)
                    .map { out.relativize(it).toString().replace('\\', '/') }
                    .filter { it.endsWith(".json") }
                    .sorted()
                    .toList()
            }
        } else emptyList()
        out.resolve("reports/tag_files.json").toFile().writeText(
            "[" + tagFiles.joinToString(",") { "\"$it\"" } + "]\n"
        )

        // A sorted content index makes the generated data auditable and allows
        // CI to detect stale or nondeterministic reports without checking in
        // thousands of generated JSON files.
        val requiredReports = listOf(
            "reports/blocks.json",
            "reports/registries.json",
            "reports/packets.json",
            "reports/tag_files.json"
        )
        requiredReports.forEach { relative ->
            val file = out.resolve(relative)
            check(Files.isRegularFile(file) && Files.size(file) > 2L) {
                "Required 26.4-snapshot-3 report is missing or empty: $relative"
            }
        }
        val reportIndex = out.resolve("reports/SHA256SUMS")
        val digest = MessageDigest.getInstance("SHA-256")
        val indexLines = Files.walk(out).use { stream ->
            stream.filter(Files::isRegularFile)
                .filter { it != reportIndex }
                .sorted()
                .map { file ->
                    val hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))
                        .joinToString("") { "%02x".format(it) }
                    "$hash  ${out.relativize(file).toString().replace('\\', '/')}"
                }
                .toList()
        }
        Files.writeString(reportIndex, indexLines.joinToString("\n", postfix = "\n"))
    }
}

val verifyVanilla26_4Reports by tasks.registering {
    group = "verification"
    description = "Verify mandatory reports and the deterministic SHA-256 index from Mojang 26.4-snapshot-3."
    dependsOn(generateVanilla26_4)
    doLast {
        val root = vanillaGeneratedDir.get().asFile.toPath()
        val sums = root.resolve("reports/SHA256SUMS")
        check(Files.isRegularFile(sums)) { "Generated report checksum index is missing" }
        val lines = Files.readAllLines(sums)
        check(lines.isNotEmpty()) { "Generated report checksum index is empty" }
        check(lines.map { it.substringAfter("  ") } == lines.map { it.substringAfter("  ") }.sorted()) { "Generated report checksum index is not sorted/deterministic" }
        val seen = HashSet<String>()
        lines.forEach { line ->
            val match = Regex("^([0-9a-f]{64})  (.+)$").matchEntire(line)
                ?: error("Malformed generated report checksum line: $line")
            val relative = match.groupValues[2]
            check(seen.add(relative)) { "Duplicate path in generated report checksum index: $relative" }
            val file = root.resolve(relative).normalize()
            check(file.startsWith(root) && Files.isRegularFile(file)) { "Indexed report is missing or escapes data root: $relative" }
            val actual = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))
                .joinToString("") { "%02x".format(it) }
            check(actual == match.groupValues[1]) { "Generated report checksum mismatch: $relative" }
        }
        listOf("reports/blocks.json", "reports/registries.json",
            "reports/packets.json", "reports/tag_files.json").forEach { relative ->
            check(Files.size(root.resolve(relative)) > 2L) { "Required report is empty: $relative" }
        }
        println("Verified ${lines.size} deterministic generated files for 26.4-snapshot-3")
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(verifyVanilla26_4Reports)
    from(vanillaGeneratedDir) { into("vanilla/26.4") }
}

val verifyVanilla26_4Reference by tasks.registering {
    group = "verification"
    dependsOn(downloadVanilla26_4Reference)
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

val vanillaReferenceSmoke26_4 by tasks.registering {
    group = "verification"
    notCompatibleWithConfigurationCache("The smoke test launches and manages an external JVM process.")
    description = "Boot the exact checked-in Mojang 26.4-snapshot-3 server.jar and verify clean startup/shutdown."
    dependsOn(verifyVanilla26_4Reference)
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


/*
 * Smoke-test the actual Grimholt distribution, not just the Mojang reference.
 * The temporary working directory prevents local configs/plugins/worlds from
 * masking missing runtime inputs. Port 0 avoids collisions on CI runners.
 */
val standaloneSmoke by tasks.registering {
    group = "verification"
    description = "Launch the built Grimholt standalone JAR and verify ready + graceful shutdown."
    dependsOn(standaloneJar)
    notCompatibleWithConfigurationCache("The smoke test launches and manages an external JVM process.")
    doLast {
        val artifact = standaloneJar.get().archiveFile.get().asFile.toPath()
        check(Files.isRegularFile(artifact)) { "Standalone artifact missing: $artifact" }
        val work = layout.buildDirectory.dir("standalone-smoke").get().asFile.toPath()
        if (Files.exists(work)) work.toFile().deleteRecursively()
        Files.createDirectories(work)
        val world = work.resolve("world").toAbsolutePath().toString().replace("\\", "/")
        val config = work.resolve("grimholt.properties")
        Files.writeString(config,
            "config-version=2\n" +
            "bind-address=127.0.0.1\n" +
            "port=0\n" +
            "online-mode=false\n" +
            "max-players=10\n" +
            "dispatcher-threads=2\n" +
            "view-distance=4\n" +
            "simulation-distance=4\n" +
            "world-directory=$world\n")
        val javaExecutable = Path.of(
            System.getProperty("java.home"), "bin",
            if (System.getProperty("os.name").lowercase().contains("win")) "java.exe" else "java"
        )
        val launcher = ProcessBuilder(
            javaExecutable.toString(), "-Xms128M", "-Xmx512M", "-jar",
            artifact.toAbsolutePath().toString(), config.toAbsolutePath().toString()
        ).directory(work.toFile()).redirectErrorStream(true)
        val process = launcher.start()
        val output = StringBuilder()
        val reader = Thread {
            try {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line -> synchronized(output) { output.append(line).append('\n') } }
                }
            } catch (_: java.io.IOException) {
                // Process teardown may close the pipe while the reader is blocked.
            }
        }
        reader.isDaemon = true
        reader.start()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45)
        var ready = false
        while (System.nanoTime() < deadline && process.isAlive) {
            synchronized(output) {
                if (output.contains("Grimholt is running")) ready = true
            }
            if (ready) break
            Thread.sleep(100)
        }
        if (!ready) {
            process.destroyForcibly()
            process.waitFor(10, TimeUnit.SECONDS)
            reader.join(2000)
            error("Grimholt standalone JAR did not reach ready state. Output:\n$output")
        }
        // Use Grimholt's explicit console command rather than depending on
        // SIGTERM handling, which varies across JVM launchers and CI hosts.
        Thread.sleep(500)
        process.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write("stop\n")
            writer.flush()
        }
        if (!process.waitFor(15, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            process.waitFor(10, TimeUnit.SECONDS)
        }
        reader.join(5000)
        check(!process.isAlive) { "Grimholt standalone JAR did not terminate. Output:\n$output" }
        check(output.contains("Grimholt main waiting for shutdown signal")) {
            "Standalone process did not enter its managed lifetime wait; likely stale or incorrect entry-point bytecode. Output:\n$output"
        }
        synchronized(output) {
            check(output.contains("Grimholt shutdown hook entered")) {
                "JVM exited without entering Grimholt's shutdown hook (exit=${process.exitValue()}). " +
                    "Check signal handling / injected JVM options. Output:\n$output"
            }
            check(output.contains("Grimholt stopped")) {
                "Grimholt shutdown hook ran but server lifecycle did not confirm graceful shutdown " +
                    "(exit=${process.exitValue()}). Output:\n$output"
            }
        }
        println("Grimholt standalone smoke passed: ready state and graceful shutdown confirmed.")
    }
}


/*
 * Release baseline integrity gate. This complements dependency resolution by
 * checking source imports and the actual self-contained artifact.
 */
val forkIntegrityAudit by tasks.registering {
    group = "verification"
    description = "Audit forbidden server implementation imports and standalone JAR contents."
    dependsOn("dependencyAudit", standaloneJar)

    doLast {
        val forbiddenRoots = listOf(
            "net.minestom", "org.bukkit", "org.spigotmc", "io.papermc.paper",
            "dev.folia", "org.purpurmc", "com.velocitypowered"
        )
        val importPattern = Regex("^\\s*import\\s+(?:static\\s+)?(" +
            forbiddenRoots.joinToString("|") { Regex.escape(it) } + ")(?:\\.|;)")
        val sourceRoot = layout.projectDirectory.dir("src/main/java").asFile.toPath()
        val sourceViolations = Files.walk(sourceRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".java") }
                .flatMap { file ->
                    Files.readAllLines(file).withIndex()
                        .filter { (_, line) -> importPattern.containsMatchIn(line) }
                        .map { (lineNumber, line) ->
                            file.toString() + ":" + (lineNumber + 1) + ": " + line.trim()
                        }.stream()
                }.toList()
        }
        check(sourceViolations.isEmpty()) {
            "Forbidden Minecraft implementation imports found:\\n" + sourceViolations.joinToString("\\n")
        }

        val artifact = standaloneJar.get().archiveFile.get().asFile.toPath()
        check(Files.isRegularFile(artifact)) { "Standalone artifact missing: " + artifact }
        ZipFile(artifact.toFile()).use { zip ->
            val entries = zip.entries().asSequence().map { it.name }.toList()
            val forbiddenPrefixes = forbiddenRoots.map { it.replace('.', '/') + "/" }
            val forbiddenEntries = entries.filter { entry ->
                forbiddenPrefixes.any { entry.startsWith(it) } ||
                    entry.startsWith("reference/minecraft/") ||
                    entry.endsWith("/server.jar")
            }
            check(forbiddenEntries.isEmpty()) {
                "Standalone artifact contains forbidden implementation/reference entries: " + forbiddenEntries.joinToString()
            }
            check("dev/grimholt/server/Grimholt.class" in entries) {
                "Standalone artifact does not contain Grimholt's application entry point"
            }
            val manifest = zip.getEntry("META-INF/MANIFEST.MF")
            check(manifest != null) { "Standalone artifact has no manifest" }
            val manifestText = zip.getInputStream(manifest).bufferedReader(Charsets.UTF_8).use { it.readText() }
            check(Regex("""(?m)^Main-Class: dev\.grimholt\.server\.Grimholt\s*$""").containsMatchIn(manifestText)) {
                "Standalone artifact manifest does not name the Grimholt entry point"
            }
        }
        println("Fork integrity audit passed: source imports, runtime graph and standalone JAR contents.")
    }
}
