FROM openjdk:17-oracle

WORKDIR /app

COPY out/artifacts/SearchEngine_jar/SearchEngine.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]