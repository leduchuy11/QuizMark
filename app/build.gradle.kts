plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.quizmark"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.quizmark"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
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

    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.60.1")
    kapt("com.google.dagger:hilt-android-compiler:2.60.1")

    // Navigation & Hilt Compose
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Icons
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("com.google.firebase:firebase-firestore")

    implementation("androidx.appcompat:appcompat:1.6.1")

    implementation("org.apache.poi:poi-ooxml:5.2.3")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // OpenCV cho Android
    implementation("com.quickbirdstudios:opencv:4.5.3.0")

    // Firebase Storage (Lưu trữ ảnh trên đám mây)
    implementation("com.google.firebase:firebase-storage")

    // WorkManager (Quản lý tiến trình upload ảnh chạy ngầm khi có mạng)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Coil Compose (Thư viện siêu nhẹ giúp hiển thị ảnh từ File local hoặc URL có sẵn cơ chế tự động Cache)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Gson (Giúp chuyển đổi dữ liệu gửi cho WorkManager dễ dàng hơn)
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.amazonaws:aws-android-sdk-s3:2.39.0")
    implementation("com.amazonaws:aws-android-sdk-mobile-client:2.39.0")
    // Xác thực tài khoản Google
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Google Drive REST API
    implementation("com.google.api-client:google-api-client-android:1.33.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0")
    implementation("com.google.http-client:google-http-client-gson:1.43.3")
}