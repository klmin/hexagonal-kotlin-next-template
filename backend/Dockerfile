# jar는 밖에서 먼저 만든다 (jOOQ 코드 생성에 Docker가 필요해서 이미지 안에서는 빌드하지 않음)
#   ./gradlew bootJar
#   docker build -t backend .
#   docker run -p 8080:8080 -e SPRING_DATASOURCE_URL=... backend

FROM eclipse-temurin:25-jre-alpine AS extract
WORKDIR /app
COPY build/libs/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=extract /app/extracted/dependencies/ ./
COPY --from=extract /app/extracted/spring-boot-loader/ ./
COPY --from=extract /app/extracted/snapshot-dependencies/ ./
COPY --from=extract /app/extracted/application/ ./
USER app
EXPOSE 8080 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
