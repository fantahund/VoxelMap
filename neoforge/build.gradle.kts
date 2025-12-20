plugins {
    id("idea")
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
    id("java-library")
    id("maven-publish")
}

val MINECRAFT_VERSION: String by rootProject.extra
val FORGE_VERSION: String by rootProject.extra
val MOD_VERSION: String by rootProject.extra

base {
    archivesName = "voxelmap-forge"
}

sourceSets {

}

repositories {
    mavenLocal()
    maven("https://maven.minecraftforge.net/")
    maven("https://api.modrinth.com/maven")
}

val serviceJar: Jar by tasks.creating(Jar::class) {
    from(rootDir.resolve("LICENSE.md"))
    manifest.attributes["FMLModType"] = "LIBRARY"
    archiveClassifier = "service"
}

configurations {
    create("serviceConfig") {
        isCanBeConsumed = true
        isCanBeResolved = false
        outgoing {
            artifact(serviceJar)
        }
    }
}

dependencies {
    jarJar(project(":neoforge", "serviceConfig"))
}

tasks.jar {
    val main = project.project(":common").sourceSets.getByName("main")
    from(main.output.classesDirs) {
        exclude("/voxelmap.refmap.json")
    }
    from(main.output.resourcesDir)

    from(rootDir.resolve("LICENSE.md"))

    filesMatching("mods.toml") {
        expand(mapOf("version" to MOD_VERSION))
    }
}

tasks.jar.get().destinationDirectory = rootDir.resolve("build").resolve("libs")

minecraft {
    mappings("official", MINECRAFT_VERSION)

    runs {
        create("client") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")

            mods {
                create("voxelmap") {
                    source(sourceSets.main.get())
                    source(project.project(":common").sourceSets.main.get())
                }
            }
        }
    }
}

tasks.named("compileTestJava").configure {
    enabled = false
}

dependencies {
    minecraft("net.minecraftforge:forge:${MINECRAFT_VERSION}-${FORGE_VERSION}")
    compileOnly(project.project(":common").sourceSets.main.get().output)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)
