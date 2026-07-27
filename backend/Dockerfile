# Multi-stage: build com Gradle, runtime com JRE
FROM gradle:8.9-jdk21 AS build
WORKDIR /app
COPY build.gradle ./
RUN gradle build --no-daemon || true   # baixa dependências; o build real ocorre abaixo
COPY . .
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/backend/build/libs/app.jar app.jar
# Volume para os arquivos da Receita Federal baixados pelo ETL
VOLUME /data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
