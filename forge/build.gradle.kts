plugins {
    id("idea")
    id("net.minecraftforge.gradle")
    id("java-library")
}

val minecraftVersion: String by rootProject.extra
val forgeVersion: String by rootProject.extra

val fullVersion: String by rootProject.extra

base {
    archivesName.set("voxelmap-forge")
}

sourceSets {
    all {
        val buildDir = layout.buildDirectory.dir("sourcesSets/${this.name}")
        output.setResourcesDir(buildDir)
        java.destinationDirectory.set(buildDir)
    }
}

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)

    maven { url = uri("https://maven.minecraftforge.net/") }
}

dependencies {
    implementation(minecraft.dependency("net.minecraftforge:forge:${minecraftVersion}-${forgeVersion}"))
    compileOnly(project.project(":common").sourceSets.main.get().output)
}

minecraft {
    accessTransformers = files("src/main/resources/META-INF/accesstransformer.cfg")

    runs {
        register("client") {
            workingDir.set(file("run"))
            args("--mixin.config=mixin.voxelmap.json", "--mixin.config=mixin.voxelmap.forge.json")

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

        inputs.property("version", fullVersion)
        filesMatching("META-INF/mods.toml") {
            expand(mapOf("version" to fullVersion))
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