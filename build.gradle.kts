import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    `java-library`

    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.diffplug.spotless") version "6.22.0"
}

the<JavaPluginExtension>().toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
}

configurations.all {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }

    maven {
        url = uri("https://repo.dmulloy2.net/repository/public/")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }

    maven {
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }
}

dependencies {
    compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.20.2-R0.1-SNAPSHOT")
    compileOnly("javax.json:javax.json-api:1.1.2")
    compileOnly("org.apache.logging.log4j:log4j-api:2.19.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.14.0")
    implementation("net.kyori:adventure-api:4.14.0")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("io.papermc:paperlib:1.0.7")
}

group = "me.dordsor21"
version = "2.0.0-SNAPSHOT"
description = "AdvSwearBlock"

spotless {
    java {
        licenseHeaderFile(rootProject.file("HEADER.txt"))
        target("**/*.java")
        endWithNewline()
        trimTrailingWhitespace()
        removeUnusedImports()
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set(null as String?)
    dependencies {
        relocate("com.zaxxer.hikari", "me.dordsor21.hikari") {
            include(dependency("com.zaxxer:HikariCP"))
        }
        relocate("org.apache.commons", "me.dordsor21.apache.commons") {
            include(dependency("org.apache.commons:commons-lang3"))
        }
        relocate("io.papermc.lib", "me.dordsor21.apache.paperlib") {
            include(dependency("io.papermc:paperlib"))
        }
    }
    minimize()
}

tasks.named("build").configure {
    dependsOn("shadowJar")
}
