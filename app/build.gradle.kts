plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
}

// Workaround: Windows + Java 17 @file encoding issue with non-ASCII project paths.
// Move build outputs to an ASCII-only temp directory so test worker classpaths are clean.
// Keep independent checkouts from sharing generated Room/R resources. The review
// checkout has the same module name (`app`), and the old shared directory caused
// Windows file locks on R.jar and misleading downstream Java errors.
val buildIdentity = Integer.toHexString(projectDir.absolutePath.lowercase().hashCode())
val asciiBuildDir = File(System.getenv("TEMP") ?: "C:\\tmp", "shizi-build-${project.name}-$buildIdentity")
layout.buildDirectory.set(asciiBuildDir)

android {
    namespace = "com.family.shizi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.family.shizi"
        minSdk = 23
        targetSdk = 35
        versionCode = 16
        versionName = "1.1-t04-t16"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        javaCompileOptions {
            annotationProcessorOptions {
                argument("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            all {
                val kotlinClasses = layout.buildDirectory.dir("tmp/kotlin-classes/debugUnitTest").get().asFile
                if (kotlinClasses.exists()) {
                    it.classpath = it.classpath + files(kotlinClasses)
                    it.testClassesDirs = it.testClassesDirs + files(kotlinClasses)
                }
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8")
    systemProperty("file.encoding", "UTF-8")
    systemProperty("sun.jnu.encoding", "UTF-8")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.kotlinx.serialization.json)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    kapt(libs.androidx.room.compiler)

    testImplementation(libs.junit4)
    testImplementation(libs.networknt.json.schema.validator)
    testImplementation(libs.jackson.databind)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

val verifyContentG1 by tasks.registering {
    group = "verification"
    description = "Runs the strict T03 content, schema, G1, and mutation test suite."
    dependsOn("testDebugUnitTest")
}

val verifyContentG2 by tasks.registering {
    group = "verification"
    description = "Runs the T04 real resource, manifest, media metadata, review gate, and mutation tests."
    dependsOn("testDebugUnitTest")
}

tasks.named("check") {
    dependsOn(verifyContentG1)
    dependsOn(verifyContentG2)
}
