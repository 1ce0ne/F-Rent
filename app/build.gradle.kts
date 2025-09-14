plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.f_rent"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.f_rent"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "Beta Test Version"

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

    implementation("org.osmdroid:osmdroid-android:6.1.12")


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}