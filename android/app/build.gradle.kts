plugins {
    alias(libs.plugins.android.application)
}
android {
    namespace = "com.deliverpro.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.deliverpro.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // ViewBinding — génère automatiquement les classes de binding pour chaque layout
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Retrofit — appels HTTP vers l'API Node.js
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp — intercepteur pour ajouter le token JWT automatiquement
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Gson — sérialisation/désérialisation JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Glide — chargement des images (photos de profil)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // WorkManager — planification du batch sync 23h59
    implementation("androidx.work:work-runtime:2.9.0")

    implementation ("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}