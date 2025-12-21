plugins {
    id("java")
    id("net.fabricmc.fabric-loom") version ("1.14-SNAPSHOT") apply (false)
}

val MINECRAFT_VERSION by extra { "26.1-snapshot-1" }
val NEOFORGE_VERSION by extra { "21.5.47-beta" }
val FABRIC_LOADER_VERSION by extra { "0.18.3" }
val FABRIC_API_VERSION by extra { "0.140.1+26.1" }
val VOXELMAP_VERSION by extra { "1.15.11" }

val MOD_VERSION by extra { "$MINECRAFT_VERSION-$VOXELMAP_VERSION" }

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.jar {
    enabled = false
}

subprojects {
    apply(plugin = "maven-publish")

    java.toolchain.languageVersion = JavaLanguageVersion.of(25)

    tasks.processResources {
        filesMatching("META-INF/neoforge.mods.toml") {
            expand(mapOf("version" to MOD_VERSION))
        }
    }

    version = MOD_VERSION
    group = "com.mamiyaotaru"

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    tasks.withType<GenerateModuleMetadata>().configureEach {
        enabled = false
    }
}
