import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt.android.plugin)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

apply(from = "../autodimension.gradle")

fun loadSigningProperties(fileName: String) = Properties().apply {
    rootProject.file(fileName).let { file ->
        if (file.exists()) {
            load(file.reader())
        }
    }
}

android {
    namespace = "com.artsal.photo.editor.collage.maker"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.artsal.photo.editor.collage.maker"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()

        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("releaseDev") {
            val properties = loadSigningProperties("signing-dev.properties")
            if (properties.isNotEmpty()) {
                storeFile = rootProject.file(properties.getProperty("DEV_KEYSTORE_FILE"))
                storePassword = properties.getProperty("DEV_KEYSTORE_STORE_PASSWORD")
                keyPassword = properties.getProperty("DEV_KEYSTORE_KEY_PASSWORD")
                keyAlias = properties.getProperty("DEV_KEYSTORE_ALIAS")
            }
        }

        create("releaseProduct") {
            val properties = loadSigningProperties("signing-product.properties")
            if (properties.isNotEmpty()) {
                storeFile = rootProject.file(properties.getProperty("PROD_KEYSTORE_FILE"))
                storePassword = properties.getProperty("PROD_KEYSTORE_STORE_PASSWORD")
                keyPassword = properties.getProperty("PROD_KEYSTORE_KEY_PASSWORD")
                keyAlias = properties.getProperty("PROD_KEYSTORE_ALIAS")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }

        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            )
        }
    }

    flavorDimensions.add("default")
    productFlavors {
        create("dev") {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            signingConfig = signingConfigs.getByName("releaseDev")

            resValue("string", "app_name", "Photo Editor Dev")
            resValue("string", "ad_app_id", "ca-app-pub-3940256099942544~3347511713")
            resValue("string", "facebook_app_id", "1234567890")
            resValue("string", "facebook_client_token", "1234567890")
            resValue("string", "appflayer_key", "1234567890")

            buildConfigField("String", "API_DOMAIN", "\"https://apidatn.nguyenvanhan.site/\"")
            buildConfigField("Boolean", "isDevelopment", "true")
        }
        create("product") {
            signingConfig = signingConfigs.getByName("releaseProduct")
            val productProperties = loadSigningProperties("signing-product.properties")

            resValue("string", "app_name", "Photo Editor")
            resValue("string", "ad_app_id", productProperties.getProperty("AD_APP_ID"))
            resValue("string", "facebook_app_id", productProperties.getProperty("FB_APP_ID"))
            resValue("string", "facebook_client_token", productProperties.getProperty("FB_CLIENT_TOKEN"))
            resValue("string", "appflayer_key", productProperties.getProperty("AF_KEY"))

            buildConfigField("String", "API_DOMAIN", "\"https://apidatn.nguyenvanhan.site/\"")
            buildConfigField("Boolean", "isDevelopment", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    firebaseCrashlytics {
        mappingFileUploadEnabled = true
    }

    // Add lint options
    lint {
        abortOnError = false
        checkReleaseBuilds = true
        disable += "MissingTranslation"
    }

    kotlin.sourceSets.all {
        languageSettings.optIn("kotlin.RequiresOptIn")
    }

    assetPacks += listOf(":asset_photo")

    bundle {
        language {
            enableSplit = false
        }
    }

}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.core.splashscreen)
    // Trickle
    implementation(fileTree(mapOf(
        "dir" to "libs",
        "include" to listOf("*.aar")
    )))
    // Lifecycle
    implementation(libs.androidx.lifecycle.process)
    // Material Design
    implementation(libs.material)
    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    // Coroutine
    implementation(libs.kotlinx.coroutines.android)
    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
//    implementation(libs.androidx.ui.unit.android)
    ksp(libs.hilt.android.compiler)
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // Network
    implementation(libs.gson)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    //Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    // Image
    implementation(libs.glide)
    ksp(libs.ksp.glide)
    implementation(libs.glide.transformations)
    implementation(libs.circleimageview)
    // Timber
    implementation(libs.timber)
    // Flexbox
    implementation(libs.flexbox)
    // Leakcanary
//    debugImplementation(libs.leakcanary.android)
    // MinSap sdk ads
    implementation(libs.minsap.adsdk)
    // Lottie
    implementation(libs.lottie)
    // Fb sdk
    implementation(libs.shimmer)
    // assets delivery
    implementation(libs.asset.delivery.ktx)
    // coil3
    implementation(libs.coil)
    implementation(libs.coil3.coil.network.okhttp)
    // desugaring
    coreLibraryDesugaring(libs.desugaring)
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // implementation for module dependencies
    implementation(project(":ucrop"))
}

// Translation Task
tasks.register("translateStrings") {
    dependsOn(":translator:jar")

    doLast {
        val workingDir = project.projectDir
        val srcResDir = file("${workingDir}/src/main/res")
        val sourceFile = file("${srcResDir}/values/strings.xml")
        val supportedLanguages = listOf(
            "en-rUS" to "English(US)",
            "en-rGB" to "English(UK)",
            "hi" to "Hindi(हिंद)",
            "bn" to "Bangla(বাংলা)",
            "pt-rBR" to "Portuguese(brasileiro)",
            "ar-rSA" to "Saudi Arabia(عربي)",
            "ar-rTN" to "Tunisia(عربي)",
            "ar-rOM" to "Oman(عربي)",
            "in" to "Indonesian(bahasa Indo)",
            "pt-rPT" to "Portuguese(português)",
            "es" to "Spanish(español)",
            "mr" to "Marathi(मराठी)",
            "te" to "Tegulu(తెలుగు)",
            "ta" to "Tamil(தமிழ்)",
            "it-rIT" to "Italian(italiano)",
            "ru" to "Russian(русский)",
            "fr" to "French(français)",
            "vi" to "Vietnamese(Tiếng Việt)",
            "de-rDE" to "German(Deutsch)",
            "ko" to "Korean(한국인)",
            "ja" to "Japanese(日本語)",
            "zh" to "Chinese(中国人)",
            "tr-rTR" to "Turkish(Türkçe)",
            "th" to "Thai(คนไทย)",
            "nl-rNL" to "Dutch(Nederlands)",
            "da-rDK" to "Danish(dansk)",
            "ga" to "Irish(Gaeilge)",
            "pl" to "Polish(polski)",
            "zu" to "Zulu",
        )

        // Execute the translation using the JAR from translator module
        val jarFile = project.rootProject.file("translator/build/libs/translator.jar")

        if (!jarFile.exists()) {
            logger.error("Translator JAR not found. Make sure to build the translator module first.")
            return@doLast
        }

        exec {
            workingDir(projectDir)
            commandLine(
                "java", "-jar", jarFile.absolutePath,
                "--sourcefile", sourceFile.absolutePath,
                "--languages", supportedLanguages.joinToString(",") { "${it.first}:${it.second}" },
                "--outdir", srcResDir.absolutePath
            )
        }
    }
}