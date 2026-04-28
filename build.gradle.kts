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
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.commonmark:commonmark:0.21.0")
}

intellijPlatform {
    pluginVerification {
        ides {
            recommended()
        }
    }
}

sourceSets["main"].java.srcDirs("src/main/gen")

// 1. Устанавливаем единый Toolchain для всего проекта (Java 21)
kotlin {
    jvmToolchain(21)
}

tasks {
    // 2. Указываем компилятору Java версию 21
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    // 3. Указываем компилятору Kotlin версию 21 (теперь они совпадают!)
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }

    patchPluginXml {
        sinceBuild.set("243")
        untilBuild.set("300.*")
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
