import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

group = "com.razomy"
version = "0.0-alpha.13"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.2.5")
        bundledPlugin("com.intellij.java")
        pluginVerifier()
    }
}

intellijPlatform {
    pluginVerification {
        ides {
            recommended()
        }
    }
}

sourceSets["main"].java.srcDirs("src/main/gen")

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions.jvmTarget = JvmTarget.JVM_17
    }

    patchPluginXml {
        sinceBuild.set("243")
        untilBuild.set("253.*")
    }

    signPlugin {
        certificateChainFile.set(file("certificate/chain.crt"))
        privateKeyFile.set(file("certificate/private.pem"))
        password.set(file("certificate/password.txt").readText())
    }

    publishPlugin {
        token.set(file("certificate/token.txt").readText())
        channels.set(listOf("alpha"))
    }
}
