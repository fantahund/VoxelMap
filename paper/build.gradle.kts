plugins {
    id("java")
}

val paperApiVersion: String by rootProject.extra
val fullVersion: String by rootProject.extra

base {
    archivesName.set("voxelmap-paper")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${paperApiVersion}")
    implementation(project(":server-common"))
}

tasks {
    processResources {
        inputs.property("version", fullVersion)

        filesMatching("plugin.yml") {
            expand(mapOf("version" to fullVersion))
        }
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(project.project(":server-common").sourceSets.main.get().output)
    }

    jar.get().destinationDirectory = rootDir.resolve("build").resolve("libs")
}
