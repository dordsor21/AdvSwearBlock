plugins {
    java
    `maven-publish`
}

plugins.withId("java") {
    the<JavaPluginExtension>().toolchain {
        languageVersion.set(JavaLanguageVersion.of(16))
        vendor.set(JvmVendorSpec.ADOPTOPENJDK)
    }
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://ci.athion.net/plugin/repository/tools/")
    }

    maven {
        url = uri("https://mvnrepository.com/artifact/javax.json/javax.json-api")
    }

    maven {
        url = uri("https://repo.dmulloy2.net/repository/public/")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    compileOnly("com.comphenix.protocol:ProtocolLib:4.7.0")
    compileOnly("org.spigotmcv1_17_r1_2:spigotmcv1_17_r1_2:1_17_r1_2")
    compileOnly("javax.json:javax.json-api:1.1.2")
}

group = "me.dordsor21"
version = "1.0.4"
description = "AdvSwearBlock"

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}
