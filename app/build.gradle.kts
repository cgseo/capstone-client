plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.soundwatch"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.soundwatch"
        minSdk = 24
        targetSdk = 34
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation ("com.prolificinteractive:material-calendarview:1.4.3") // 캘린더 뷰를 위해 추가
    implementation ("com.squareup.retrofit2:retrofit:2.9.0") // retrofit 의존성 추가
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0") // JSON 변환에 필요 (Gson 사용 시)
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.3") // 로깅 인터셉터
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0") // 실시간 소음 데이터 그래프 API
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("androidx.activity:activity:1.8.0")
}