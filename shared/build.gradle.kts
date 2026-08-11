plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
            // Supabase (Multiplatform)
            implementation("io.github.jan-tennert.supabase:postgrest-kt:3.0.3")
            implementation("io.github.jan-tennert.supabase:auth-kt:3.0.3")
            implementation("io.github.jan-tennert.supabase:realtime-kt:3.0.3")
            implementation("io.github.jan-tennert.supabase:storage-kt:3.0.3")
            implementation("io.ktor:ktor-client-core:3.0.3")
            implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")
            implementation("io.ktor:ktor-client-websockets:3.0.3")
            
            implementation(libs.jetbrains.navigation.compose)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
            implementation("media.kamel:kamel-image:0.9.3")
        }
        
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation("io.ktor:ktor-client-okhttp:3.0.3")
            implementation("com.squareup.okhttp3:okhttp:4.12.0")
            
            // Google Play Services & Firebase (Android specific)
            implementation(libs.google.maps)
            implementation(libs.maps.compose)
            implementation("com.google.android.gms:play-services-location:21.2.0")
            implementation("com.google.firebase:firebase-database-ktx:21.0.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
        }
    }
}

android {
    namespace = "com.example.myapplication.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            java.srcDirs("src/androidMain/java", "src/androidMain/kotlin")
            assets.srcDirs("src/androidMain/assets")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
