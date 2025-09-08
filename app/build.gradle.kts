plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.akkubattrent"
    compileSdk = 35

//    buildFeatures {
//        buildConfig = true  // ← ДОБАВЬ ЭТУ СТРОЧКУ
//    }

    defaultConfig {
        applicationId = "com.example.akkubattrent"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "2.0.0.0 -BETA"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

//    buildTypes {
//        debug {
//            buildConfigField("String", "API_BASE_URL", "\"https://akkubatt-work.ru/\"")
//            buildConfigField("String", "API_KEY", "\"o4bAvyn0ZXaFeLDr89lk\"")
//        }
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//            buildConfigField("String", "API_BASE_URL", "\"https://akkubatt-work.ru/\"")
//            buildConfigField("String", "API_KEY", "\"o4bAvyn0ZXaFeLDr89lk\"")
//        }
//    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // === Основные библиотеки AndroidX ===
    implementation(libs.appcompat)                                      // Базовая библиотека поддержки AppCompatActivity
    implementation(libs.material)                                       // Библиотека Material Design от Google (через version catalog)
    implementation("com.google.android.material:material:1.6.0")        // Альтернативная версия Material Design (возможно, дубликат)
    implementation("androidx.preference:preference:1.2.0")              // Работа с настройками приложения (SharedPreferences)
    implementation("androidx.work:work-runtime:2.7.1")                  // Библиотека для фоновых задач

    // === Работа с картами ===
    implementation("org.osmdroid:osmdroid-android:6.1.12")              // Библиотека osmdroid для отображения карт (используется в коде)

    // === Работа с сетью и API ===
    implementation("com.android.volley:volley:1.2.1")                   // Библиотека для HTTP-запросов (используется в PostamatApiService)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")             // Альтернативная библиотека для работы с REST API
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")       // Конвертер JSON для Retrofit
    implementation("com.squareup.okhttp3:okhttp:4.9.3")                 // HTTP клиент (используется в коде и Retrofit)
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")    // Логирование сетевых запросов для OkHttp

    // === Работа с JSON ===
    implementation("com.google.code.gson:gson:2.10.1")                  // Библиотека для сериализации/десериализации JSON

    // === Работа с изображениями ===
    implementation("com.github.bumptech.glide:glide:4.12.0")            // Загрузка и кэширование изображений
    implementation("de.hdodenhof:circleimageview:3.1.0")                // Круглый ImageView (для аватаров и т.п.)

    // === Работа со сканером QR-кодов ===
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")      // Интеграция ZXing для сканирования QR
    implementation("com.google.zxing:core:3.5.2")                       // Ядро библиотеки ZXing

    // === Работа с базой данных ===
    implementation("mysql:mysql-connector-java:5.1.49")                 // JDBC драйвер для MySQL (используется в коде)

    // === Безопасность ===
    implementation("org.mindrot:jbcrypt:0.4")                           // Библиотека для хеширования паролей

    // === Тестирование ===
    testImplementation(libs.junit)                                      // JUnit для unit-тестов
    androidTestImplementation(libs.ext.junit)                           // Расширения JUnit для Android тестов
    androidTestImplementation(libs.espresso.core)                       // Espresso для UI тестов
}