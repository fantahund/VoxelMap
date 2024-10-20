plugins {
    id("idea")
    id("net.neoforged.moddev") version "2.0.32-beta"
    id("java-library")
    id("maven-publish")
}

val MINECRAFT_VERSION: String by rootProject.extra
val PARCHMENT_VERSION: String? by rootProject.extra
val NEOFORGE_VERSION: String by rootProject.extra
val MOD_VERSION: String by rootProject.extra

base {
    archivesName = "voxelmap-neoforge"
}

sourceSets {

}

repositories {
    mavenLocal()
    maven("https://maven.su5ed.dev/releases")
    maven("https://maven.neoforged.net/releases/")

    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
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

    filesMatching("neoforge.mods.toml") {
        expand(mapOf("version" to MOD_VERSION))
    }
}

tasks.jar.get().destinationDirectory = rootDir.resolve("build").resolve("libs")

neoForge {
    // Specify the version of NeoForge to use.
    version = NEOFORGE_VERSION

    runs {
        create("client") {
            client()
        }
    }

    mods {
        create("voxelmap") {
            sourceSet(sourceSets.main.get())
            sourceSet(project.project(":common").sourceSets.main.get())
        }
    }
}

tasks.named("compileTestJava").configure {
    enabled = false
}

dependencies {
    compileOnly(project.project(":common").sourceSets.main.get().output)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)