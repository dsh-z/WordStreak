import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// ───────────────────────────────────────────────────────────────────────────
// 构建配置分区：本地兼容 vs 标准(CI)
// 默认（未设置 wordstreak.localBionic）= 标准构建：官方未修改 room-compiler 2.8.5，
//   恢复 SQL 编译期查询验证（DatabaseVerifier 正常加载）。
// -Pwordstreak.localBionic=true = 仅限本机 bionic/Termux 宿主的兼容构建：
//   该宿主无 glibc，官方 room-compiler 的 Xerial sqlite-jdbc 原生库无法加载，
//   故改用 localrepo 中打桩（DatabaseVerifier.create→null / cleanup 空）的 room-compiler。
// 注意：本地兼容不影响 Room 源码，也不进入标准/CI 构建。
// ───────────────────────────────────────────────────────────────────────────
val localBionic = (project.findProperty("wordstreak.localBionic") as String?)?.toBoolean() ?: false

if (localBionic) {
    configurations.matching { it.name.startsWith("ksp") }.configureEach {
        resolutionStrategy {
            force("androidx.room:room-compiler:2.8.5-patched")
            eachDependency {
                if (requested.group == "androidx.room" && requested.name == "room-compiler") {
                    useVersion("2.8.5-patched")
                }
            }
        }
    }
}

// 对齐 androidTest 运行时 kotlinx-serialization 版本：room-testing 2.8.5 依赖 serialization-json 1.8.1，
// 但运行时曾被 strict 1.7.3 将 core 降到 1.7.3，导致 MigrationTestHelper 反序列化 schema 时
// AbstractMethodError(typeParametersSerializers)。统一到 1.8.1。（仅影响测试运行时，不改生产依赖）
configurations.matching { it.name.contains("AndroidTest") }.configureEach {
    resolutionStrategy {
        force(
            "org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1",
            "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.1",
            "org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1",
            "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.1"
        )
    }
}

// ───────────────────────────────────────────────────────────────────────────
// 固定签名（Stage 3.5）：优先读仓库外本地 keystore.properties；CI 后续可通过环境变量注入。
// 未配置时 hasFixedSigning=false，回退 AGP 默认 debug 签名（保证无签名环境的构建不受影响）。
// 严禁把 keystore / 密码 / 私钥提交到 Git。
// ───────────────────────────────────────────────────────────────────────────
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
val signingValue: (String, String) -> String? = { propKey, envKey ->
    keystoreProps.getProperty(propKey) ?: System.getenv(envKey)
}
val fixedStoreFile = signingValue("storeFile", "WS_STORE_FILE")
val fixedStorePassword = signingValue("storePassword", "WS_STORE_PASSWORD")
val fixedKeyAlias = signingValue("keyAlias", "WS_KEY_ALIAS")
val fixedKeyPassword = signingValue("keyPassword", "WS_KEY_PASSWORD")
val hasFixedSigning = fixedStoreFile != null && file(fixedStoreFile).exists() &&
    fixedStorePassword != null && fixedKeyAlias != null && fixedKeyPassword != null

android {
    namespace = "com.wordstreak"
    compileSdk = 37

    signingConfigs {
        if (hasFixedSigning) {
            create("fixed") {
                storeFile = file(fixedStoreFile!!)
                storePassword = fixedStorePassword
                keyAlias = fixedKeyAlias
                keyPassword = fixedKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.wordstreak"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            if (hasFixedSigning) signingConfig = signingConfigs.getByName("fixed")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasFixedSigning) signingConfig = signingConfigs.getByName("fixed")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Room schema 导出目录，供迁移测试使用
    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)

    implementation(libs.androidx.navigation.compose)

    // Room 运行时 + KTX
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // KSP 注解处理器：标准构建用官方 room-compiler；本地 bionic 宿主用打桩版本
    if (localBionic) {
        ksp("androidx.room:room-compiler:2.8.5-patched")
    } else {
        ksp(libs.androidx.room.compiler)
    }

    // DataStore：偏好设置
    implementation(libs.androidx.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // 数据库 / 数据持久化测试（Android Instrumented Test）
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
