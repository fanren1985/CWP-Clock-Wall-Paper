plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// ============================================================
// 自动版本号管理
// 每次编译 APK/AAB 时版本号自动 +0.1
// 版本格式：1.0 → 1.1 → 1.2 → 1.3 → ...
// ============================================================
val versionPropsFile = file("version.properties")

// 读取当前版本计数器
var versionCounter = 0
if (versionPropsFile.exists()) {
    val lines = versionPropsFile.readLines()
    for (line in lines) {
        if (line.startsWith("VERSION_CODE=")) {
            versionCounter = line.substringAfter("VERSION_CODE=").trim().toIntOrNull() ?: 0
            break
        }
    }
}

android {
    namespace = "com.CWP.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.CWP.app"
        minSdk = 26
        targetSdk = 34
        // Android 内部整数版本号（必须单调递增）
        versionCode = versionCounter + 1
        // 对外显示的版本名：每次 +0.1
        versionName = "1.$versionCounter"
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

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

// 编译成功后自动递增版本计数器
// 仅 assembleRelease / assembleDebug / bundleRelease 触发，sync 等操作不影响
tasks.matching {
    it.name == "assembleRelease" || it.name == "assembleDebug" || it.name == "bundleRelease"
}.configureEach {
    doLast {
        // 重新读取当前值，避免配置缓存导致的过期问题
        var counter = 0
        if (versionPropsFile.exists()) {
            val lines = versionPropsFile.readLines()
            for (line in lines) {
                if (line.startsWith("VERSION_CODE=")) {
                    counter = line.substringAfter("VERSION_CODE=").trim().toIntOrNull() ?: 0
                    break
                }
            }
        }
        counter += 1
        versionPropsFile.writeText("VERSION_CODE=$counter\n")
    }
}
