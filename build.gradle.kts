plugins {
    id("java")
}

var hytaleHome = ("D:/Hytale/HytaleGame")

group = "com.cybaricfox"
version = "beta0.4.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("$hytaleHome/install/release/package/game/latest/Server/HytaleServer.jar"))
}