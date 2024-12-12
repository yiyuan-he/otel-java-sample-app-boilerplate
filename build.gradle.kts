plugins {
  id("java")
  id("org.springframework.boot") version "3.0.6"
  id("io.spring.dependency-management") version "1.1.0"
}

sourceSets {
  main {
    java.setSrcDirs(setOf("."))
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation(platform("software.amazon.awssdk:bom:2.27.21"))
  implementation("software.amazon.awssdk:s3")
  implementation("software.amazon.awssdk:sfn")
  implementation("software.amazon.awssdk:sns")
  implementation("software.amazon.awssdk:sqs")
  implementation("software.amazon.awssdk:secretsmanager")
  implementation("software.amazon.awssdk:lambda")
  implementation("software.amazon.awssdk:bedrock")
  implementation("software.amazon.awssdk:bedrockagent")
  implementation("software.amazon.awssdk:bedrockruntime")
  implementation("org.json:json:20240303")
  implementation("io.opentelemetry:opentelemetry-api:1.32.0")
  implementation("io.opentelemetry.semconv:opentelemetry-semconv:1.21.0-alpha")
  implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs:3.1.0")
}


