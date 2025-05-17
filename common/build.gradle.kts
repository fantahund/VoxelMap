plugins {
    id("java")
    id("idea")
    id("fabric-loom") version ("1.10-SNAPSHOT")
}

val MINECRAFT_VERSION: String by rootProject.extra
val FABRIC_LOADER_VERSION: String by rootProject.extra
val FABRIC_API_VERSION: String by rootProject.extra
val IRIS_VERSION: String by rootProject.extra

repositories {
    maven { url = uri("https://api.modrinth.com/maven") }
}

dependencies {
    minecraft(group = "com.mojang", name = "minecraft", version = MINECRAFT_VERSION)
    mappings(loom.layered() {
        officialMojangMappings()
    })
    compileOnly("net.fabricmc:sponge-mixin:0.15.3+mixin.0.8.7")

    compileOnly("io.github.llamalad7:mixinextras-common:0.4.1")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.4.1")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${FABRIC_API_VERSION}")
    //MC Verison modCompileOnly("maven.modrinth:iris:${IRIS_VERSION}+${MINECRAFT_VERSION}-fabric")
    modCompileOnly("maven.modrinth:iris:${IRIS_VERSION}+1.21.5-fabric")
}

sourceSets {

}

loom {
    @Suppress("UnstableApiUsage")
    mixin {
        defaultRefmapName.set("voxelmap.refmap.json")
        useLegacyMixinAp = false
    }

    accessWidenerPath = file("src/main/resources/voxelmap.accesswidener")

    mods {
        val main by creating { // to match the default mod generated for Forge
            sourceSet("main")
        }
    }
}

tasks {
    jar {
        from(rootDir.resolve("LICENSE.md"))
    }
}

// This trick hides common tasks in the IDEA list.
tasks.configureEach {
    group = null
}