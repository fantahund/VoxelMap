plugins {
    id("java")
    id("fabric-loom") version ("1.9-SNAPSHOT") apply (false)
}

val MINECRAFT_VERSION by extra { "25w05a" }
val NEOFORGE_VERSION by extra { "21.4.6-beta" }
val FABRIC_LOADER_VERSION by extra { "0.16.10" }
val FABRIC_API_VERSION by extra { "0.115.2+1.21.5" }

val MOD_VERSION by extra { "$MINECRAFT_VERSION-1.14.9" }

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
