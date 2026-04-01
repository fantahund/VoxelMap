plugins {
    id("java")
    id("idea")
    id("net.fabricmc.fabric-loom")
}

val minecraftVersion: String by rootProject.extra
val fabricVersion: String by rootProject.extra
val fabricApiVersion: String by rootProject.extra

val fullVersion: String by rootProject.extra

base {
    archivesName.set("voxelmap-fabric")
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")

    implementation("net.fabricmc:fabric-loader:${fabricVersion}")
    implementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}")

    implementation(project.project(":common").sourceSets.getByName("main").output)
}

tasks.named("compileTestJava").configure {
    enabled = false
}

tasks.named("test").configure {
    enabled = false
}

tasks.named("validateAccessWidener") {
    mustRunAfter(project(":common").tasks.named("genSourcesWithVineflower"))
}

loom {
    if (project(":common").file("src/main/resources/voxelmap.accesswidener").exists())
        accessWidenerPath.set(project(":common").file("src/main/resources/voxelmap.accesswidener"))

    runs {
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}

tasks {
    processResources {
        from(project.project(":common").sourceSets.main.get().resources)
        inputs.property("version", fullVersion)

        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to fullVersion))
        }
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(zipTree(project.project(":common").tasks.jar.get().archiveFile))
    }

    jar.get().destinationDirectory = rootDir.resolve("build").resolve("libs")
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }

    repositories {
        maven("file://${System.getenv("local_maven")}")
    }
}