plugins {
    java
}

val shade by configurations.creating

val hytaleHome = "D:/Hytale/HytaleGame/install"
val hytaleRelease = "$hytaleHome/release/package/game/latest/Server/HytaleServer.jar"
val hytalePre = "$hytaleHome/pre-release/package/game/latest/Server/HytaleServer.jar"

group = "com.cybaricfox"
version = "alpha0.4.6"

repositories {
    mavenCentral()

    maven {
        name = "AzureDoom Maven"
        url = uri("https://maven.azuredoom.com/mods")
    }
}

dependencies {
    compileOnly(files(hytaleRelease))

    implementation("com.azuredoom.hytalecustomassetloader:hytale-custom-asset-loader:1.1.3")

    shade("com.azuredoom.hytalecustomassetloader:hytale-custom-asset-loader:1.1.3")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from({
        shade.map {
            if (it.isDirectory) it else zipTree(it)
        }
    })

    exclude(
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA"
    )
}