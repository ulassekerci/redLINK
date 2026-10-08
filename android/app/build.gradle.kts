import java.io.ByteArrayOutputStream
import java.io.OutputStream
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The latest version tag in git: empty when there is none to read, or no git
// to ask.
abstract class LatestVersionTag : ValueSource<String, LatestVersionTag.Parameters> {
    interface Parameters : ValueSourceParameters {
        val directory: DirectoryProperty
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val output = ByteArrayOutputStream()
        return runCatching {
            execOperations.exec {
                workingDir = parameters.directory.get().asFile
                commandLine("git", "describe", "--tags", "--abbrev=0", "--match", "v[0-9]*")
                standardOutput = output
                errorOutput = OutputStream.nullOutputStream()
                isIgnoreExitValue = true
            }
            output.toString().trim()
        }.getOrDefault("")
    }
}

// The version is stamped at build time; no file in the repo holds it. The
// release workflow passes the tag's version as -PredlinkVersion=1.2.0. A local
// build takes the latest version tag in git with -dev added, so its protocol
// version is the major of the latest release. A release candidate's suffix is
// dropped first. With no version tag it is 0.0.0-dev.
val localVersion = providers.of(LatestVersionTag::class) { parameters.directory = layout.projectDirectory }.map { tag ->
    val release = Regex("""^v(\d+\.\d+\.\d+)(-.*)?$""").find(tag)?.groupValues?.get(1)
    "${release ?: "0.0.0"}-dev"
}

val redlinkVersion = providers.gradleProperty("redlinkVersion").orElse(localVersion).get()

// A candidate and its final release share a version code, so Android allows
// installing one over the other. Android refuses a code of 0, so a build with
// no version tag to read, 0.0.0-dev, gets 1.
val redlinkVersionCode = run {
    val match = Regex("""^(\d+)\.(\d+)\.(\d+)(-.*)?$""").find(redlinkVersion)
        ?: throw GradleException("Not a version: $redlinkVersion")
    val (major, minor, patch) = match.destructured
    (major.toInt() * 10000 + minor.toInt() * 100 + patch.toInt()).coerceAtLeast(1)
}

val vectors = rootProject.file("../protocol/vectors.json")

android {
    namespace = "org.metucet.redlink"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.metucet.redlink"
        minSdk = 31
        targetSdk = 37
        versionCode = redlinkVersionCode
        versionName = redlinkVersion
    }

    buildTypes {
        // Installs beside the release app, with its own settings and team code.
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.all {
            // The cases both apps' suites read. An input, so a change to the
            // file runs the tests again.
            it.inputs.file(vectors).withPathSensitivity(PathSensitivity.NONE)
            it.systemProperty("redlink.vectors", vectors.path)
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
}
