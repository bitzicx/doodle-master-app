plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.bitzicx.doodlemaster"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.bitzicx.doodlemaster"
        minSdk = 24
        targetSdk = 37
        versionName = (project.findProperty("appVersionName") as String?) ?: "1.0.0-local"
        versionCode = (project.findProperty("appVersionCode") as String?)?.toInt() ?: 1

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs{
        create("release"){
            val path = System.getenv("KEYSTORE_PATH")
            if(path != null){
                storeFile = file(path)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildFeatures{
        buildConfig = true
        compose = true
    }

    buildTypes {
        debug{
            val host = providers.gradleProperty("serverHost").orElse("10.0.2.2:8080").get()
            val literal = host.replace("\\", "\\\\").replace("\"", "\\\"")
            buildConfigField("String", "SERVER_HOST", "\"$literal\"")
            buildConfigField("boolean", "USE_TLS", "false")
        }
        release {
            buildConfigField("String", "SERVER_HOST", "\"doodlemaster-backend.onrender.com\"")
            buildConfigField("boolean", "USE_TLS", "true")
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
            optimization {
                enable = true
            }
        }
    }
    testOptions {
        unitTests.all {
            it.systemProperty("backendUrl", providers.gradleProperty("backendUrl").orNull ?: "")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // OkHttp for WebSockets
    implementation("com.squareup.okhttp3:okhttp:5.5.0")

    // Gson for crushing/un-crushing JSON
    implementation("com.google.code.gson:gson:2.14.0")

    // Hilt Core
    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-compiler:2.60.1")

    // Hilt for Jetpack Compose Navigation
    implementation("androidx.hilt:hilt-navigation-compose:1.4.0")

    implementation("androidx.navigation:navigation-compose:2.10.0")

//    data store
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    implementation("androidx.compose.material:material-icons-extended")

    // Standard Coil library for Jetpack Compose image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Extension to decode and render scalable vectors (SVGs) smoothly
    implementation("io.coil-kt:coil-svg:2.7.0")

    testImplementation("com.squareup.okhttp3:mockwebserver:5.5.0")

}