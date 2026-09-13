plugins {
    id("java")
    id("idea")
    id("net.fabricmc.fabric-loom")
    id("com.gradleup.shadow") version "8.3.0"
}

val minecraftVersion: String by rootProject.extra
val fabricVersion: String by rootProject.extra
val fabricApiVersion: String by rootProject.extra
val voxelConfigVersion: String by rootProject.extra

repositories {

}

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")

    compileOnly("net.fabricmc:fabric-loader:${fabricVersion}")
    implementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}")

    implementation("de.tobi:voxelconfig:${voxelConfigVersion}")

    compileOnly("net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7")
    testImplementation("com.google.code.gson:gson:2.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
    shadowJar {
        dependencies {
            include(dependency("de.tobi:voxelconfig:.*"))
        }
    }
    
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(rootDir.resolve("LICENSE.md"))
        // we no longer need manual zipTree because shadowJar handles it!
    }
}

// This trick hides common tasks in the IDEA list.
tasks.configureEach {
    group = null
}

tasks.test {
    useJUnitPlatform()
}
