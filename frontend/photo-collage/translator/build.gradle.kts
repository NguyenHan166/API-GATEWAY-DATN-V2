plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.google.cloud.translate)
}

sourceSets {
    main {
        kotlin {
            srcDir("src/main")
        }
        java.destinationDirectory.set(layout.buildDirectory.dir("classes/kotlin/main"))
        resources.destinationDirectory.set(layout.buildDirectory.dir("resources/main"))
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "translator.StringTranslator"
    }
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/MANIFEST.MF")
    
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

// Disable generating classes in bin directory
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
    }
    destinationDirectory.set(file("${layout.buildDirectory.get()}/classes/kotlin/main"))
}

// Force delete bin directory if it exists
tasks.register("deleteBinDirectory") {
    group = "build"
    description = "Delete bin directory"
    doLast {
        delete("bin")
    }
}

// Make sure build tasks run deleteBinDirectory
tasks.build {
    dependsOn("deleteBinDirectory")
}

// Run deleteBinDirectory before compileKotlin
tasks.compileKotlin {
    dependsOn("deleteBinDirectory")
}

// Clean bin directory on clean task
tasks.clean {
    delete("bin")
}

tasks.register<JavaExec>("runSelectiveTranslator") {
    group = "translator"
    description = "Runs the SelectiveStringTranslator"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("translator.SelectiveStringTranslator")
    
    // Delete bin directory before running
    doFirst {
        delete("bin")
    }
}