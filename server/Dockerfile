FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q -DskipTests package && cp target/server-*.jar /server.jar

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 app && mkdir -p /data/imports && chown app /data/imports
USER app
COPY --from=build /server.jar /app/server.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
