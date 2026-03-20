plugins {
    id("java")
    id("idea")
    id("net.fabricmc.fabric-loom")
}

val minecraftVersion: String by rootProject.extra
val fabricVersion: String by rootProject.extra
val fabricApiVersion: String by rootProject.extra

repositories {

}

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")

    compileOnly("net.fabricmc:fabric-loader:${fabricVersion}")
    implementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}")

    compileOnly("net.fabricmc:sponge-mixin:0.16.4+mixin.0.8.7")
//    compileOnly("io.github.llamalad7:mixinextras-common:0.5.0")
//    annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.0")
}

sourceSets {

}

loom {
    accessWidenerPath = file("src/main/resources/voxelmap.accesswidener")

    mods {
        val main by creating { // to match the default mod generated for Forge
            sourceSet("main")
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