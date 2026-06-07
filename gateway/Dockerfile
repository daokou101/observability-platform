FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY gateway/pom.xml gateway/pom.xml
COPY platform-service/pom.xml platform-service/pom.xml
RUN mvn dependency:go-offline -B -q
COPY common/ common/
RUN mvn clean install -pl common -B -q -DskipTests
COPY gateway/ gateway/
RUN mvn clean package -pl gateway -B -q -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
