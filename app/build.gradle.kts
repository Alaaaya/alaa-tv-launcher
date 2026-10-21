plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace = "com.alaa.tvlauncher"
 compileSdk = 36
 defaultConfig { applicationId = "com.alaa.tvlauncher"; minSdk = 22; targetSdk = 36; versionCode = 4; versionName = "1.1" }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17; isCoreLibraryDesugaringEnabled = true }
 kotlinOptions { jvmTarget = "17" }
 testOptions { unitTests.isIncludeAndroidResources = true; unitTests.all { it.maxHeapSize = "1200m"; it.maxParallelForks = 1 } }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.06.01"))
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.tv:tv-material:1.0.1")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.robolectric:robolectric:4.15.1")
 testImplementation("androidx.compose.ui:ui-test-junit4")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
 testImplementation("androidx.test:core:1.6.1")
}
