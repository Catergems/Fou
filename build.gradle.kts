plugins {
    id("fabric-loom") version "1.16-SNAPSHOT"
    id("org.jetbrains.kotlin.jvm") version "2.3.21"
}

version = project.property("mod_version").toString()
group = project.property("maven_group").toString()

base {
    archivesName.set(project.property("archives_base_name").toString())
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("fou") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.getByName("client"))
        }
    }
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_api_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("fabric_kotlin_version")}")
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(
            "version"              to project.version.toString(),
            "minecraft_version"    to project.property("minecraft_version").toString(),
            "loader_version"       to project.property("loader_version").toString(),
            "fabric_kotlin_version" to project.property("fabric_kotlin_version").toString(),
            "fabric_api_version"   to project.property("fabric_api_version").toString()
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

kotlin {
    jvmToolchain(21)
}
