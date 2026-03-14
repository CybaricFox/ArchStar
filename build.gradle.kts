plugins {
    id("java")
}

var hytaleHome = ("D:/Hytale/HytaleGame")

group = "com.cybaricfox"
version = "alpha0.2.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("$hytaleHome/install/release/package/game/latest/Server/HytaleServer.jar"))
}