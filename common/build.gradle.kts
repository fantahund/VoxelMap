plugins {
    id("java")
    id("idea")
    id("fabric-loom") version ("1.13-SNAPSHOT")
}

val MINECRAFT_VERSION: String by rootProject.extra
val FABRIC_LOADER_VERSION: String by rootProject.extra
val FABRIC_API_VERSION: String by rootProject.extra

repositories {
    maven { url = uri("https://api.modrinth.com/maven") }
}

dependencies {
    minecraft(group = "com.mojang", name = "minecraft", version = MINECRAFT_VERSION)
    mappings(loom.layered() {
        officialMojangMappings()
    })
    compileOnly("net.fabricmc:sponge-mixin:0.16.4+mixin.0.8.7")
    modCompileOnly("net.fabricmc:fabric-loader:$FABRIC_LOADER_VERSION")

    compileOnly("io.github.llamalad7:mixinextras-common:0.5.0")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.0")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${FABRIC_API_VERSION}")

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