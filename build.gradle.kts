plugins {
    id("java")
    id("fabric-loom") version ("1.7.3") apply (false)
}

val MINECRAFT_VERSION by extra { "24w40a" }
val NEOFORGE_VERSION by extra { "21.1.65" }
val FABRIC_LOADER_VERSION by extra { "0.16.5" }
val FABRIC_API_VERSION by extra { "0.105.2+1.21.2" }

val MOD_VERSION by extra { "1.21.2-1.14-beta.1" }

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
