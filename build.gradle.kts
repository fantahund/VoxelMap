plugins {
    id("java")
    id("fabric-loom") version ("1.8-SNAPSHOT") apply (false)
}

val MINECRAFT_VERSION by extra { "1.21.2" }
val NEOFORGE_VERSION by extra { "21.2.0-beta" }
val FABRIC_LOADER_VERSION by extra { "0.16.7" }
val FABRIC_API_VERSION by extra { "0.106.1+1.21.2" }

val MOD_VERSION by extra { "1.21.2-1.14" }

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
