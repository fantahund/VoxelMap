plugins {
    id("java")
    id("fabric-loom") version ("1.10-SNAPSHOT") apply (false)
}

val MINECRAFT_VERSION by extra { "25w14craftmine" }
val NEOFORGE_VERSION by extra { "0.25w14craftmine.5-beta" }
val FABRIC_LOADER_VERSION by extra { "0.16.14" }
val FABRIC_API_VERSION by extra { "0.119.10+25w14craftmine" }
val IRIS_VERSION by extra { "1.8.11" }
val VOXELMAP_VERSION by extra { "1.15.3" }

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
