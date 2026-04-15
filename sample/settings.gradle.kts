pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("..")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

includeBuild("..") {
    dependencySubstitution {
        substitute(module("io.github.harryjhin:slf4j-extensions-runtime"))
            .using(project(":slf4j-extensions-runtime"))
        substitute(module("io.github.harryjhin:slf4j-extensions-compiler"))
            .using(project(":slf4j-extensions-compiler"))
    }
}

rootProject.name = "sample"
