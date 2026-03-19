plugins {
    id("java")
    id("fabric-loom") version ("1.13-SNAPSHOT") apply (false)
    id("net.minecraftforge.gradle") version ("6.0.47") apply (false)
    id("net.neoforged.moddev") version ("2.0.137") apply (false)
    id("org.spongepowered.mixin") version ("0.7-SNAPSHOT") apply (false)
}

val minecraftVersion by extra { "1.21.11" }
val forgeVersion by extra { "61.0.1" }
val neoForgeVersion by extra { "21.11.21-beta" }
val fabricVersion by extra { "0.18.2" }
val fabricApiVersion by extra { "0.140.0+1.21.11" }
val voxelMapVersion by extra { "1.16.3" }

val fullVersion by extra { "${minecraftVersion}-${voxelMapVersion}" }

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

    repositories {
        maven { url = uri("https://api.modrinth.com/maven") }
    }

    java.toolchain.languageVersion = JavaLanguageVersion.of(21)

    tasks.processResources {
        filesMatching("META-INF/neoforge.mods.toml") {
            expand(mapOf("version" to fullVersion))
        }
    }

    version = fullVersion
    group = "com.mamiyaotaru"

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<GenerateModuleMetadata>().configureEach {
        enabled = false
    }
}
