plugins {
    id("java")
}

var hytaleHome = ("D:/Hytale/HytaleGame/install")
var hytaleRelease = ("$hytaleHome/release/package/game/latest/Server/HytaleServer.jar")
var hytalePre = ("$hytaleHome/pre-release/package/game/latest/Server/HytaleServer.jar")

group = "com.cybaricfox"
version = "alpha0.3.8"

repositories {
    mavenCentral()
}

dependencies {
    implementation(files(hytaleRelease))
}