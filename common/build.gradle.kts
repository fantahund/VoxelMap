plugins {
    id("java")
    id("idea")
    id("fabric-loom") version ("1.7.3")
}

val MINECRAFT_VERSION: String by rootProject.extra
val FABRIC_LOADER_VERSION: String by rootProject.extra
val FABRIC_API_VERSION: String by rootProject.extra

dependencies {
    minecraft(group = "com.mojang", name = "minecraft", version = MINECRAFT_VERSION)
    mappings(loom.layered() {
        officialMojangMappings()
    })
    compileOnly("net.fabricmc:sponge-mixin:0.13.2+mixin.0.8.5")
    compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")

    fun addDependentFabricModule(name: String) {
        val module = fabricApi.module(name, FABRIC_API_VERSION)
        modCompileOnly(module)
    }

    addDependentFabricModule("fabric-api-base")
    addDependentFabricModule("fabric-networking-api-v1")
    addDependentFabricModule("fabric-rendering-v1")
    addDependentFabricModule("fabric-lifecycle-events-v1")
}

sourceSets {

}

loom {
    mixin {
        defaultRefmapName = "voxelmap.refmap.json"
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