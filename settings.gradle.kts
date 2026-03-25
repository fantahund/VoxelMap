rootProject.name = "voxelmap"

pluginManagement {
    repositories {
        mavenLocal()
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://maven.minecraftforge.net/") }
        maven { url = uri("https://maven.neoforged.net/releases/") }
        maven { url = uri("https://repo.spongepowered.org/repository/maven-public") }
        gradlePluginPortal()
    }
}

include("common")
include("fabric")
//include("forge")
include("neoforge")
