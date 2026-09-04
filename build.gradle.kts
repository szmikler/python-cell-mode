plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "net.fhtagn.pycharm"
version = "1.5.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        pycharm("2026.2")
        bundledPlugin("PythonCore")
    }
}

tasks.withType<JavaCompile> {
    options.release = 21
    options.encoding = "UTF-8"
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "262"
            untilBuild = provider { null }
        }
    }
}
