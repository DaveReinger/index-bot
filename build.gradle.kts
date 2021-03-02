import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.4.2"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.4.30"
    kotlin("plugin.spring") version "1.4.30"
}

group = "com.scomarlf"
version = "2.0.0"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    // google()
    maven {
        url = uri("http://maven.aliyun.com/nexus/content/groups/public")
    }
    maven {
        url = uri("https://snapshots.elastic.co/maven/")
    }
    maven {
        url = uri("https://s3.amazonaws.com/download.elasticsearch.org/lucenesnapshots/83f9835")
        name = "lucene-snapshots"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.springframework.boot:spring-boot-starter")

    implementation("com.google.guava:guava:24.0-jre")
    implementation("org.dom4j:dom4j:2.1.3")
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("org.apache.logging.log4j:log4j-api:2.14.0")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")

    // telegram
    api("com.github.pengrad:java-telegram-bot-api:5.0.1")
    // elasticsearch
    implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.11.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}