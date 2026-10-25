plugins {
    `kotlin-dsl`
    kotlin("jvm") version "2.2.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.kikugie.dev/snapshots")
    maven("https://maven.kikugie.dev/releases")
}

dependencies {
    implementation("dev.kikugie:stonecutter:0.7")
    implementation("net.fabricmc:fabric-loom:1.11-SNAPSHOT")
    implementation("net.neoforged.moddev:net.neoforged.moddev.gradle.plugin:2.0.107")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20-Beta1")
    implementation("com.github.mwiede:jsch:0.2.16")
}