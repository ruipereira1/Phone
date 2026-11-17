import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Fossify Connect — reservado à videochamada Android↔Android (opção D do relatório).
// Ainda sem implementação nem dependências: nenhuma biblioteca Matrix/WebRTC é acrescentada antes da PoC-D.
plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "pt.familia.connect.matrix"
    compileSdk = libs.versions.app.build.compileSDKVersion.get().toInt()

    defaultConfig {
        minSdk = libs.versions.app.build.minimumSDK.get().toInt()
    }

    sourceSets {
        getByName("main").java.directories.add("src/main/kotlin")
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
