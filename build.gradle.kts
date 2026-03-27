plugins {
    id("java")
}

var hytaleHome = ("D:/Hytale/HytaleGame")

group = "com.cybaricfox"
version = "beta0.3.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("$hytaleHome/install/pre-release/package/game/latest/Server/HytaleServer.jar"))
}