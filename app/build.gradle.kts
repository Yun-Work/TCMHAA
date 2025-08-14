plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.tcmhaa"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tcmhaa"
        minSdk = 24
        targetSdk = 35
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

    // 建議開啟，之後用 binding.XXX 就不會因 id 對不到而 NPE
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // 你原本的
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // ===== CameraX（必要）=====
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion") // PreviewView
    // 如需拍影片再加：implementation("androidx.camera:camera-video:$cameraxVersion")

    //（可選）如要處理圖片旋轉/EXIF
    implementation("androidx.exifinterface:exifinterface:1.3.7")
}
