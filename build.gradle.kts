buildscript {

    dependencies {
        classpath("com.android.tools.build:gradle:8.13.2")
        classpath("com.google.gms:google-services:4.4.4")
        classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.6")
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.3.0" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}