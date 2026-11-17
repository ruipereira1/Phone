import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Fossify Connect — videochamada FaceTime através dos links oficiais da Apple.
// Depende apenas de :connect-core.
plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "pt.familia.connect.facetime"
    compileSdk = libs.versions.app.build.compileSDKVersion.get().toInt()

    defaultConfig {
        minSdk = libs.versions.app.build.minimumSDK.get().toInt()
    }

    sourceSets {
        getByName("main").java.directories.add("src/main/kotlin")
        getByName("test").java.directories.add("src/test/kotlin")
    }

    compileOptions {
        val javaVersion = JavaVersion.valueOf(libs.versions.app.build.javaVersion.get())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(libs.versions.app.build.kotlinJVMTarget.get()))
}

dependencies {
    implementation(project(":connect-core"))
    testImplementation(libs.junit)
}
