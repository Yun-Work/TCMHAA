plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.tcmhaa"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tcmhaa"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    buildTypes {
        debug {
            // 模擬器 URL
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:6060\"")
        }
        release {
            // 真機 URL（改成你的電腦 IPv4）
            buildConfigField("String", "BASE_URL", "\"http://172.20.10.2:6060\"")
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)



    // ===== CameraX（必要）=====
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion") // PreviewView
    // 如需拍影片再加：implementation("androidx.camera:camera-video:$cameraxVersion")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("org.json:json:20230227")
    //（可選）如要處理圖片旋轉/EXIF
    implementation("androidx.exifinterface:exifinterface:1.3.7")



    // OkHttp 4.9.3
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

    // Retrofit + Gson 轉換器
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    // 日期
    implementation ("com.google.android.material:material:1.12.0")
    //notify(time.java)
    implementation ("androidx.core:core:1.12.0")
    //做圖表用的
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

}