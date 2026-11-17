plugins {
    alias(libs.plugins.android).apply(false)
    alias(libs.plugins.androidLibrary).apply(false) // Fossify Connect
    alias(libs.plugins.kotlinSerialization).apply(false)
    alias(libs.plugins.detekt).apply(false)
}
