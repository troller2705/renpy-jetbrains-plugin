plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.2.10"
    id("org.jetbrains.intellij.platform") version "2.10.5"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

group = "ee.vstepa.jetbrains.plugins"
version = "0.0.3-252"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure IntelliJ Platform Gradle Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        pycharmCommunity("2025.2.5")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        bundledPlugin("PythonCore")
    }
}

sourceSets {
    main {
        java {
            srcDirs("src/main/gen")
        }
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252"
        }
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    generateLexer {
        sourceFile.set(File("src/main/grammars/RenPyScript.flex"))
        targetOutputDir.set(File("src/main/gen/ee/vstepa/jetbrains/plugins/renpy/lang/script/lexer"))
//        purgeOldFiles.set(true)
    }

    compileJava {
        dependsOn(generateLexer)
    }

    compileKotlin {
        dependsOn(generateLexer)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
