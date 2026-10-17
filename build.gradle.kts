plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

group = "org.connecttag.libs.kotlin"
version = "1.0.4"

android {
    namespace = "org.connecttag.core.integrity.guard"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.ktx)
    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.core)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["release"])
            groupId = "org.connecttag.libs.kotlin"
            artifactId = "integrity-guard"
            version = "1.0.4"

            pom {
                name.set("ConnectTag Integrity Guard")
                description.set("Android app integrity and security checks (Root detection, Installer verification, Emulator check, Debugger check)")
                url.set("https://github.com/connecttag/integrity-guard")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("connecttagye")
                        name.set("Connect Tag")
                        email.set("connecttagye@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/connecttag/integrity-guard.git")
                    developerConnection.set("scm:git:ssh://git@github.com:connecttag/integrity-guard.git")
                    url.set("https://github.com/connecttag/integrity-guard/tree/main")
                }
            }
        }
    }
}
