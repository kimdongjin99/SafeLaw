buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://artifacts.objectbox.io/repos") }
    }
    dependencies {
        classpath("io.objectbox:objectbox-gradle-plugin:4.0.3")
    }

}

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}