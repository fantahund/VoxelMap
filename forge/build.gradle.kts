plugins {
    id("idea")
    id("net.minecraftforge.gradle")
    id("org.spongepowered.mixin")
    id("java-library")
}

val MINECRAFT_VERSION: String by rootProject.extra
val FORGE_VERSION: String by rootProject.extra

val MOD_VERSION: String by rootProject.extra

base {
    archivesName.set("voxelmap-forge")
}

mixin {
    config("mixin.voxelmap.json")
    config("mixin.voxelmap.forge.json")
}

sourceSets {
    all {
        val buildDir = layout.buildDirectory.dir("sourcesSets/${this.name}")
        output.setResourcesDir(buildDir)
        java.destinationDirectory.set(buildDir)
    }
}

dependencies {
    minecraft ("net.minecraftforge:forge:${MINECRAFT_VERSION}-${FORGE_VERSION}")
    compileOnly(project.project(":common").sourceSets.main.get().output)
}

minecraft {
    mappings("official", MINECRAFT_VERSION)

    accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))

    reobf = false
    copyIdeResources = true

    runs {
        create("client") {
            workingDirectory(file("run"))
            ideaModule = "${rootProject.name}.${project.name}.main"
            taskName = "Client"

            mods {
                create("voxelmap") {
                    source(sourceSets.main.get())
                    source(project.project(":common").sourceSets.main.get())
                }
            }
        }
    }
}

tasks {
    withType<JavaCompile> {
        val commonMain = project(":common").sourceSets.main.get()
        source(commonMain.java.srcDirs)
    }

    processResources {
        val commonMain = project(":common").sourceSets.main.get()
        from(commonMain.resources.srcDirs) {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

        inputs.property("version", project.version)
        filesMatching("META-INF/mods.toml") {
            expand(mapOf("version" to MOD_VERSION))
        }
    }

    jar {
        manifest {
            attributes["MixinConfigs"] = "mixin.voxelmap.json,mixin.voxelmap.forge.json"
        }

        from(rootDir.resolve("LICENSE.md"))
    }

    jar.get().destinationDirectory = rootDir.resolve("build").resolve("libs")

    test {
        enabled = false
    }
}