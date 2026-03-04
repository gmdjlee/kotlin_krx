import java.util.Properties

plugins {
    kotlin("jvm") version "2.1.0"
    `java-library`
}

group = "com.krxkt"
version = "1.0.0-SNAPSHOT"

dependencies {
    // HTTP Client
    api("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON Parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Date/Time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
}

tasks.test {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

// Integration tests (requires network access)
tasks.register<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("integration")
    }
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// local.properties에서 KRX credentials 로드
val localProps = Properties()
file("local.properties").takeIf { it.exists() }?.inputStream()?.use { localProps.load(it) }

// Run integration test main classes
tasks.register<JavaExec>("runIntegrationTest") {
    group = "verification"
    description = "Run integration test main class"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set(project.findProperty("mainClass")?.toString() ?: "com.krxkt.integration.EtfPortfolioTestKt")

    // Gradle 프로퍼티 또는 local.properties에서 KRX credentials 전달
    listOf("krxId", "krxPw").forEach { prop ->
        val value = project.findProperty(prop)?.toString() ?: localProps.getProperty(prop)
        if (value != null) systemProperty(prop, value)
    }
}
