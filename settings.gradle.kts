rootProject.name = "voxelmap"

pluginManagement {
    repositories {
        mavenLocal()
        maven { url = uri("https://maven.minecraftforge.net/") }
        gradlePluginPortal()
    }
}

include("common")
include("neoforge")