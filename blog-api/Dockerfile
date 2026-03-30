# 依赖 layertools 目录：在 blog-api 执行 mvn -DskipTests package 后生成 target/docker-layers/
# 增量：依赖不变时通常只需替换 target/docker-layers/application/
# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring

COPY target/docker-layers/dependencies/ ./
COPY target/docker-layers/spring-boot-loader/ ./
COPY target/docker-layers/snapshot-dependencies/ ./
COPY target/docker-layers/application/ ./

USER spring:spring
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
