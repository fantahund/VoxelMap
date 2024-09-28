plugins {
    id("idea")
    id("net.neoforged.moddev") version "2.0.28-beta"
    id("java-library")
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
    maven("https://maven.pkg.github.com/ims212/Forge_Fabric_API") {
        credentials {
            username = "IMS212"
            // Read only token
            password = "ghp_" + "DEuGv0Z56vnSOYKLCXdsS9svK4nb9K39C1Hn"
        }
    }
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
        exclude("/coxelmap.refmap.json")
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

    if (PARCHMENT_VERSION != null) {
        parchment {
            minecraftVersion = MINECRAFT_VERSION
            mappingsVersion = PARCHMENT_VERSION
        }
    }

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

fun includeDep(dependency: String, closure: Action<ExternalModuleDependency>) {
    dependencies.implementation(dependency, closure)
    dependencies.jarJar(dependency, closure)
}

fun includeDep(dependency: String) {
    dependencies.implementation(dependency)
    dependencies.jarJar(dependency)
}

tasks.named("compileTestJava").configure {
    enabled = false
}

dependencies {
    compileOnly(project.project(":common").sourceSets.main.get().output)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)