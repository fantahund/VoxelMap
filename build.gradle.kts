plugins {
    id("java")
    id("fabric-loom") version ("1.11-SNAPSHOT") apply (false)
}

val MINECRAFT_VERSION by extra { "1.21.9" }
val NEOFORGE_VERSION by extra { "21.5.47-beta" }
val FABRIC_LOADER_VERSION by extra { "0.17.2" }
val FABRIC_API_VERSION by extra { "0.134.0+1.21.9" }
val VOXELMAP_VERSION by extra { "1.15.8" }

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

    java.toolchain.languageVersion = JavaLanguageVersion.of(21)

    tasks.processResources {
        filesMatching("META-INF/neoforge.mods.toml") {
            expand(mapOf("version" to MOD_VERSION))
        }
    }

    version = MOD_VERSION
    group = "com.mamiyaotaru"

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<GenerateModuleMetadata>().configureEach {
        enabled = false
    }
}
