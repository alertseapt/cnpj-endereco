# Multi-stage: compila com Gradle, roda com JRE
FROM gradle:8.9-jdk21 AS build
WORKDIR /app
# copia primeiro o build.gradle para aproveitar cache de dependencias
COPY build.gradle ./
RUN gradle dependencies --no-daemon || true
COPY . .
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/app.jar app.jar
# Volume para os arquivos da Receita Federal baixados pelo ETL
VOLUME /data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
