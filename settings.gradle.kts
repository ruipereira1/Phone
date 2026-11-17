rootProject.name = "FossifyConnect"
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://www.jitpack.io") }
        mavenLocal()
    }
}
include(":app")
// Fossify Connect: módulos independentes (ver FORK.md)
include(":connect-core", ":connect-facetime", ":connect-simple-ui", ":connect-matrix")
