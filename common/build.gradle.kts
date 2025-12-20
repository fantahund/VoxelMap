plugins {
    id("java")
    id("idea")
}

val MINECRAFT_VERSION: String by rootProject.extra
val FORGE_VERSION: String by rootProject.extra

repositories {
    maven { url = uri("https://api.modrinth.com/maven") }
    maven { url = uri("https://maven.minecraftforge.net/") }
    maven { url = uri("https://repo.spongepowered.org/maven/") }
}

dependencies {
    compileOnly("net.minecraftforge:forge:${MINECRAFT_VERSION}-${FORGE_VERSION}")
    compileOnly("org.spongepowered:mixin:0.8.5")

    compileOnly("io.github.llamalad7:mixinextras-common:0.5.0")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.0")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
}

sourceSets {
    main {
        java {
            srcDir("src/main/java")
        }
        resources {
            srcDir("src/main/resources")
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
