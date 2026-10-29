import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "com.razomy"
version = "0.0-alpha.14"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2026.2.3")
        bundledPlugin("com.intellij.java")
        pluginVerifier()
        plugins("com.redhat.devtools.lsp4ij:0.21.0")
    }

    implementation("com.google.code.gson:gson:2.12.1")
    implementation("org.commonmark:commonmark:0.24.0")
}

intellijPlatform {
    pluginVerification {
        ides {
            recommended()
        }
    }
}

sourceSets["main"].java.srcDirs("src/main/gen")

// Единый Toolchain для всего проекта (Java 21 — стандарт для IDEA начиная с версии 2024.2+)
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
        sinceBuild.set("243") // Начиная с 2024.3
        untilBuild.set("270.*") // До 2025.1 включительно (или закомментируйте, чтобы не ограничивать верхнюю планку)
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
