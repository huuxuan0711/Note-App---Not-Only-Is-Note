plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.xmobile.project0"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.xmobile.project0"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
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

    //memory leak tool
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.10")

    //Room
    implementation("androidx.room:room-runtime:2.5.0")
    annotationProcessor("androidx.room:room-compiler:2.5.0")

    //Viewpager
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    //Scalable DP and SP
    implementation(libs.sdp.android)
    implementation(libs.ssp.android)

    //Rounded ImageView
    implementation("com.makeramen:roundedimageview:2.3.0")

    //Rich-Text Editor
    implementation("jp.wasabeef:richeditor-android:2.0.0")

    //Calendar
    implementation("com.kizitonwose.calendar:view:2.6.1")

    //WheelView
    implementation("com.github.zyyoona7.WheelPicker:wheelview:2.0.7")

    //Pdf
    implementation("com.itextpdf:itext7-core:9.0.0")

    //Gson
    implementation("com.google.code.gson:gson:2.10.1")

    //rxjava
    implementation("io.reactivex.rxjava3:rxjava:3.1.5")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("androidx.room:room-rxjava3:2.5.0")
}