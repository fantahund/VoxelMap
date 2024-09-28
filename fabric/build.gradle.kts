plugins {
    id("java")
    id("idea")
    id("fabric-loom") version ("1.7.3")
}

val MINECRAFT_VERSION: String by rootProject.extra
val FABRIC_LOADER_VERSION: String by rootProject.extra
val FABRIC_API_VERSION: String by rootProject.extra
val MOD_VERSION: String by rootProject.extra

base {
    archivesName.set("voxelmap-fabric")
}

dependencies {
    minecraft("com.mojang:minecraft:${MINECRAFT_VERSION}")
    mappings(loom.layered {
        officialMojangMappings()
    })
    modImplementation("net.fabricmc:fabric-loader:$FABRIC_LOADER_VERSION")

    fun addEmbeddedFabricModule(name: String) {
        val module = fabricApi.module(name, FABRIC_API_VERSION)
        modImplementation(module)
        include(module)
    }

    implementation(project.project(":common").sourceSets.getByName("main").output)
}

tasks.named("compileTestJava").configure {
    enabled = false
}

tasks.named("test").configure {
    enabled = false
}

loom {
    if (project(":common").file("src/main/resources/voxelmap.accesswidener").exists())
        accessWidenerPath.set(project(":common").file("src/main/resources/voxelmap.accesswidener"))

    @Suppress("UnstableApiUsage")
    mixin { defaultRefmapName.set("voxelmap-voxelmap.refmap.json") }

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
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to project.version))
        }
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(zipTree(project.project(":common").tasks.jar.get().archiveFile))
    }

    remapJar.get().destinationDirectory = rootDir.resolve("build").resolve("libs")
}