plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.engine"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":core:model"))
    api(project(":core:domain"))
    // sherpa-onnx vendored: AGP 9 forbids direct .aar deps when producing an
    // AAR, so the published AAR was unpacked into `libs/sherpa-onnx.jar`
    // (classes) + `src/main/jniLibs` (native .so). See AGENTS.md "Plugin rules".
    implementation(files("libs/sherpa-onnx.jar"))
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}
