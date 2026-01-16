plugins {
    id("java")
}

var hytaleHome = ("D:/Hytale/HytaleGame")
var testServer = ("C:/Users/cmorg/Documents/ArchStar/server/HytaleServer.jar")
var assets = ("C:/Users/cmorg/Documents/ArchStar/server/Assets.zip")

group = "com.cybaricfox"
version = "alpha1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("$hytaleHome/install/release/package/game/latest/Server/HytaleServer.jar"))
}

tasks.register<Exec>("runTestServer"){
    group = "TestServer"
    description = "Build the project, then run a server for testing."

    println("Test")

    dependsOn(tasks.named("build"))

    commandLine("java -jar $testServer --assets $assets")
}