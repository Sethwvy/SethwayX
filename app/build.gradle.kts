plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  id("io.sentry.android.gradle") version ("5.3.0")
}

android {
  namespace = "in.sethway"
  compileSdk = 35

  defaultConfig {
    applicationId = "in.sethway"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "0.1 Beta"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  viewBinding {
    enable = true
  }

  buildFeatures.aidl = true

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions {
    jvmTarget = "11"
  }
}

dependencies {

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(libs.androidx.activity)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.navigation.fragment.ktx)
  implementation(libs.androidx.navigation.ui.ktx)

  implementation("io.github.pilgr:paperdb:2.7.2")

  implementation("androidx.camera:camera-core:1.4.1")
  implementation("androidx.camera:camera-camera2:1.4.1")
  implementation("androidx.camera:camera-lifecycle:1.4.1")
  implementation("androidx.camera:camera-view:1.4.1")

  implementation("com.github.alexzhirkevich:custom-qr-generator:1.6.2")
  implementation("me.relex:circleindicator:2.1.6")
  implementation("com.github.f4b6a3:uuid-creator:6.0.0")

  implementation("com.github.XomaDev:Smart-UDP:00df31fc80")

  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("me.zhanghai.android.fastscroll:library:1.3.0")
  implementation(libs.play.services.mlkit.barcode.scanning)

  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
}