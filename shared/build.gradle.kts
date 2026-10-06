plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.6.10"
}

kotlin {
    android()
    ios {
        binaries {
            framework {
                baseName = "shared"
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlinx-coroutines-core:1.8.0")
                implementation("org.jetbrains.compose.foundation:foundation:1.6.10")
                implementation("org.jetbrains.compose.material3:material3:1.6.10")
                implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.6.10")
                implementation("org.jetbrains.compose.runtime:runtime-saveable:1.6.10")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.compose.ui:ui:1.6.8")
                implementation("androidx.compose.material3:material3:1.2.1")
                implementation("androidx.compose.foundation:foundation:1.6.8")
            }
        }
        val iosMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.foundation:foundation-ui:1.6.10")
                implementation("org.jetbrains.compose.material3:material3:1.6.10")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
